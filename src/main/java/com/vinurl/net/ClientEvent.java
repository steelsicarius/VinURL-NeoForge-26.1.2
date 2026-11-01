package com.vinurl.net;

import com.vinurl.client.ClientConfig;
import com.vinurl.client.FileSound;
import com.vinurl.client.KeyListener;
import com.vinurl.client.SoundManager;
import com.vinurl.exe.Executable;
import com.vinurl.gui.URLDiscScreen;
import com.vinurl.util.Url;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.vinurl.client.VinURLClient.CONFIG;
import static com.vinurl.util.Constants.LOGGER;

public class ClientEvent {

	public static void handlePlaySound(Payloads.PlaySoundRecord message, IPayloadContext context) {
		Minecraft client = Minecraft.getInstance();
		BlockPos pos = message.pos();
		int entityID = message.entityID();
		Url url = Url.parse(message.url());

		if (client.player == null || client.level == null || url == null) {return;}

		String fileName = SoundManager.getFileName(url.toString());

		FileSound fileSound = new FileSound(fileName, pos, client.level.getEntity(entityID));
		SoundManager.setSound(fileSound);

		// If download is already in progress, queue the sound to play when download completes
		if (Executable.isProcessRunning(fileName + "/download")) {
			SoundManager.queueSound(fileSound);
			return;
		}

		// If audio file already exists locally, play it immediately
		if (SoundManager.getAudioFile(fileName).exists()) {
			SoundManager.playSound(fileSound);
			return;
		}

		// Otherwise, need to download — check if downloads are enabled
		if (!CONFIG.downloadEnabled()) {return;}

		Url baseUrl = url.base();
		if (baseUrl == null) {return;}

		if (CONFIG.urlWhitelist().contains(baseUrl.toString())) {
			SoundManager.downloadSound(url.toString(), fileName);
			SoundManager.queueSound(fileSound);
			return;
		}

		// Not whitelisted — ask player to confirm via key press
		client.player.sendOverlayMessage(
			Component.translatable(
				"message.vinurl.custom_record.whitelist",
				Component.literal(KeyListener.getHotKey()).withStyle(ChatFormatting.YELLOW),
				Component.literal(baseUrl.toString()).withStyle(ChatFormatting.YELLOW)
			)
		);

		KeyListener.waitForKeyPress().thenAccept((confirmed) -> {
			if (confirmed) {
				// Persist whitelist change in ModConfigSpec (must call set() to save to disk)
				var current = ClientConfig.URL_WHITELIST.get();
				var updated = new java.util.ArrayList<>(current);
				updated.add(baseUrl.toString());
				ClientConfig.URL_WHITELIST.set(updated);
				SoundManager.downloadSound(url.toString(), fileName);
				SoundManager.queueSound(fileSound);
			}
		});
	}

	public static void handleStopSound(Payloads.StopSoundRecord message, IPayloadContext context) {
		BlockPos pos = message.pos();
		int entityID = message.entityID();
		boolean cancel = message.cancel();

		FileSound fileSound = pos != null ? SoundManager.getSound(pos) : SoundManager.getSound(entityID);
		SoundManager.stopSound(fileSound);
		SoundManager.unqueueSound(fileSound, cancel);
	}

	public static void handleGUIRecord(Payloads.GUIRecord message, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft.getInstance().setScreen(new URLDiscScreen(message.url(), message.duration()));
		});
	}
}
