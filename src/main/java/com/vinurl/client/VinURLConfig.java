package com.vinurl.client;

import com.vinurl.util.Url;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side configuration. Replaces the owo-generated config of the original mod
 * with a NeoForge ModConfigSpec (same option names, defaults and translation keys).
 */
public class VinURLConfig {
        public static final VinURLConfig CONFIG = new VinURLConfig();

        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        private static final ModConfigSpec.BooleanValue DOWNLOAD_ENABLED;
        private static final ModConfigSpec.BooleanValue UPDATES_ON_STARTUP;
        private static final ModConfigSpec.BooleanValue SHOW_DESCRIPTION;
        private static final ModConfigSpec.ConfigValue<List<? extends String>> URL_WHITELIST;
        private static final ModConfigSpec.EnumValue<AudioQuality> AUDIO_BITRATE;
        private static final ModConfigSpec.ConfigValue<String> PARAMETERS;
        public static final ModConfigSpec SPEC;

        static {
                BUILDER.translation("text.config.VinURLConfig.section.general").push("general");

                DOWNLOAD_ENABLED = BUILDER.comment("Whether to download custom music or not")
                        .translation("text.config.VinURLConfig.option.downloadEnabled")
                        .define("downloadEnabled", true);
                UPDATES_ON_STARTUP = BUILDER.comment("Update checks on startup for YT-DLP, FFmpeg, FFprobe")
                        .translation("text.config.VinURLConfig.option.updatesOnStartup")
                        .define("updatesOnStartup", true);
                SHOW_DESCRIPTION = BUILDER.comment("Whether to show 'Artist - Title' as disc tooltip")
                        .translation("text.config.VinURLConfig.option.showDescription")
                        .define("showDescription", true);
                URL_WHITELIST = BUILDER.comment("Whitelisted Providers")
                        .translation("text.config.VinURLConfig.option.urlWhitelist")
                        .defineListAllowEmpty("urlWhitelist", List.of(), () -> "", VinURLConfig::validateUrl);

                BUILDER.pop();

                BUILDER.translation("text.config.VinURLConfig.section.downloadSettings").push("downloadSettings");

                AUDIO_BITRATE = BUILDER.comment("Quality of the audio files")
                        .translation("text.config.VinURLConfig.option.audioBitrate")
                        .defineEnum("audioBitrate", AudioQuality.MEDIUM);
                PARAMETERS = BUILDER.comment("Add additional YT-DLP arguments,\nsee https://github.com/yt-dlp/yt-dlp#usage-and-options")
                        .translation("text.config.VinURLConfig.option.parameters")
                        .define("parameters", "");

                BUILDER.pop();

                SPEC = BUILDER.build();
        }

        public boolean downloadEnabled() {
                return VinURLConfig.DOWNLOAD_ENABLED.get();
        }

        public boolean updatesOnStartup() {
                return VinURLConfig.UPDATES_ON_STARTUP.get();
        }

        public boolean showDescription() {
                return VinURLConfig.SHOW_DESCRIPTION.get();
        }

        public List<? extends String> urlWhitelist() {
                return VinURLConfig.URL_WHITELIST.get();
        }

        public AudioQuality audioBitrate() {
                return VinURLConfig.AUDIO_BITRATE.get();
        }

        public String parameters() {
                return VinURLConfig.PARAMETERS.get();
        }

        /** Adds a base URL to the whitelist and persists the config file. */
        public void whitelist(String baseUrl) {
                List<String> updated = new ArrayList<>(URL_WHITELIST.get());
                if (!updated.contains(baseUrl)) {
                        updated.add(baseUrl);
                }
                URL_WHITELIST.set(updated);
        }

        /** Persists the current config values to disk. */
        public void save() {
                SPEC.save();
        }

        public enum AudioQuality implements TranslatableEnum {
                LOW("48K"),
                MEDIUM("96K"),
                HIGH("128K");

                private final String value;

                AudioQuality(String value) {
                        this.value = value;
                }

                public String getValue() {
                        return value;
                }

                @Override
                public Component getTranslatedName() {
                        return Component.translatable("text.config.VinURLConfig.option.audioBitrate.value." + name().toLowerCase());
                }
        }

        private static boolean validateUrl(final Object obj) {
                return obj instanceof String url && Url.isValid(url);
        }
}
