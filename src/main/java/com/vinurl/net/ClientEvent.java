package com.vinurl.net;

import com.vinurl.client.FileSound;
import com.vinurl.client.KeyListener;
import com.vinurl.client.SoundManager;
import com.vinurl.exe.Executable;
import com.vinurl.gui.URLDiscScreen;
import com.vinurl.util.Url;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.vinurl.client.VinURLConfig.CONFIG;
import static com.vinurl.util.Constants.MOD_ID;

public class ClientEvent {

	public record PlaySoundRecord(Optional<BlockPos> pos, int entityID, String url) implements CustomPacketPayload {
		public static final Type<PlaySoundRecord> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "play_sound_record"));
		public static final StreamCodec<ByteBuf, PlaySoundRecord> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PlaySoundRecord::pos,
			ByteBufCodecs.VAR_INT, PlaySoundRecord::entityID,
			ByteBufCodecs.STRING_UTF8, PlaySoundRecord::url,
			PlaySoundRecord::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StopSoundRecord(Optional<BlockPos> pos, int entityID, String url, boolean cancel) implements CustomPacketPayload {
		public static final Type<StopSoundRecord> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "stop_sound_record"));
		public static final StreamCodec<ByteBuf, StopSoundRecord> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StopSoundRecord::pos,
			ByteBufCodecs.VAR_INT, StopSoundRecord::entityID,
			ByteBufCodecs.STRING_UTF8, StopSoundRecord::url,
			ByteBufCodecs.BOOL, StopSoundRecord::cancel,
			StopSoundRecord::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GUIRecord(String url, int duration) implements CustomPacketPayload {
		public static final Type<GUIRecord> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "gui_record"));
		public static final StreamCodec<ByteBuf, GUIRecord> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, GUIRecord::url,
			ByteBufCodecs.VAR_INT, GUIRecord::duration,
			GUIRecord::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		/** Server-side helper: opens the URL screen on the owning client. */
		public static void sendTo(ServerPlayer player, String url, int duration) {
			PacketDistributor.sendToPlayer(player, new GUIRecord(url, duration));
		}
	}

	public static void handlePlaySoundRecord(PlaySoundRecord message, IPayloadContext context) {
		Minecraft client = Minecraft.getInstance();
		BlockPos pos = message.pos().orElse(null);
		int entityID = message.entityID();
		Url url = Url.parse(message.url());

		if (client.player == null || client.level == null || url == null) {return;}

		String fileName = SoundManager.getFileName(url.toString());

		FileSound fileSound = new FileSound(fileName, pos, client.level.getEntity(entityID));
		SoundManager.setSound(fileSound);

		if (Executable.isProcessRunning(fileName + "/download")) {
			SoundManager.queueSound(fileSound);
			return;
		}

		if (SoundManager.getAudioFile(fileName).exists()) {
			SoundManager.playSound(fileSound);
			return;
		}

		if (CONFIG.downloadEnabled()) {
			Url baseUrl = url.base();
			if (baseUrl == null) {return;}

			if (CONFIG.urlWhitelist().contains(baseUrl.toString())) {
				SoundManager.downloadSound(url.toString(), fileName);
				SoundManager.queueSound(fileSound);
				return;
			}

			client.player.sendOverlayMessage(
				Component.translatable(
					"message.vinurl.custom_record.whitelist",
					Component.literal(KeyListener.getHotKey()).withStyle(ChatFormatting.YELLOW),
					Component.literal(baseUrl.toString()).withStyle(ChatFormatting.YELLOW)
				));

			KeyListener.waitForKeyPress().thenAccept((confirmed) -> {
				if (confirmed) {
					CONFIG.whitelist(baseUrl.toString());
					SoundManager.downloadSound(url.toString(), fileName);
					SoundManager.queueSound(fileSound);
				}
			});
		}
	}

	public static void handleStopSoundRecord(StopSoundRecord message, IPayloadContext context) {
		BlockPos pos = message.pos().orElse(null);
		int entityID = message.entityID();
		boolean cancel = message.cancel();

		FileSound fileSound = pos != null ? SoundManager.getSound(pos) : SoundManager.getSound(entityID);
		SoundManager.stopSound(fileSound);
		SoundManager.unqueueSound(fileSound, cancel);
	}

	public static void handleGUIRecord(GUIRecord message, IPayloadContext context) {
		String url = message.url();
		int duration = message.duration();

		Minecraft.getInstance().setScreen(new URLDiscScreen(url, duration));
	}
}
