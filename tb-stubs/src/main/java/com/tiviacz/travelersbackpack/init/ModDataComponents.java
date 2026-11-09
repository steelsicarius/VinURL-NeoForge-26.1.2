// COMPILE-TIME STUB ONLY — excluded from the mod jar (build.gradle: exclude 'com/tiviacz/**').
// The declared type must match TB source exactly (ModDataComponents.java:69):
// DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>>.
// ItemStack.get/remove/has accept the Holder directly (NeoForge patch), so
// TravelersBackpackCompat's upgradeStack.get(ModDataComponents.BACKPACK_CONTAINER) resolves
// to the same ItemStack.get(Holder) overload used by real TB code. The field is NOT a
// compile-time constant (reference type), so no value inlining happens — only the
// descriptor Lnet/neoforged/neoforge/registries/DeferredHolder; matters at runtime.
package com.tiviacz.travelersbackpack.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModDataComponents {
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> BACKPACK_CONTAINER = null;
}
