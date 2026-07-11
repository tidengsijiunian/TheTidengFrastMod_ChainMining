package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FarmlandTillingHandler {

	public static final Set<Block> TILLABLE_BLOCKS = Set.of(
		Blocks.DIRT,
		Blocks.GRASS_BLOCK,
		Blocks.DIRT_PATH,
		Blocks.COARSE_DIRT,
		Blocks.ROOTED_DIRT,
		Blocks.MYCELIUM,
		Blocks.PODZOL
	);

	public static void tillLand(Minecraft client) {
		if (client.player == null || client.level == null) return;

		ItemStack heldItem = client.player.getMainHandItem();
		if (!(heldItem.getItem() instanceof HoeItem)) return;

		BlockPos center = getTargetCenter(client);
		int radius = 4;

		List<BlockPos> tillPositions = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos targetPos = center.offset(dx, 0, dz);
				BlockPos abovePos = targetPos.above();

				BlockState targetState = client.level.getBlockState(targetPos);
				BlockState aboveState = client.level.getBlockState(abovePos);

				if (!TILLABLE_BLOCKS.contains(targetState.getBlock())) continue;
				if (!aboveState.isAir()) continue;

				tillPositions.add(targetPos.immutable());
			}
		}

		if (tillPositions.isEmpty()) return;

		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = client.level.dimension();
			BlockState farmlandState = Blocks.FARMLAND.defaultBlockState();
			server.execute(() -> {
				ServerLevel serverLevel = server.getLevel(dimKey);
				if (serverLevel == null) return;
				for (BlockPos pos : tillPositions) {
					serverLevel.setBlock(pos, farmlandState, 3);
				}
			});
		} else {
			BlockState farmlandState = Blocks.FARMLAND.defaultBlockState();
			for (BlockPos pos : tillPositions) {
				client.level.setBlock(pos, farmlandState, 3);
			}
		}
	}

	private static BlockPos getTargetCenter(Minecraft client) {
		HitResult hit = client.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			return ((BlockHitResult) hit).getBlockPos();
		}
		return client.player.blockPosition().below();
	}
}
