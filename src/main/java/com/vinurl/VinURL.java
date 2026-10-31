package com.vinurl;

import com.vinurl.client.ClientConfig;
import com.vinurl.item.URLDisc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.JukeboxSong;

import static com.vinurl.util.Constants.*;

@Mod(MOD_ID)
public class VinURL {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

	public static final DeferredHolder<Item, Item> CUSTOM_RECORD = ITEMS.register("custom_record", () -> new URLDisc());
	public static final DeferredHolder<SoundEvent, SoundEvent> PLACEHOLDER_SOUND = SOUND_EVENTS.register("placeholder_sound", () -> SoundEvent.createVariableRangeEvent(PLACEHOLDER_SOUND_ID));

	public VinURL(IEventBus modBus) {
		VINURLPATH = FMLPaths.GAMEDIR.get().resolve(MOD_ID);

		ITEMS.register(modBus);
		SOUND_EVENTS.register(modBus);

		// Register client config
		ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

		modBus.addListener(this::onBuildCreativeModeTabContents);
		modBus.addListener(this::onCommonSetup);
	}

	private void onCommonSetup(FMLCommonSetupEvent event) {
		// Server-side setup goes here (network registration, etc.)
	}

	private void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().equals(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities")))) {
			event.accept(CUSTOM_RECORD.get().getDefaultInstance());
		}
	}
}
