package com.example.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OreOutlineRenderer {

	private static final Logger LOGGER = LoggerFactory.getLogger("tidengfirstmod");

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
			if (!ChainMiningHandler.INSTANCE.isActive()) return;

			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.level == null) return;

			ChainMiningHandler.INSTANCE.scanOres(client);
			var ores = ChainMiningHandler.INSTANCE.getFoundOres();
			if (ores.isEmpty()) return;

			Vec3 camPos = context.camera().getPosition();
			VertexConsumer consumer = context.consumers().getBuffer(RenderType.LINES);

			for (BlockPos pos : ores) {
				double x = pos.getX() - camPos.x;
				double y = pos.getY() - camPos.y;
				double z = pos.getZ() - camPos.z;
				renderWireframeBox(consumer, x, y, z, x + 1, y + 1, z + 1, 1f, 1f, 1f, 0.8f);
			}
		});
	}

	private static void renderWireframeBox(VertexConsumer consumer,
			double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ,
			float r, float g, float b, float a) {

		int color = ((int)(a * 255) << 24) | ((int)(b * 255) << 16)
			| ((int)(g * 255) << 8) | (int)(r * 255);

		line(consumer, minX, minY, minZ, maxX, minY, minZ, color);
		line(consumer, maxX, minY, minZ, maxX, minY, maxZ, color);
		line(consumer, maxX, minY, maxZ, minX, minY, maxZ, color);
		line(consumer, minX, minY, maxZ, minX, minY, minZ, color);

		line(consumer, minX, maxY, minZ, maxX, maxY, minZ, color);
		line(consumer, maxX, maxY, minZ, maxX, maxY, maxZ, color);
		line(consumer, maxX, maxY, maxZ, minX, maxY, maxZ, color);
		line(consumer, minX, maxY, maxZ, minX, maxY, minZ, color);

		line(consumer, minX, minY, minZ, minX, maxY, minZ, color);
		line(consumer, maxX, minY, minZ, maxX, maxY, minZ, color);
		line(consumer, maxX, minY, maxZ, maxX, maxY, maxZ, color);
		line(consumer, minX, minY, maxZ, minX, maxY, maxZ, color);
	}

	private static void line(VertexConsumer consumer,
			double x1, double y1, double z1,
			double x2, double y2, double z2, int color) {
		consumer.addVertex((float)x1, (float)y1, (float)z1).setColor(color).setNormal(0, 1, 0);
		consumer.addVertex((float)x2, (float)y2, (float)z2).setColor(color).setNormal(0, 1, 0);
	}
}