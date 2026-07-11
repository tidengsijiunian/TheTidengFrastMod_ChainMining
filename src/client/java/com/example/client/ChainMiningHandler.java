package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

public class ChainMiningHandler {

	public static final ChainMiningHandler INSTANCE = new ChainMiningHandler();

	private static final int SCAN_INTERVAL = 10;
	private static final int MAX_VEIN_SIZE = 64;
	private static final int MAX_PLANT_SIZE = 128;
	private static final int BREAK_BATCH_SIZE = 5;

	private boolean active = false;
	private int scanCooldown = 0;
	private BlockPos lastTargetPos = null;
	private final Set<BlockPos> foundOres = new LinkedHashSet<>();
	private Set<BlockPos> breakSnapshot = null;
	private Block breakTargetType = null;
	private boolean canBreak = false;
	private Block targetBlockType = null;

	private final List<BlockPos> breakQueue = new ArrayList<>();
	private int breakQueueIndex = 0;
	private MinecraftServer breakServer = null;
	private Map<BlockPos, BlockState> replantMap = null;

	public void setActive(boolean active) {
		this.active = active;
		if (!active) {
			foundOres.clear();
			scanCooldown = 0;
			lastTargetPos = null;
			canBreak = false;
			breakQueue.clear();
			breakQueueIndex = 0;
			breakServer = null;
			replantMap = null;
		}
	}

	public boolean isActive() {
		return active;
	}

	public Set<BlockPos> getFoundOres() {
		return foundOres;
	}

	public boolean canBreak() {
		return canBreak;
	}

	public void scanOres(Minecraft client) {
		if (!active) return;

		if (client.player == null || client.level == null) return;

		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			if (!foundOres.isEmpty()) {
				foundOres.clear();
				lastTargetPos = null;
				targetBlockType = null;
			}
			return;
		}

		BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();

		if (!targetPos.equals(lastTargetPos)) {
			foundOres.clear();
			lastTargetPos = null;
			targetBlockType = null;
			scanCooldown = 0;
		}

		if (scanCooldown > 0) {
			scanCooldown--;
			return;
		}
		scanCooldown = SCAN_INTERVAL;

		if (targetPos.equals(lastTargetPos)) return;

		Block block = client.level.getBlockState(targetPos).getBlock();
		if (!ChainMiningConfig.getInstance().getBlocks().contains(block)) {
			foundOres.clear();
			lastTargetPos = null;
			targetBlockType = null;
			canBreak = false;
			return;
		}

		canBreak = !client.level.getBlockState(targetPos).requiresCorrectToolForDrops()
				|| client.player.getMainHandItem().isCorrectToolForDrops(client.level.getBlockState(targetPos));

		foundOres.clear();
		lastTargetPos = targetPos;
		targetBlockType = block;

		boolean matchExact = isCrop(block);
		bfsScan(client, targetPos, block, matchExact);
	}

	private void bfsScan(Minecraft client, BlockPos start, Block targetBlock, boolean matchExact) {
		Queue<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new LinkedHashSet<>();
		queue.add(start);
		visited.add(start);

		ChainMiningConfig config = ChainMiningConfig.getInstance();
		int limit = config.isPlantLike(targetBlock) ? MAX_PLANT_SIZE : MAX_VEIN_SIZE;
		BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

		while (!queue.isEmpty() && visited.size() < limit) {
			BlockPos current = queue.poll();

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;

						mut.set(current.getX() + dx,
								current.getY() + dy,
								current.getZ() + dz);

						if (visited.contains(mut)) continue;
						if (!client.level.isInWorldBounds(mut)) continue;

						BlockState neighborState = client.level.getBlockState(mut);
						Block neighborBlock = neighborState.getBlock();

						if (matchExact) {
							if (neighborBlock != targetBlock) continue;
							if (isCrop(neighborBlock) && !isMatureCrop(neighborState)) continue;
						} else {
							if (!config.areSameGroup(neighborBlock, targetBlock)) continue;
						}

						BlockPos neighborPos = mut.immutable();
						visited.add(neighborPos);
						queue.add(neighborPos);
					}
				}
			}
		}

		foundOres.addAll(visited);
	}

	public void prepareBreak() {
		if (isBreaking()) return;
		if (!foundOres.isEmpty()) {
			this.breakSnapshot = new LinkedHashSet<>(foundOres);
			this.breakTargetType = targetBlockType;
		}
	}

	public void breakAllOres(Minecraft client) {
		if (isBreaking()) return;
		Set<BlockPos> targets = (breakSnapshot != null) ? breakSnapshot : foundOres;
		if (!active || targets.isEmpty()) return;
		if (client.player == null || client.level == null) return;

		ChainMiningConfig config = ChainMiningConfig.getInstance();
		Block filterBlock = (breakSnapshot != null) ? breakTargetType : targetBlockType;
		boolean holdingHoe = client.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.HoeItem;

		MinecraftServer server = client.getSingleplayerServer();
		breakQueue.clear();
		breakQueueIndex = 0;
		breakServer = server;
		replantMap = new LinkedHashMap<>();

		for (BlockPos pos : targets) {
			if (!canBreakBlock(client, pos)) continue;
			BlockState state = client.level.getBlockState(pos);
			if (state.isAir()) continue;
			if (filterBlock != null && !config.areSameGroup(state.getBlock(), filterBlock)) continue;

			if (holdingHoe && isCrop(state.getBlock())) {
				replantMap.put(pos.immutable(), state);
			}

			breakQueue.add(pos.immutable());
		}
		breakSnapshot = null;
	}

	public boolean isBreaking() {
		return !breakQueue.isEmpty() && breakQueueIndex < breakQueue.size();
	}

	public void tickBreak(Minecraft client) {
		if (!isBreaking()) return;
		if (client.player == null || client.level == null) return;

		int batchEnd = Math.min(breakQueueIndex + BREAK_BATCH_SIZE, breakQueue.size());

		if (breakServer != null) {
			ServerLevel level = breakServer.getLevel(client.level.dimension());
			if (level == null) return;
			final int from = breakQueueIndex;
			final int to = batchEnd;
			final List<BlockPos> batchCopy = new ArrayList<>(breakQueue.subList(from, to));
			final net.minecraft.server.level.ServerPlayer serverPlayer = breakServer.getPlayerList().getPlayer(client.player.getUUID());
			breakServer.execute(() -> {
				for (BlockPos pos : batchCopy) {
					level.destroyBlock(pos, true, serverPlayer);
				}
			});
		} else if (client.gameMode != null) {
			for (int i = breakQueueIndex; i < batchEnd; i++) {
				BlockPos pos = breakQueue.get(i);
				client.gameMode.destroyBlock(pos);
			}
		}

		breakQueueIndex = batchEnd;

		if (breakQueueIndex >= breakQueue.size()) {
			foundOres.clear();
			for (Map.Entry<BlockPos, BlockState> e : replantMap.entrySet()) {
				CropReplantHandler.tryAutoReplant(client, e.getKey(), e.getValue());
			}
			replantMap = null;
			breakQueue.clear();
			breakQueueIndex = 0;
			breakServer = null;
		}
	}

	private boolean isMatureCrop(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) return crop.isMaxAge(state);
		if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
		return false;
	}

	private boolean isCrop(Block block) {
		return block instanceof CropBlock
			|| block instanceof NetherWartBlock
			|| block == net.minecraft.world.level.block.Blocks.TORCHFLOWER_CROP
			|| block == net.minecraft.world.level.block.Blocks.PITCHER_CROP;
	}

	private boolean canBreakBlock(Minecraft client, BlockPos pos) {
		if (client.player == null || client.level == null) return false;
		net.minecraft.world.level.block.state.BlockState state = client.level.getBlockState(pos);
		if (!state.requiresCorrectToolForDrops()) return true;
		return client.player.getMainHandItem().isCorrectToolForDrops(state);
	}
}
