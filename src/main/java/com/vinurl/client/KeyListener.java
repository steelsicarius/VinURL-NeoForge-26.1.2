package com.vinurl.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.CompletableFuture;

import static com.vinurl.util.Constants.KEY_MAPPING_ID;
import static com.vinurl.util.Constants.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class KeyListener {
	private static final int KEY_PRESS_TIMEOUT_MILLIS = 5000;
	private static KeyMapping acceptKey;
	private static CompletableFuture<Boolean> waitingFuture;
	private static long timeout;
	public static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "mapping"));

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		acceptKey = new KeyMapping("key.vinurl.accept", GLFW.GLFW_KEY_Y, CATEGORY);
		event.register(acceptKey);
		NeoForge.EVENT_BUS.addListener(KeyListener::onClientTick);
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		if (waitingFuture == null || waitingFuture.isDone()) {return;}

		if (acceptKey.isDown()) {
			waitingFuture.complete(true);
			waitingFuture = null;
		} else if (System.currentTimeMillis() > timeout) {
			waitingFuture.complete(false);
			waitingFuture = null;
		}
	}

	public static CompletableFuture<Boolean> waitForKeyPress() {
		timeout = System.currentTimeMillis() + KEY_PRESS_TIMEOUT_MILLIS;
		return (waitingFuture = new CompletableFuture<>());
	}

	public static String getHotKey() {
		return acceptKey != null ? acceptKey.getName() : "Y";
	}
}
