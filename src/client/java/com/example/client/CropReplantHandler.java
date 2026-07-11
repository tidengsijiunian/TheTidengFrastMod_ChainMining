package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class CropReplantHandler {

	public static boolean tryAutoReplant(Minecraft client, BlockPos pos, BlockState savedState) {
		if (client.player == null || client.level == null) return false;

		ItemStack heldItem = client.player.getMainHandItem();
		if (!(heldItem.getItem() instanceof HoeItem)) return false;

		Item seedItem = SeedPlantingHandler.getSeedForCrop(savedState.getBlock());
		if (seedItem == null) return false;

		Inventory inv = client.player.getInventory();
		int seedSlot = findItem(inv, seedItem);
		if (seedSlot == -1) return false;

		BlockPos belowPos = pos.below();
		BlockState belowState = client.level.getBlockState(belowPos);
		if (!(belowState.getBlock() instanceof FarmBlock)) return false;

		ItemStack seedStack = inv.getItem(seedSlot);
		BlockState cropState;
		Block block = savedState.getBlock();
		if (block instanceof CropBlock) {
			cropState = block.defaultBlockState();
		} else {
			cropState = block.defaultBlockState();
		}

		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			BlockPos finalPos = pos.immutable();
			BlockState finalCropState = cropState;
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = client.level.dimension();
			server.execute(() -> {
				ServerLevel serverLevel = server.getLevel(dimKey);
				if (serverLevel != null) {
					serverLevel.setBlock(finalPos, finalCropState, 3);
				}
			});
		} else {
			client.level.setBlock(pos, cropState, 3);
		}

		seedStack.shrink(1);
		return true;
	}

	private static int findItem(Inventory inv, Item item) {
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() == item) return i;
		}
		return -1;
	}
}
