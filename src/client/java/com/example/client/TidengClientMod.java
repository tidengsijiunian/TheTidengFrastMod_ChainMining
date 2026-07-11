package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class TidengClientMod implements ClientModInitializer {

	private static KeyMapping chainMiningKey;
	private static boolean wasActive = false;
	private static boolean chainBreakPending = false;
	private static BlockPos chainBreakTarget = null;
	private static net.minecraft.world.level.block.Block chainBreakBlockType = null;

	@Override
	public void onInitializeClient() {
		chainMiningKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.tidengfirstmod.chain_mining",
			GLFW.GLFW_KEY_GRAVE_ACCENT,
			"category.tidengfirstmod.chain_mining"
		));

		ChainMiningConfig.getInstance();

		OreOutlineRenderer.register();
		ChainMiningHud.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			if (client.screen != null) {
				if (ChainMiningHandler.INSTANCE.isActive()) {
					ChainMiningHandler.INSTANCE.setActive(false);
				}
				return;
			}

			boolean isDown = chainMiningKey.isDown();
			ChainMiningHandler.INSTANCE.setActive(isDown);

			if (!isDown && wasActive) {
				chainBreakPending = false;
				chainBreakTarget = null;
			}
			wasActive = isDown;

			if (ChainMiningHandler.INSTANCE.isBreaking()) {
				ChainMiningHandler.INSTANCE.tickBreak(client);
				return;
			}

			if (chainBreakPending && chainBreakTarget != null && client.level != null) {
				BlockState state = client.level.getBlockState(chainBreakTarget);
				if (state.isAir() || state.getBlock() != chainBreakBlockType) {
					chainBreakPending = false;
					ChainMiningHandler.INSTANCE.breakAllOres(client);
				}
			}
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen != null) return InteractionResult.PASS;
			if (ChainMiningHandler.INSTANCE.isBreaking()) return InteractionResult.PASS;

			BlockState state = world.getBlockState(pos);

			if (ChainMiningHandler.INSTANCE.isActive()
					&& !ChainMiningHandler.INSTANCE.getFoundOres().isEmpty()) {
				chainBreakPending = true;
				chainBreakTarget = pos.immutable();
				chainBreakBlockType = state.getBlock();
				ChainMiningHandler.INSTANCE.prepareBreak();
			}

			return InteractionResult.PASS;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen != null) return InteractionResult.PASS;
			if (ChainMiningHandler.INSTANCE.isBreaking()) return InteractionResult.PASS;

			if (!chainMiningKey.isDown()) return InteractionResult.PASS;

			if (hitResult.getType() != HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}

			BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
			BlockState targetState = world.getBlockState(targetPos);
			net.minecraft.world.item.Item heldItem = player.getMainHandItem().getItem();

			if (heldItem instanceof HoeItem) {
				if (!FarmlandTillingHandler.TILLABLE_BLOCKS.contains(targetState.getBlock())) {
					return InteractionResult.PASS;
				}
				FarmlandTillingHandler.tillLand(Minecraft.getInstance());
				return InteractionResult.SUCCESS;
			}

			if (SeedPlantingHandler.isSeed(heldItem)) {
				if (!(targetState.getBlock() instanceof FarmBlock)) {
					return InteractionResult.PASS;
				}
				SeedPlantingHandler.plantSeeds(Minecraft.getInstance());
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
	}
}
