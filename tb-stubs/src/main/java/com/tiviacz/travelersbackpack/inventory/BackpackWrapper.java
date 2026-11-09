// COMPILE-TIME STUB ONLY — excluded from the mod jar (build.gradle: exclude 'com/tiviacz/**').
// At runtime the REAL Traveler's Backpack class is used. Field/method DESCRIPTORS below
// must match the real TB 26.1 classes exactly (verified against the user's TB source):
//   - getBackpackOwner() → Lnet/minecraft/world/entity/player/Player;
//   - getUpgradeManager() → Lcom/tiviacz/travelersbackpack/inventory/UpgradeManager;
package com.tiviacz.travelersbackpack.inventory;

import net.minecraft.world.entity.player.Player;

public class BackpackWrapper {
        public Player getBackpackOwner() { throw new RuntimeException("VinURL compile-time stub"); }
        public UpgradeManager getUpgradeManager() { throw new RuntimeException("VinURL compile-time stub"); }
}
