package com.example.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new ConfigScreen(parent);
	}

	private static class ConfigScreen extends Screen {

		private final Screen parent;
		private EditBox addBlockField;
		private final List<String> displayList = new ArrayList<>();
		private int scrollOffset = 0;
		private static final int ITEMS_PER_PAGE = 14;

		ConfigScreen(Screen parent) {
			super(Component.literal("ChainMining - 方块配置"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			refreshDisplayList();

			int leftX = this.width / 2 - 155;
			int rightX = this.width / 2 + 5;

			this.addBlockField = new EditBox(this.font, leftX, 32, 200, 20,
				Component.literal("输入方块ID"));
			this.addBlockField.setMaxLength(200);
			this.addBlockField.setHint(Component.literal("例: minecraft:stone"));
			this.addRenderableWidget(this.addBlockField);

			this.addRenderableWidget(
				Button.builder(Component.literal("添加"), btn -> {
					String id = addBlockField.getValue().trim();
					if (!id.isEmpty()) {
						ChainMiningConfig.getInstance().addBlock(id);
						addBlockField.setValue("");
						refreshDisplayList();
					}
				}).bounds(rightX, 32, 60, 20).build()
			);

			this.addRenderableWidget(
				Button.builder(Component.literal("删除选中"), btn -> {
					String id = addBlockField.getValue().trim();
					if (!id.isEmpty()) {
						ChainMiningConfig.getInstance().removeBlock(id);
						addBlockField.setValue("");
						refreshDisplayList();
					}
				}).bounds(rightX, 56, 60, 20).build()
			);

			this.addRenderableWidget(
				Button.builder(Component.literal("重置默认"), btn -> {
					ChainMiningConfig.getInstance().resetToDefaults();
					refreshDisplayList();
				}).bounds(rightX, 80, 60, 20).build()
			);

			int maxScroll = Math.max(0, displayList.size() - ITEMS_PER_PAGE);
			if (scrollOffset > maxScroll) scrollOffset = maxScroll;

			this.addRenderableWidget(
				Button.builder(Component.literal("▲"), btn -> {
					if (scrollOffset > 0) scrollOffset--;
				}).bounds(rightX, 110, 28, 20).build()
			);

			this.addRenderableWidget(
				Button.builder(Component.literal("▼"), btn -> {
					if (scrollOffset < displayList.size() - ITEMS_PER_PAGE) scrollOffset++;
				}).bounds(rightX + 32, 110, 28, 20).build()
			);

			this.addRenderableWidget(
				Button.builder(Component.literal("完成"), btn -> this.onClose())
					.bounds(this.width / 2 - 100, this.height - 32, 200, 20)
					.build()
			);
		}

		private void refreshDisplayList() {
			displayList.clear();
			displayList.addAll(ChainMiningConfig.getInstance().getUserBlockIds());
		}

		@Override
		public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
			context.fill(0, 0, this.width, this.height, 0xC0101010);
			super.render(context, mouseX, mouseY, delta);

			context.drawString(this.font,
				Component.literal("已配置方块 (" + displayList.size() + "个)"),
				10, 10, 0xFFFFFF);

			int y = 58;
			int end = Math.min(scrollOffset + ITEMS_PER_PAGE, displayList.size());
			for (int i = scrollOffset; i < end; i++) {
				String id = displayList.get(i);
				int color = isBlockValid(id) ? 0x55FF55 : 0xFF5555;
				context.drawString(this.font, "  " + id, 10, y, color);
				y += 12;
			}
		}

		private boolean isBlockValid(String id) {
			var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
			if (rl == null) return false;
			var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl)
				.map(net.minecraft.core.Holder.Reference::value)
				.orElse(null);
			return block != null && block != net.minecraft.world.level.block.Blocks.AIR;
		}

		@Override
		public void onClose() {
			this.minecraft.setScreen(parent);
		}
	}
}
