package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class ChainMiningConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir().resolve("tidengfirstmod_chain_blocks.json");

	private static final Set<String> DEFAULT_BLOCKS = new LinkedHashSet<>();
	private static final List<List<String>> DEFAULT_GROUPS = new ArrayList<>();

	static {
		addDefault("coal_ore", "iron_ore", "copper_ore", "gold_ore", "diamond_ore",
			"emerald_ore", "redstone_ore", "lapis_ore",
			"nether_quartz_ore", "ancient_debris", "nether_gold_ore",
			"deepslate_coal_ore", "deepslate_iron_ore", "deepslate_copper_ore",
			"deepslate_gold_ore", "deepslate_diamond_ore", "deepslate_emerald_ore",
			"deepslate_redstone_ore", "deepslate_lapis_ore");

		String[] logTypes = {"oak", "spruce", "birch", "jungle", "acacia",
			"dark_oak", "mangrove", "cherry", "pale_oak"};
		for (String t : logTypes) {
			addDefault(t + "_log", "stripped_" + t + "_log",
				t + "_wood", "stripped_" + t + "_wood");
		}

		addDefault("crimson_stem", "stripped_crimson_stem",
			"crimson_hyphae", "stripped_crimson_hyphae",
			"warped_stem", "stripped_warped_stem",
			"warped_hyphae", "stripped_warped_hyphae");

		addDefault("bamboo_block", "stripped_bamboo_block");

		for (String t : logTypes) {
			addDefault(t + "_leaves");
		}
		addDefault("azalea_leaves", "flowering_azalea_leaves");

		addDefault("red_mushroom_block", "brown_mushroom_block", "mushroom_stem");

		addDefault("nether_wart_block", "warped_wart_block");

		addDefault("sea_lantern", "glowstone", "shroomlight");

		addDefault("stone", "granite", "andesite", "deepslate", "obsidian",
			"moss_block", "sand", "red_sand", "clay");

		addDefault("dirt", "grass_block", "mycelium",
			"coarse_dirt", "rooted_dirt", "podzol");

		addDefault("short_grass", "tall_grass", "fern", "large_fern");

		addDefault("dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
			"red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
			"oxeye_daisy", "cornflower", "lily_of_the_valley",
			"torchflower", "wither_rose");

		addDefault("sunflower", "lilac", "rose_bush", "peony", "pitcher_plant");

		addDefault("wheat", "carrots", "potatoes", "beetroots",
			"nether_wart", "torchflower_crop", "pitcher_crop",
			"melon_stem", "pumpkin_stem",
			"melon", "pumpkin");

		DEFAULT_GROUPS.add(List.of(
			"minecraft:dirt", "minecraft:grass_block", "minecraft:mycelium",
			"minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:podzol"));

		DEFAULT_GROUPS.add(List.of(
			"minecraft:short_grass", "minecraft:tall_grass",
			"minecraft:fern", "minecraft:large_fern",
			"minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
			"minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
			"minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip",
			"minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley",
			"minecraft:torchflower", "minecraft:wither_rose",
			"minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush",
			"minecraft:peony", "minecraft:pitcher_plant"));

		DEFAULT_GROUPS.add(List.of(
			"minecraft:wheat", "minecraft:carrots", "minecraft:potatoes",
			"minecraft:beetroots", "minecraft:nether_wart",
			"minecraft:torchflower_crop", "minecraft:pitcher_crop",
			"minecraft:melon_stem", "minecraft:pumpkin_stem",
			"minecraft:melon", "minecraft:pumpkin"));
	}

	private static void addDefault(String... ids) {
		for (String id : ids) {
			DEFAULT_BLOCKS.add("minecraft:" + id);
		}
	}

	private Set<String> userBlocks;
		private Block cachedPlantRef = null;
	private boolean plantRefResolved = false;

	private Set<Block> resolvedBlocks;
	private Map<Block, Integer> blockToGroup;

	private static ChainMiningConfig INSTANCE;

	public static ChainMiningConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ChainMiningConfig();
			INSTANCE.load();
		}
		return INSTANCE;
	}

	private ChainMiningConfig() {}

	public void load() {
		userBlocks = new LinkedHashSet<>(DEFAULT_BLOCKS);
		if (CONFIG_PATH.toFile().exists()) {
			try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
				Type type = new TypeToken<LinkedHashSet<String>>() {}.getType();
				Set<String> loaded = GSON.fromJson(reader, type);
				if (loaded != null) {
					userBlocks.addAll(loaded);
				}
			} catch (Exception e) {
				System.err.println("[TidengMod] Failed to load config, using defaults");
			}
		}
		resolveBlocks();
		resolveGroups();
	}

	public void save() {
		try {
			CONFIG_PATH.toFile().getParentFile().mkdirs();
			try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
				GSON.toJson(userBlocks, writer);
			}
		} catch (Exception e) {
			System.err.println("[TidengMod] Failed to save config");
		}
	}

	private void resolveBlocks() {
		resolvedBlocks = new LinkedHashSet<>();
		for (String id : userBlocks) {
			ResourceLocation rl = ResourceLocation.tryParse(id);
			if (rl != null) {
				Block block = BuiltInRegistries.BLOCK.get(rl)
						.map(Holder.Reference::value)
						.orElse(null);
				if (block != null && block != Blocks.AIR) {
					resolvedBlocks.add(block);
				}
			}
		}
	}

	private void resolveGroups() {
		blockToGroup = new HashMap<>();
		int nextId = 0;

		for (List<String> group : DEFAULT_GROUPS) {
			for (String id : group) {
				ResourceLocation rl = ResourceLocation.tryParse(id);
				if (rl != null) {
					Block block = BuiltInRegistries.BLOCK.get(rl)
							.map(Holder.Reference::value)
							.orElse(null);
					if (block != null) {
						blockToGroup.put(block, nextId);
					}
				}
			}
			nextId++;
		}

		for (Block block : resolvedBlocks) {
			if (!blockToGroup.containsKey(block)) {
				blockToGroup.put(block, nextId++);
			}
		}
	}

	public int getGroup(Block block) {
		Integer group = blockToGroup.get(block);
		return group != null ? group : -1;
	}

	public boolean areSameGroup(Block a, Block b) {
		int ga = getGroup(a);
		int gb = getGroup(b);
		return ga >= 0 && ga == gb;
	}

	public boolean isPlantLike(Block block) {
		if (!plantRefResolved) {
			ResourceLocation rl = ResourceLocation.tryParse("minecraft:short_grass");
			if (rl != null) {
				cachedPlantRef = BuiltInRegistries.BLOCK.get(rl)
						.map(Holder.Reference::value)
						.orElse(null);
			}
			plantRefResolved = true;
		}
		if (cachedPlantRef == null) return false;
		return areSameGroup(block, cachedPlantRef);
	}

	public Set<Block> getBlocks() {
		return resolvedBlocks;
	}

	public Set<String> getUserBlockIds() {
		return userBlocks;
	}

	public void addBlock(String blockId) {
		userBlocks.add(blockId);
		resolveBlocks();
		resolveGroups();
		save();
	}

	public void removeBlock(String blockId) {
		userBlocks.remove(blockId);
		resolveBlocks();
		resolveGroups();
		save();
	}

	public void resetToDefaults() {
		userBlocks = new LinkedHashSet<>(DEFAULT_BLOCKS);
		resolveBlocks();
		resolveGroups();
		save();
	}

	public int getBlockCount() {
		return resolvedBlocks.size();
	}
}