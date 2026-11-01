# VinURL

VinURL is a Minecraft mod for NeoForge 26.1.2 that adds a writeable music disc, allowing you to customize the music that plays from any link.

## About

VinURL is a port of the popular [URL Music Discs](https://modrinth.com/mod/url-music-discs) mod by [Hoofer](https://github.com/HooferDevelops). 
With the Custom Music Disc, players can download and play any cloud-hosted audio file directly in Minecraft or download
them from YouTube or other supported [Sites](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md), all via a URL, creating a personalized musical experience like never before.

## Usage

| Enter URL | Use Disc |
|-----------|----------|
| 1. Right click the Custom Music Disc | 1. Insert the Disc into a Jukebox |
| 2. Enter a valid URL | 2. Wait until the download completes |
| 3. Press Enter or Escape to set the URL | 3. Enjoy the music :) |

## Commands

- `/vinurl delete` - Delete all downloaded audio files
- `/vinurl update` - Check for executable updates (yt-dlp, ffmpeg, deno)
- `/vinurl config` - Open configuration screen

## Configuration

VinURL uses NeoForge's built-in config system. The config file is located at `config/vinurl-client.toml`.

Available options:
- `downloadEnabled` - Enable/disable audio downloads (default: true)
- `updatesOnStartup` - Check for executable updates on startup (default: true)
- `showDescription` - Show song description in item tooltip (default: true)
- `urlWhitelist` - List of whitelisted base URLs (default: empty)
- `audioBitrate` - Audio quality: LOW (48K), MEDIUM (96K), HIGH (128K) (default: MEDIUM)
- `parameters` - Additional yt-dlp parameters (default: empty)

## Port to 26.1.2

This is a NeoForge port of the original Fabric mod. Key changes from the original:

- **Mod loader**: Fabric → NeoForge 26.1.2.114
- **Networking**: owo.network → NeoForge CustomPacketPayload + StreamCodec
- **Config**: owo.config → NeoForge ModConfigSpec
- **Registries**: Fabric Registry.register → NeoForge DeferredRegister
- **GUI**: owo-ui → Vanilla Minecraft widgets
- **Mod ID**: `vinurl` (unchanged)
- **Package**: `com.vinurl` (unchanged)
- **Jar**: `vinurl-2.6.2.jar`

### Fixes in 2.6.1 (post-port bugfix release)

- **Mod icon in disc screen**: 26.1.2 resolves `blit()` identifiers directly as resource paths (no automatic `textures/` prefix / `.png` suffix). Icon id is now `vinurl:textures/icon.png`; `logoFile` in `neoforge.mods.toml` likewise points to the jar-root path `assets/vinurl/textures/icon.png`.
- **Download/Simulate never finishing**: `commons-exec` was missing from the production jar (`runtimeOnly` does not package dependencies). It is now shaded into the jar via the `jar` task.
- **Config resetting on restart**: (1) `ConfigValue.set()` in NeoForge 26.1.2 only updates memory — every `set()` is now followed by `save()`. (2) `urlWhitelist` was declared with `define(path, Collections.emptyList())`, whose validator rejects the `ArrayList` read back from the TOML file, so the config loader reset it on every startup — now declared with `defineListAllowEmpty(..., o -> o instanceof String)`.
- **Silent placeholder sound**: `sounds.json` referenced `vinurl:placeholder_sound` without an audio file. Added a 1-second silent OGG (`assets/vinurl/sounds/placeholder_sound.ogg`), matching the Fabric original's use of `fabric-sound-api-v1:empty`. Without a real file, `SoundEngine.play()` aborts with `EMPTY_SOUND` before the custom `getStream()` is ever called — no audio would play even after a successful download.
- `InGameHudMixin` moved to the `client` section of `vinurl.mixins.json` (removes the `Attempted to load class net.minecraft.client.gui.Gui` error on dedicated servers).

## Fixes in 2.6.2

- **No sound from VinURL discs**: `FileSound` now plays the downloaded `.ogg` directly with vanilla `JOrbisAudioStream` (origin parity). The previous approach (external ffmpeg `.ogg → .wav` conversion + JavaSound) returned a failed future that `SoundEngine` swallows silently whenever the WAV was missing or lost the race against playback — resulting in total silence. ffmpeg is now only needed for yt-dlp's own audio postprocessing, not for playback.

## Mixins

VinURL uses SpongePowered mixins for two features:
- **JukeboxMixin** (server): Automatically starts/stops VinURL sound when inserting/removing discs from jukeboxes, and stops playback after the configured duration
- **InGameHudMixin** (client): Suppresses the vanilla record overlay when playing VinURL records

Both mixins are loaded at runtime via `vinurl.mixins.json` referenced in `neoforge.mods.toml`.

## Client Disconnect

When the client disconnects from a server, all active download/transcode processes (yt-dlp, ffmpeg, deno) are automatically killed via `Executable.killAllProcesses()`.

## Building

```bash
JAVA_HOME=/usr/java/jdk-25-oracle-aarch64 ./gradlew build
```

## Running

```bash
JAVA_HOME=/usr/java/jdk-25-oracle-aarch64 ./gradlew runServer
```

Server runs on port 25599 by default.

## Dependencies

- commons-exec 1.6.0 (yt-dlp/ffmpeg/deno process management) — **shaded into the jar** (plain library, cannot use JarJar; the Fabric original bundled it via `include`)
- Provided by vanilla Minecraft at runtime (launcher libraries, not bundled): JNA 5.17.0 (platform detection), jorbis (Vorbis audio metadata), commons-codec, commons-io, commons-lang3
- Mixin annotations 0.8.5 (compileOnly)
