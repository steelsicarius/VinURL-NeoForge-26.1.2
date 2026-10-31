package com.vinurl.client;

import com.vinurl.util.Url;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;

import java.util.List;

import static com.vinurl.util.Constants.MOD_ID;

public class ClientConfig {
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.ConfigValue<Boolean> DOWNLOAD_ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> UPDATES_ON_STARTUP;
	public static final ModConfigSpec.ConfigValue<Boolean> SHOW_DESCRIPTION;
	public static final ModConfigSpec.ConfigValue<List<String>> URL_WHITELIST;
	public static final ModConfigSpec.ConfigValue<AudioQuality> AUDIO_BITRATE;
	public static final ModConfigSpec.ConfigValue<String> PARAMETERS;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder.push("general");
		DOWNLOAD_ENABLED = builder.define("downloadEnabled", true);
		UPDATES_ON_STARTUP = builder.define("updatesOnStartup", true);
		SHOW_DESCRIPTION = builder.define("showDescription", true);
		URL_WHITELIST = builder.define("urlWhitelist", java.util.Collections.<String>emptyList());
		builder.pop();

		builder.push("downloadSettings");
		AUDIO_BITRATE = builder.defineEnum("audioBitrate", AudioQuality.MEDIUM);
		PARAMETERS = builder.define("parameters", "");
		builder.pop();

		SPEC = builder.build();
	}

	public boolean downloadEnabled() {
		return DOWNLOAD_ENABLED.get();
	}

	public boolean updatesOnStartup() {
		return UPDATES_ON_STARTUP.get();
	}

	public boolean showDescription() {
		return SHOW_DESCRIPTION.get();
	}

	public List<String> urlWhitelist() {
		return URL_WHITELIST.get();
	}

	public AudioQuality audioBitrate() {
		return AUDIO_BITRATE.get();
	}

	public String parameters() {
		return PARAMETERS.get();
	}

	public enum AudioQuality {
		LOW("48K"), MEDIUM("96K"), HIGH("128K");

		private final String value;

		AudioQuality(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}
