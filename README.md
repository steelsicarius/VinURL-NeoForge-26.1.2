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
- **Jar**: `vinurl-2.6.0-mc26.1.2.jar`

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

- commons-exec 1.6.0 (yt-dlp/ffmpeg/deno process management)
- JNA 5.14.0 (platform detection)
- jorbis (Vorbis audio metadata reading)
- commons-codec (SHA-256 hashing for URL filenames)
