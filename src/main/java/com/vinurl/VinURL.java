package com.vinurl;

import com.mojang.logging.LogUtils;
import com.vinurl.item.URLDisc;
import com.vinurl.net.VinURLNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import static com.vinurl.util.Constants.*;

@Mod(MOD_ID)
public class VinURL {
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

	public static final DeferredItem<URLDisc> CUSTOM_RECORD = ITEMS.register("custom_record", URLDisc::new);
	public static final DeferredHolder<SoundEvent, SoundEvent> PLACEHOLDER_SOUND = SOUND_EVENTS.register("placeholder_sound", () -> SoundEvent.createVariableRangeEvent(PLACEHOLDER_SOUND_ID));

	public VinURL(IEventBus modEventBus, ModContainer modContainer) {
		ITEMS.register(modEventBus);
		SOUND_EVENTS.register(modEventBus);

		modEventBus.addListener(VinURLNetwork::onRegisterPayloads);
		modEventBus.addListener(VinURL::addCreative);
	}

	private static void addCreative(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(CUSTOM_RECORD);
		}
	}
}
