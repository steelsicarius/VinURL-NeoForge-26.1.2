package com.vinurl.mixin;

import com.tiviacz.travelersbackpack.common.ServerActions;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.vinurl.compat.TravelersBackpackCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the single chokepoint that every Traveler's Backpack upgrade-tab
 * server action goes through (play/stop record, tab open/close, ...).
 * Applied ONLY when Traveler's Backpack is installed (VinURLMixinPlugin);
 * TravelersBackpackCompat filters everything but the PLAY_RECORD action.
 *
 * Signature verified against the user's TB source (Travelers-Backpack-26.1):
 * modifyUpgradeTab(BackpackWrapper, int slot, boolean open, int packetType)
 * → descriptor (Lcom/tiviacz/travelersbackpack/inventory/BackpackWrapper;IZI)V.
 * The 2.6.7 attempt wrongly targeted (…IIZ)V and never applied on servers.
 */
@Mixin(value = ServerActions.class, remap = false)
public abstract class ServerActionsMixin {

        @Inject(method = "modifyUpgradeTab(Lcom/tiviacz/travelersbackpack/inventory/BackpackWrapper;IZI)V", at = @At("TAIL"))
        private static void vinurl$onModifyUpgradeTab(BackpackWrapper wrapper, int slot, boolean open, int packetType, CallbackInfo ci) {
                TravelersBackpackCompat.onModifyUpgradeTab(wrapper, slot, open, packetType);
        }
}
