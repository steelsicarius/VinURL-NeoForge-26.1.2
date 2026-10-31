package com.vinurl.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = com.vinurl.util.Constants.MOD_ID)
public class Network {

	@SubscribeEvent
	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");

		// Serverbound: client sends URL to server
		registrar.playToServer(
			Payloads.SET_URL_TYPE,
			Payloads.SET_URL_CODEC,
			ServerEvent::handleSetURL
		);

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
