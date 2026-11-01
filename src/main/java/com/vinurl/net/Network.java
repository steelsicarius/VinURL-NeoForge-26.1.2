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

		// Serverbound: client sends URL to server (common: works on server and integrated client)
		registrar.playToServer(
			Payloads.SET_URL_TYPE,
			Payloads.SET_URL_CODEC,
			ServerEvent::handleSetURL
		);
	}
}
