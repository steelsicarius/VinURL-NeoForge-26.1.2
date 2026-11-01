package com.vinurl.client;

import com.vinurl.exe.Executable;
import com.vinurl.gui.ProgressOverlay;
import com.vinurl.item.URLDisc;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import static com.vinurl.VinURL.CUSTOM_RECORD;

@EventBusSubscriber(modid = com.vinurl.util.Constants.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class VinURLClient {
	public static final ClientConfig CONFIG = new ClientConfig();

	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
		// Kill all active download/transcode processes when disconnecting from a server
		Executable.killAllProcesses();
		org.slf4j.LoggerFactory.getLogger("VinURL").info("Client disconnected: killed all active processes");
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			// Check executables on startup
			java.util.concurrent.CompletableFuture.runAsync(() -> {
				for (Executable exe : Executable.values()) {
					if (!exe.checkForExecutable()) {
						org.slf4j.LoggerFactory.getLogger("VinURL").error("Failed to load executable {}", exe);
					}
				}
			});
		});
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		ProgressOverlay.render(event.getGuiGraphics());
	}

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!stack.is(CUSTOM_RECORD.get()) || !stack.has(DataComponents.CUSTOM_DATA)) {return;}

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

		event.getToolTip().clear();
		event.getToolTip().add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
		event.getToolTip().add(Component.translatable("itemGroup.tools").withStyle(ChatFormatting.BLUE));

		if (CONFIG.showDescription()) {
			String description = SoundManager.getDescription(SoundManager.getFileName(tag.getString("music_url").orElse("")));
			String locked = tag.getBoolean("lock").orElse(false) ? "🔒 " : "";
			event.getToolTip().add(Component.literal(locked + description).withStyle(ChatFormatting.GRAY));
		}
	}
}
