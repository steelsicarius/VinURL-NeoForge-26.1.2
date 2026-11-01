package com.vinurl.net;

import com.vinurl.gui.URLDiscScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientEvent {

	public static void handlePlaySound(Payloads.PlaySoundRecord message, IPayloadContext context) {
		// TODO: Phase 3 - Implement client-side sound playback
		// Depends on SoundManager, FileSound, Executable, etc.
	}

	public static void handleStopSound(Payloads.StopSoundRecord message, IPayloadContext context) {
		// TODO: Phase 3 - Implement client-side sound stop
	}

	public static void handleGUIRecord(Payloads.GUIRecord message, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft.getInstance().setScreen(new URLDiscScreen(message.url(), message.duration()));
		});
	}
}
