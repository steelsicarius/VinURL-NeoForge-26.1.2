package com.vinurl.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Keeps the Traveler's Backpack mixin optional: it is applied only when TB is on the
 * mod list, so VinURL keeps working (with a clean log) for players without TB.
 */
public class VinURLMixinPlugin implements IMixinConfigPlugin {
        private static final String TRAVELERS_BACKPACK_ID = "travelersbackpack";
        private Boolean travelersBackpackLoaded;

        @Override
        public void onLoad(String mixinPackage) {}

        @Override
        public String getRefMapperConfig() {
                return null;
        }

        @Override
        public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
                if (!mixinClassName.endsWith("ServerActionsMixin")) {
                        return true;
                }
                if (this.travelersBackpackLoaded == null) {
                        this.travelersBackpackLoaded = detectTravelersBackpack();
                }
                return this.travelersBackpackLoaded;
        }

        @Override
        public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

        @Override
        public List<String> getMixins() {
                return null;
        }

        @Override
        public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

        @Override
        public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

        /**
         * ModList.get() is still NULL during Mixin "prepare" (config plugins are consulted
         * before FML finishes constructing ModList — the NPE was verified), so detection
         * falls back to the LoadingModList, which already knows every mod jar discovered.
         */
        private static boolean detectTravelersBackpack() {
                try {
                        net.neoforged.fml.ModList modList = net.neoforged.fml.ModList.get();
                        if (modList != null) {
                                return modList.isLoaded(TRAVELERS_BACKPACK_ID);
                        }
                        net.neoforged.fml.loading.FMLLoader current = net.neoforged.fml.loading.FMLLoader.getCurrentOrNull();
                        return current != null
                                        && current.getLoadingModList() != null
                                        && current.getLoadingModList().getModFileById(TRAVELERS_BACKPACK_ID) != null;
                } catch (Throwable t) {
                        return false;
                }
        }
}
