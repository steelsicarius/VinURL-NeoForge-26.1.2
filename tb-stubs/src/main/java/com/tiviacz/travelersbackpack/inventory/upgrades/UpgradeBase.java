// COMPILE-TIME STUB ONLY — excluded from the mod jar (build.gradle: exclude 'com/tiviacz/**').
// Real class: public abstract class UpgradeBase<T> implements IUpgrade<T>; the method used by
// TravelersBackpackCompat is getDataHolderStack() → Lnet/minecraft/world/item/ItemStack;
// (UpgradeBase.java:36). Generics <T> must match so UpgradeBase<?> compiles with the same
// erasure as at runtime.
package com.tiviacz.travelersbackpack.inventory.upgrades;

import net.minecraft.world.item.ItemStack;

public abstract class UpgradeBase<T> {
        public ItemStack getDataHolderStack() { throw new RuntimeException("VinURL compile-time stub"); }
}
