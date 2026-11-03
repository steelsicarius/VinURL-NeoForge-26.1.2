package com.vinurl.net;

import com.vinurl.util.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro de canales de red personalizados de VinURL.
 * <p>
 * Se suscribe al bus de mod común, por lo que se ejecuta tanto en el servidor dedicado como en el
 * cliente integrado/físico. Esto es necesario para que la negociación de canales de NeoForge
 * ({@link net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator}) tenga éxito:
 * cualquier canal no opcional declarado en un lado debe ser conocido por el otro, independientemente
 * de la dirección del flujo.
 * <p>
 * Los canales cliente-limitados ({@code play_sound}, {@code stop_sound}, {@code gui_record}) se
 * registran en ambos lados para que el servidor los conozca y pueda enviarlos, pero su handler de
 * cliente solo se adjunta en el cliente físico, evitando cargar clases cliente-solo
 * ({@code ClientEvent}, {@code Minecraft}...) en un servidor dedicado.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public class Network {

	@SubscribeEvent
	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		boolean isClient = FMLEnvironment.getDist() == Dist.CLIENT;
		PayloadRegistrar registrar = event.registrar("1");

		// Serverbound: el cliente envía la URL al servidor (lo maneja el servidor, ambos lados conocen el canal)
		registrar.playToServer(
			Payloads.SET_URL_TYPE,
			Payloads.SET_URL_CODEC,
			ServerEvent::handleSetURL
		);

		// Clientbound: el servidor envía al cliente. Se declaran en ambos lados (negociación + hasChannel)
		// pero el handler solo se adjunta en el cliente físico.
		if (isClient) {
			registrar.playToClient(
				Payloads.PLAY_SOUND_TYPE,
				Payloads.PLAY_SOUND_CODEC,
				ClientEvent::handlePlaySound
			);
			registrar.playToClient(
				Payloads.STOP_SOUND_TYPE,
				Payloads.STOP_SOUND_CODEC,
				ClientEvent::handleStopSound
			);
			registrar.playToClient(
				Payloads.GUI_RECORD_TYPE,
				Payloads.GUI_RECORD_CODEC,
				ClientEvent::handleGUIRecord
			);
		} else {
			registrar.playToClient(Payloads.PLAY_SOUND_TYPE, Payloads.PLAY_SOUND_CODEC);
			registrar.playToClient(Payloads.STOP_SOUND_TYPE, Payloads.STOP_SOUND_CODEC);
			registrar.playToClient(Payloads.GUI_RECORD_TYPE, Payloads.GUI_RECORD_CODEC);
		}
	}
}
