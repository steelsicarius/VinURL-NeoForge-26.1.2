package com.vinurl.mixin;

import com.vinurl.VinURL;
import com.vinurl.client.FileSound;
import com.vinurl.client.KeyListener;
import com.vinurl.client.SoundManager;
import com.vinurl.client.VinURLClient;
import com.vinurl.exe.Executable;
import com.vinurl.util.Constants;
import com.vinurl.util.Url;
import com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Mixin into Traveler's Backpack's JukeboxWidget to integrate VinURL custom discs.
 * When a VinURL disc is played/stopped in the backpack jukebox, we use VinURL's
 * FileSound streaming system instead of the vanilla sound event (which is silent
 * for VinURL discs since they use the placeholder sound).
 * <p>
 * Uses reflection for all Traveler's Backpack field/method access so this mod does
 * not require a compile-time dependency on Traveler's Backpack and is resilient to
 * mapping differences between dev and production environments.
 */
@Mixin(JukeboxWidget.class)
public abstract class JukeboxWidgetMixin {

    // Reflection method handles, resolved lazily
    private static Method METHOD_IS_TAB_OPENED;
    private static Method METHOD_IS_BACKPACK_OWNER;
    private static Method METHOD_IS_MOUSE_OVER_PLAY_BUTTON;
    private static Method METHOD_IS_MOUSE_OVER_STOP_BUTTON;
    private static Method METHOD_GET_DISK_HANDLER;

    static {
        try {
            METHOD_IS_TAB_OPENED = Class.forName("com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase")
                .getMethod("isTabOpened");
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not resolve UpgradeWidgetBase.isTabOpened", e);
        }
        try {
            METHOD_IS_BACKPACK_OWNER = Class.forName("com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase")
                .getMethod("isBackpackOwner");
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not resolve UpgradeWidgetBase.isBackpackOwner", e);
        }
        try {
            METHOD_IS_MOUSE_OVER_PLAY_BUTTON = Class.forName("com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget")
                .getMethod("isMouseOverPlayButton", double.class, double.class);
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not resolve JukeboxWidget.isMouseOverPlayButton", e);
        }
        try {
            METHOD_IS_MOUSE_OVER_STOP_BUTTON = Class.forName("com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxWidget")
                .getMethod("isMouseOverStopButton", double.class, double.class);
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not resolve JukeboxWidget.isMouseOverStopButton", e);
        }
        try {
            METHOD_GET_DISK_HANDLER = Class.forName("com.tiviacz.travelersbackpack.inventory.upgrades.jukebox.JukeboxUpgrade")
                .getDeclaredField("diskHandler")
                .getType()
                .getMethod("getResource", int.class);
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not resolve JukeboxUpgrade.diskHandler.getResource", e);
        }
    }

    /**
     * Intercept play button click: if the disc is a VinURL custom record, cancel
     * the vanilla (silent) sound and start VinURL's FileSound playback instead.
     */
    @Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean pButton, CallbackInfoReturnable<Boolean> cir) {
        if (METHOD_IS_MOUSE_OVER_PLAY_BUTTON == null || METHOD_IS_MOUSE_OVER_STOP_BUTTON == null ||
            METHOD_IS_BACKPACK_OWNER == null || METHOD_IS_TAB_OPENED == null) {
            return; // Traveler's Backpack not properly mapped, skip integration
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Object target = this; // the JukeboxWidget instance

        try {
            // Play button
            boolean isMouseOverPlay = (boolean) METHOD_IS_MOUSE_OVER_PLAY_BUTTON.invoke(target, event.x(), event.y());
            boolean isBackpackOwner = (boolean) METHOD_IS_BACKPACK_OWNER.invoke(target);
            boolean isTabOpened = (boolean) METHOD_IS_TAB_OPENED.invoke(target);

            if (isMouseOverPlay && isBackpackOwner && isTabOpened) {
                ItemStack stack = getDiscStack();
                if (stack != null && !stack.isEmpty() && stack.is(VinURL.CUSTOM_RECORD.get())) {
                    cir.setReturnValue(true); // consumed, don't run original
                    startVinylDisc(client.player, stack);
                    return;
                }
            }

            // Stop button
            boolean isMouseOverStop = (boolean) METHOD_IS_MOUSE_OVER_STOP_BUTTON.invoke(target, event.x(), event.y());
            if (isMouseOverStop && isBackpackOwner && isTabOpened) {
                ItemStack stack = getDiscStack();
                if (stack != null && !stack.isEmpty() && stack.is(VinURL.CUSTOM_RECORD.get())) {
                    cir.setReturnValue(true); // consumed, don't run original
                    stopVinylDisc(client.player, stack);
                    return;
                }
            }
        } catch (Exception e) {
            Constants.LOGGER.warn("VinURL: Error in Traveler's Backpack jukebox integration", e);
        }
    }

    /**
     * Get the ItemStack currently in the jukebox upgrade's disk slot, using
     * reflection to access JukeboxUpgrade.diskHandler (Traveler's Backpack class).
     */
    @Nullable
    private ItemStack getDiscStack() {
        try {
            Object target = this;
            // Get the 'upgrade' field (type JukeboxUpgrade)
            java.lang.reflect.Field upgradeField = target.getClass().getSuperclass().getDeclaredField("upgrade");
            upgradeField.setAccessible(true);
            Object upgrade = upgradeField.get(target);

            if (upgrade == null) {
                return ItemStack.EMPTY;
            }

            // Get diskHandler from JukeboxUpgrade
            java.lang.reflect.Field diskHandlerField = upgrade.getClass().getDeclaredField("diskHandler");
            diskHandlerField.setAccessible(true);
            Object diskHandler = diskHandlerField.get(upgrade);

            if (diskHandler == null || !(diskHandler instanceof ItemStacksResourceHandler handler)) {
                return ItemStack.EMPTY;
            }

            // Get resource at slot 0
            Object resource = handler.getClass().getMethod("getResource", int.class).invoke(handler, 0);
            if (resource instanceof ItemResource itemResource && !itemResource.isEmpty()) {
                return itemResource.toStack();
            }
        } catch (Exception e) {
            Constants.LOGGER.debug("VinURL: Could not access Traveler's Backpack jukebox disk handler", e);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Read the music URL from a VinURL disc's custom data.
     */
    @Nullable
    private String getDiscUrl(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getString("music_url").orElse(null);
    }

    /**
     * Start playing a VinURL disc from the backpack jukebox.
     */
    private void startVinylDisc(Player player, ItemStack stack) {
        String url = getDiscUrl(stack);
        if (url == null || url.isEmpty()) {
            return;
        }

        String fileName = SoundManager.getFileName(url);
        FileSound fileSound = new FileSound(fileName, null, player);
        SoundManager.setSound(fileSound);

        String processId = fileName + "/download";

        // If download already in progress, queue the sound
        if (Executable.isProcessRunning(processId)) {
            SoundManager.queueSound(fileSound);
            return;
        }

        // If audio file already exists locally, play immediately
        if (SoundManager.getAudioFile(fileName).exists()) {
            SoundManager.playSound(fileSound);
            return;
        }

        // Otherwise, check config and download
        if (!VinURLClient.CONFIG.downloadEnabled()) {
            return;
        }

        Url baseUrl = Url.parse(url).base();
        if (baseUrl == null) {
            return;
        }

        if (VinURLClient.CONFIG.urlWhitelist().contains(baseUrl.toString())) {
            SoundManager.downloadSound(url, fileName);
            SoundManager.queueSound(fileSound);
            return;
        }

        // Not whitelisted — ask player to confirm via key press
        player.sendOverlayMessage(
            Component.translatable(
                "message.vinurl.custom_record.whitelist",
                Component.literal(KeyListener.getHotKey()).withStyle(ChatFormatting.YELLOW),
                Component.literal(baseUrl.toString()).withStyle(ChatFormatting.YELLOW)
            )
        );
    }

    /**
     * Stop playing a VinURL disc from the backpack jukebox.
     */
    private void stopVinylDisc(Player player, ItemStack stack) {
        String url = getDiscUrl(stack);
        if (url == null || url.isEmpty()) {
            return;
        }

        String fileName = SoundManager.getFileName(url);
        FileSound fileSound = SoundManager.getSound(player.getId());
        SoundManager.stopSound(fileSound);
        SoundManager.unqueueSound(fileSound, true);
    }
}
