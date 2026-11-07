package com.vinurl.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Accessors for the custom data stored on a URLDisc item stack.
 * Replaces the owo KeyedEndec fields of the original mod ("music_url", "lock", "duration").
 * Defaults match the original KeyedEndec defaults ("", false, 0).
 */
public final class DiscData {
	public static final String URL_KEY = "music_url";
	public static final String LOCK_KEY = "lock";
	public static final String DURATION_KEY = "duration";

	private DiscData() {}

	public static CompoundTag copyTag(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static String url(CompoundTag tag) {
		return tag.getStringOr(URL_KEY, "");
	}

	public static boolean lock(CompoundTag tag) {
		return tag.getBooleanOr(LOCK_KEY, false);
	}

	public static int duration(CompoundTag tag) {
		return tag.getIntOr(DURATION_KEY, 0);
	}

	public static void save(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}
}
