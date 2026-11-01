package com.vinurl.net;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = com.vinurl.util.Constants.MOD_ID, value = Dist.CLIENT)
public class ClientNetwork {

	@SubscribeEvent
	public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");

		// Clientbound: server tells client to play sound
		registrar.playToClient(
			Payloads.PLAY_SOUND_TYPE,
			Payloads.PLAY_SOUND_CODEC,
			ClientEvent::handlePlaySound
		);

		// Clientbound: server tells client to stop sound
		registrar.playToClient(
			Payloads.STOP_SOUND_TYPE,
			Payloads.STOP_SOUND_CODEC,
			ClientEvent::handleStopSound
		);

		// Clientbound: server tells client to open GUI
		registrar.playToClient(
			Payloads.GUI_RECORD_TYPE,
			Payloads.GUI_RECORD_CODEC,
			ClientEvent::handleGUIRecord
		);
	}
}
