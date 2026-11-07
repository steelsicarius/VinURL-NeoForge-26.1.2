package com.vinurl.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.vinurl.util.Constants.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class VinURLNetwork {

        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
                final PayloadRegistrar registrar = event.registrar("1");

                registrar.playToServer(ServerEvent.SetURLRecord.TYPE, ServerEvent.SetURLRecord.STREAM_CODEC, ServerEvent::handleSetURLRecord);

                if (FMLEnvironment.getDist().isClient()) {
                        // Handler classes must never be loaded on the dedicated server: they reference client-only
                        // classes (URLDiscScreen extends Screen), which crashes the DistCleaner during verification.
                        registrar.playToClient(ClientEvent.PlaySoundRecord.TYPE, ClientEvent.PlaySoundRecord.STREAM_CODEC, ClientEvent::handlePlaySoundRecord);
                        registrar.playToClient(ClientEvent.StopSoundRecord.TYPE, ClientEvent.StopSoundRecord.STREAM_CODEC, ClientEvent::handleStopSoundRecord);
                        registrar.playToClient(ClientEvent.GUIRecord.TYPE, ClientEvent.GUIRecord.STREAM_CODEC, ClientEvent::handleGUIRecord);
                } else {
                        // Declare the clientbound payload types on the dedicated server without handlers.
                        registrar.playToClient(ClientEvent.PlaySoundRecord.TYPE, ClientEvent.PlaySoundRecord.STREAM_CODEC);
                        registrar.playToClient(ClientEvent.StopSoundRecord.TYPE, ClientEvent.StopSoundRecord.STREAM_CODEC);
                        registrar.playToClient(ClientEvent.GUIRecord.TYPE, ClientEvent.GUIRecord.STREAM_CODEC);
                }
        }
}
