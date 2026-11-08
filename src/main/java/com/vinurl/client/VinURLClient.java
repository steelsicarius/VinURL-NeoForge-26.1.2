package com.vinurl.client;

import com.vinurl.exe.Executable;
import com.vinurl.gui.ProgressOverlay;
import com.vinurl.net.ClientEvent;
import com.vinurl.util.DiscData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.concurrent.CompletableFuture;

import static com.vinurl.VinURL.CUSTOM_RECORD;
import static com.vinurl.client.VinURLConfig.CONFIG;
import static com.vinurl.util.Constants.LOGGER;
import static com.vinurl.util.Constants.MOD_ID;
import static com.vinurl.util.Constants.PROGRESS_HUD_ID;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MOD_ID, dist = Dist.CLIENT)
// The bus is auto-detected in NeoForge 26.1.2 (IModBusEvent goes to the mod bus, everything else to the game bus).
@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class VinURLClient {
        /**
         * Resolved on every use instead of cached in a static initializer:
         * NeoForge can class-load this type before the Minecraft instance exists
         * (mod construction / @EventBusSubscriber registration), which made the old
         * `static final Minecraft CLIENT = Minecraft.getInstance()` permanently null
         * and silently kill sound playback (NPE inside the play/stop payload handlers).
         */
        public static Minecraft getClient() {
                return Minecraft.getInstance();
        }

        public VinURLClient(ModContainer container) {
                // Register the client config so FML can create and load the config file for us
                container.registerConfig(ModConfig.Type.CLIENT, VinURLConfig.SPEC);
                // Allows NeoForge to create a config screen for this mod's configs (Mods screen > VinURL > config)
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

                CompletableFuture.runAsync(() -> {
                        for (Executable exe : Executable.values()) {
                                if (!exe.checkForExecutable()) {
                                        LOGGER.error("Failed to load executable {}", exe);
                                }
                        }
                });
        }

        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
                LOGGER.info("VinURL client setup");
        }

        @SubscribeEvent
        static void onTooltip(ItemTooltipEvent event) {
                var stack = event.getItemStack();
                if (!stack.is(CUSTOM_RECORD.get()) || !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {return;}

                var tag = DiscData.copyTag(stack);

                var lines = event.getToolTip();
                lines.clear();
                lines.add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("itemGroup.tools").withStyle(ChatFormatting.BLUE));

                if (CONFIG.showDescription()) {
                        String description = SoundManager.getDescription(SoundManager.getFileName(DiscData.url(tag)));
                        String locked = DiscData.lock(tag) ? "🔒 " : "";
                        lines.add(Component.literal(locked + description).withStyle(ChatFormatting.GRAY));
                }
        }

        @SubscribeEvent
        static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
                Executable.killAllProcesses();
        }

        @SubscribeEvent
        static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
                event.registerBelowAll(PROGRESS_HUD_ID, (guiGraphics, deltaTracker) -> ProgressOverlay.render(guiGraphics));
        }
}
