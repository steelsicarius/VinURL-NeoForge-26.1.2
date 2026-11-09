package com.vinurl.compat;

import com.tiviacz.travelersbackpack.common.ServerActions;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.vinurl.api.VinURLSound;
import com.vinurl.util.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Server-side bridge for the Traveler's Backpack jukebox upgrade.
 *
 * TB only plays the CLIENT-side JukeboxSong SoundEvent of the inserted disc; for a
 * VinURL record that song is the mod's silent placeholder, so nothing is ever heard.
 * The real audio is therefore triggered from the SERVER, exactly like the vanilla
 * jukebox does through JukeboxMixin:
 *
 *  - play  (open == true)  → VinURLSound.playFor  (64-block range, sound anchored to
 *    the player entity, so it survives opening/closing any screen — including the
 *    backpack menu with the 'B' key).
 *  - stop  (open == false) → VinURLSound.stopFor (cancelable == false).
 *
 * Called from ServerActionsMixin (TAIL of modifyUpgradeTab), the single chokepoint
 * every upgrade-tab server action passes through. Everything except PLAY_RECORD is
 * ignored, which is why the 'B' key, tab switches and enable/disable toggles never
 * touch the audio. VinURLSound.send() itself ignores stacks that are not a VinURL
 * custom record, so vanilla discs stay handled by TB's own client logic.
 */
public final class TravelersBackpackCompat {
        private TravelersBackpackCompat() {}

        public static void onModifyUpgradeTab(BackpackWrapper wrapper, int slot, boolean open, int packetType) {
                if (packetType != ServerActions.PLAY_RECORD) {
                        return;
                }

                try {
                        if (!(wrapper.getBackpackOwner() instanceof ServerPlayer player)) {
                                return;
                        }

                        UpgradeManager upgradeManager = wrapper.getUpgradeManager();
                        if (upgradeManager == null || upgradeManager.mappedUpgrades == null
                                        || !upgradeManager.mappedUpgrades.containsKey(slot)) {
                                return;
                        }

                        upgradeManager.mappedUpgrades.get(slot).ifPresent(upgrade -> playStopRecord(player, upgrade, open));
                } catch (Throwable t) {
                        Constants.LOGGER.error("VinURL: Traveler's Backpack jukebox compat failed", t);
                }
        }

        private static void playStopRecord(ServerPlayer player, UpgradeBase<?> upgrade, boolean open) {
                ItemStack upgradeStack = upgrade.getDataHolderStack();
                if (upgradeStack == null || upgradeStack.isEmpty()) {
                        return;
                }

                ItemContainerContents contents = upgradeStack.get(ModDataComponents.BACKPACK_CONTAINER);
                if (contents == null) {
                        return;
                }

                contents.allItemsCopyStream()
                                .filter(disc -> !disc.isEmpty())
                                .findFirst()
                                .ifPresent(disc -> {
                                        if (open) {
                                                VinURLSound.playFor(player.level(), disc, player.getId());
                                        } else {
                                                VinURLSound.stopFor(player.level(), disc, player.getId(), false);
                                        }
                                });
        }
}
