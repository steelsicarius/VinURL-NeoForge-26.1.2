# VinURL — NeoForge 26.1.2

A NeoForge port of [VinURL](https://github.com/Plompi/VinURL) (a fork of *URL Music Discs* by Hoofer), originally written by Plompi for Fabric 26.1.

Adds a writeable music disc, allowing you to customize the music that plays from any link.

## Port to 26.1.2

This repository contains the **NeoForge 26.1.2 port** (VinURL `2.6.0`, NeoForge `26.1.2.114`, ModDevGradle `2.0.148`, Java 25). The full porting log lives in [PORTING.md](PORTING.md). Highlights:

- **owo-lib removed.** The Fabric-only library was replaced with NeoForge equivalents, feature by feature:
  - `OwoNetChannel` → `CustomPacketPayload` payloads (`PlaySoundRecord`, `StopSoundRecord`, `GUIRecord`, `SetURLRecord`) registered on `RegisterPayloadHandlersEvent`.
  - owo config → NeoForge `ModConfigSpec` + the built-in `ConfigurationScreen` (same option names, defaults and translation keys as the original config).
  - owo-ui XML screen → a vanilla `Screen` with the same textures and layout (URL field, simulate & lock nine-patch buttons, duration slider with mm:ss drag tooltip).
  - `KeyedEndec` typed NBT → plain `CompoundTag` keys (`music_url`, `lock`, `duration`, same defaults).
- **commons-exec removed.** Only `CommandLine` was used; a tiny in-repo `CommandBuilder` replaces it (no new dependencies).
- **Mixins kept.** `JukeboxBlockEntity` (`setTheItem`/`popOutTheItem`/`tick`) and `Gui.setNowPlaying` have no NeoForge event equivalents; the mixin targets were verified against the 26.1.2 sources and declared via `[[mixins]]` in `neoforge.mods.toml`.
- **API changes applied for 26.1.2** (all verified against the decompiled sources): item model list in `assets/<ns>/items/*.json`, `SoundInstance.getAudioStream(...)` → `getStream(SoundBufferLibrary, Sound, boolean)`, `CompoundTag.getString/getBoolean/getInt` → `*Or(key, default)`, sound pipeline is OGG-only (own silent `empty.ogg` replaces `fabric-sound-api-v1:empty`), `data/minecraft/tags/item/` (singular) for registry tags, new widget render model (`extractRenderState`/`extractContents`), `RegisterGuiLayersEvent`/`RegisterKeyMappingsEvent`/`RegisterClientCommandsEvent`/`ItemTooltipEvent`/`ClientPlayerNetworkEvent.LoggingOut`.
- **Bug fix vs. the original:** the `minecraft:music_discs` item tag was shipped under `tags/items/` (plural), which vanilla never loads — it is now under `tags/item/`.

## Features (same as the original)

- Download and play any cloud-hosted audio file directly in Minecraft.
- Download music from YouTube or other supported sites via URL (yt-dlp, deno and FFmpeg binaries are downloaded automatically on first client start).
- Custom Music Disc item (shapeless recipe with note block + 8 wools); right-click with the disc to enter a URL.
- Insert the disc into a jukebox to play the downloaded music; synced to nearby players.
- `/vinurl delete`, `/vinurl update` and `/vinurl config` client commands.
- Client-side settings in the mods screen config (download toggle, whitelist, audio quality, extra yt-dlp parameters).

## Building

```bash
.\gradlew.bat build      # Windows
./gradlew build          # Linux/macOS
```

The jar is produced at `build/libs/VinURL-2.6.0-26.1.2.jar`. Requires Java 25 (the Gradle toolchain downloads it automatically).

## Running

```bash
.\gradlew.bat runServer   # dedicated server test
.\gradlew.bat runClient   # graphical client
```

## License

VinURL is licensed under [CC-BY-NC-4.0](https://creativecommons.org/licenses/by-nc/4.0/), like the original mod. Authors: Plompi (original mod), Hoofer (URL Music Discs).
