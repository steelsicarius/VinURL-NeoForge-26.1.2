package com.vinurl.api;

import com.vinurl.net.Payloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.vinurl.VinURL.CUSTOM_RECORD;
import static com.vinurl.util.Constants.*;

@SuppressWarnings("unused")
public class VinURLSound {
	private static final double JUKEBOX_RANGE = 64;
	private static final double INFINITE_RANGE = Double.POSITIVE_INFINITY;

	public static void playAt(ServerLevel level, ItemStack stack, BlockPos pos) {
		if (!stack.is(CUSTOM_RECORD.get())) {return;}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		List<ServerPlayer> players = playersInRange(level, pos, JUKEBOX_RANGE);
		Payloads.PlaySoundRecord payload = new Payloads.PlaySoundRecord(pos, -1, tag.getString("music_url").orElse(""));
		for (ServerPlayer player : players) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	public static void playFor(ServerLevel level, ItemStack stack, int entityID) {
		if (!stack.is(CUSTOM_RECORD.get())) {return;}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		List<ServerPlayer> players = playersInRange(level, entityID, JUKEBOX_RANGE);
		Payloads.PlaySoundRecord payload = new Payloads.PlaySoundRecord(null, entityID, tag.getString("music_url").orElse(""));
		for (ServerPlayer player : players) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	public static void stopAt(ServerLevel level, ItemStack stack, BlockPos pos, boolean cancelable) {
		if (!stack.is(CUSTOM_RECORD.get())) {return;}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		List<ServerPlayer> players = playersInRange(level, pos, INFINITE_RANGE);
		Payloads.StopSoundRecord payload = new Payloads.StopSoundRecord(pos, -1, tag.getString("music_url").orElse(""), cancelable);
		for (ServerPlayer player : players) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	public static void stopFor(ServerLevel level, ItemStack stack, int entityID, boolean cancelable) {
		if (!stack.is(CUSTOM_RECORD.get())) {return;}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		List<ServerPlayer> players = playersInRange(level, entityID, INFINITE_RANGE);
		Payloads.StopSoundRecord payload = new Payloads.StopSoundRecord(null, entityID, tag.getString("music_url").orElse(""), cancelable);
		for (ServerPlayer player : players) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	private static List<ServerPlayer> playersInRange(ServerLevel level, BlockPos pos, double range) {
		return level.getPlayers((player) -> player.position().distanceTo(pos.getCenter()) <= range);
	}

	private static List<ServerPlayer> playersInRange(ServerLevel level, int entityID, double range) {
		return Optional.ofNullable(level.getEntity(entityID))
			.map(entity -> playersInRange(level, entity.blockPosition(), range))
			.orElse(List.of());
	}
}
