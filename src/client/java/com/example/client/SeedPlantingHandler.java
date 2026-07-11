package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

public class SeedPlantingHandler {

	private static long lastPlantTime = 0;
	private static final long PLANT_COOLDOWN_MS = 500;

	private static final Map<Item, Block> SEED_TO_CROP = new LinkedHashMap<>();
	private static final Map<Block, Item> CROP_TO_SEED = new LinkedHashMap<>();

	static {
		register(Items.WHEAT_SEEDS, Blocks.WHEAT);
		register(Items.CARROT, Blocks.CARROTS);
		register(Items.POTATO, Blocks.POTATOES);
		register(Items.BEETROOT_SEEDS, Blocks.BEETROOTS);
		register(Items.NETHER_WART, Blocks.NETHER_WART);
		register(Items.TORCHFLOWER_SEEDS, Blocks.TORCHFLOWER_CROP);
		register(Items.PITCHER_POD, Blocks.PITCHER_CROP);
		register(Items.MELON_SEEDS, Blocks.MELON_STEM);
		register(Items.PUMPKIN_SEEDS, Blocks.PUMPKIN_STEM);
	}

	private static void register(Item seed, Block crop) {
		SEED_TO_CROP.put(seed, crop);
		CROP_TO_SEED.put(crop, seed);
	}

	public static boolean isSeed(Item item) {
		return SEED_TO_CROP.containsKey(item);
	}

	public static Block getCropForSeed(Item seed) {
		return SEED_TO_CROP.get(seed);
	}

	public static Item getSeedForCrop(Block crop) {
		return CROP_TO_SEED.get(crop);
	}

	public static void plantSeeds(Minecraft client) {
		if (client.player == null || client.level == null) return;

		long now = System.currentTimeMillis();
		if (now - lastPlantTime < PLANT_COOLDOWN_MS) return;
		lastPlantTime = now;

		Item heldItem = client.player.getMainHandItem().getItem();
		Block cropBlock = getCropForSeed(heldItem);
		if (cropBlock == null) return;

		Inventory inv = client.player.getInventory();
		int seedSlot = findItem(inv, heldItem);
		if (seedSlot == -1) return;
		ItemStack seedStack = inv.getItem(seedSlot);

		BlockPos center = getTargetCenter(client);
		int radius = 6;

		List<BlockPos> plantPositions = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos farmlandPos = center.offset(dx, 0, dz);
				BlockPos abovePos = farmlandPos.above();

				BlockState belowState = client.level.getBlockState(farmlandPos);
				BlockState aboveState = client.level.getBlockState(abovePos);

				if (!(belowState.getBlock() instanceof FarmBlock)) continue;
				if (!aboveState.isAir()) continue;

				plantPositions.add(abovePos.immutable());
			}
		}

		if (plantPositions.isEmpty()) return;

		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = client.level.dimension();
			BlockState cropState = cropBlock.defaultBlockState();
			server.execute(() -> {
				ServerLevel serverLevel = server.getLevel(dimKey);
				if (serverLevel == null) return;
				for (BlockPos pos : plantPositions) {
					serverLevel.setBlock(pos, cropState, 3);
				}
			});
		} else {
			BlockState cropState = cropBlock.defaultBlockState();
			for (BlockPos pos : plantPositions) {
				client.level.setBlock(pos, cropState, 3);
			}
		}

		seedStack.shrink(plantPositions.size());
	}

	private static int findItem(Inventory inv, Item item) {
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() == item) return i;
		}
		return -1;
	}

	private static BlockPos getTargetCenter(Minecraft client) {
		HitResult hit = client.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			return ((BlockHitResult) hit).getBlockPos();
		}
		return client.player.blockPosition().below();
	}
}
