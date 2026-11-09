package com.vinurl.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Constants {
        //general
        public static final String MOD_ID = "vinurl";
        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
        public static final Path VINURLPATH = FMLPaths.GAMEDIR.get().resolve(MOD_ID);

        //resources
        public static final Identifier PLACEHOLDER_SOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "placeholder_sound");
        public static final Identifier CUSTOM_RECORD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "custom_record");
        public static final Identifier KEY_MAPPING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "mapping");
        public static final Identifier PROGRESS_HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "progress_hud");
        public static final Identifier URL_DISC_SCREEN_ID = Identifier.fromNamespaceAndPath(MOD_ID, "disc_url_screen");
        // GUI texture paths must be the REAL file paths ("textures/gui/…png"): GuiGraphicsExtractor.blit
        // uses the Identifier literally as the texture location, it does NOT prepend textures/gui.
        public static final Identifier SIMULATE_BUTTON_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/simulate_button.png");
        public static final Identifier SIMULATE_BUTTON_HOVER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/simulate_button_hovered.png");
        public static final Identifier SIMULATE_BUTTON_DISABLED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/simulate_button_disabled.png");
        public static final Identifier LOCK_BUTTON_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/lock_button.png");
        public static final Identifier LOCK_BUTTON_DISABLED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/lock_button_disabled.png");
        public static final ResourceKey<JukeboxSong> SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, PLACEHOLDER_SOUND_ID);
        public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, CUSTOM_RECORD_ID);
}
