# VinURL Porting to NeoForge MC 26.1.2

## Current Status
- **Phase**: Release — version 2.6.1 (post-port bugfix release), pending user verification
- **Build**: PASSING (`./gradlew build`) → `build/libs/vinurl-2.6.1.jar`
- **runServer**: PASSING — VinURL 2.6.1 loaded, `Done (0.5s)!`, **0 exceptions/errors** in log
  - NOTE: verified on temporary port 25600 because 25599 was held by an *unrelated* project's runServer
    (Bosses of Mass Destruction, PID 2881232 — not ours, not touched). `run/server.properties` restored to `server-port=25599` afterwards.
- **runClient**: NOT TESTED — LWJGL platform mismatch (aarch64 JVM, x64 natives only)
- **User must replace** the old `vinurl-2.6.0.jar` in their mods folder with `vinurl-2.6.1.jar`

## Issues Fixed — 2.6.2 Session (user report: disc inserts but no sound; downloads + vanilla discs fine)

### No audio from VinURL discs (files download correctly to .minecraft/vinurl)
**Root cause**: `FileSound.getStream()` required a WAV previously produced by an external ffmpeg
conversion (`SoundManager.convertToWav`), and if the WAV was missing (conversion failed on the user's
machine, or `queueSound`'s onComplete fired `playSound` BEFORE the blocking conversion finished),
`getStream()` returned a **failed future**. `SoundEngine.play()` consumes that future only via
`thenAccept(...)` — a failed future is swallowed silently → **no sound, no error in the log**.
(Also verified: `SoundEngine.play()` only calls `SoundInstance.getStream()` when the resolved
`Sound` entry has `stream: true` — `isStreaming = sound.shouldStream()`; otherwise it plays the
sound entry's own file via `getCompleteBuffer`, bypassing the hook. Our sounds.json sets
`"stream": true` on the placeholder event, so the hook path is correct.)

**Fix**: play the downloaded `.ogg` directly — exactly what the Fabric original did:
`FileSound.getStream()` → `SkippableAudioStream.offset(new JOrbisAudioStream(new FileInputStream(ogg)), elapsed)`
on `Util.nonCriticalIoPool()`. `JOrbisAudioStream` is a vanilla class (implements `AudioStream` via
`FloatSampleSource`); `org.jcraft:jorbis` is a vanilla launcher library. Removed `convertToWav`,
`getWavFile`, `WavAudioStream` (dead code after this change) — ffmpeg is now only needed for
yt-dlp's audio postprocessing, not for playback.
Also added a fallback in `SoundManager.queueSound()`: if the download process was already
deregistered (race between `isProcessRunning` and `getProcessStream`) but the file exists, play
immediately instead of staying silent.

**Files**: `FileSound.java`, `SoundManager.java`

**Version bumped to 2.6.2** so the user can distinguish the jar.

## Issues Fixed — 2.6.1 Session (user-reported bugs from logs/)

### 1. Download / Simulate stuck forever ("Downloading 1/1", "Calculating...")
**Root cause** (from user's latest.log):
`Caused by: java.lang.ClassNotFoundException: org.apache.commons.exec.CommandLine at com.vinurl.exe.Executable.getCommandLine(Executable.java:204)`.
`runtimeOnly`/`compileOnly` do NOT package a dependency into the built jar — in dev runs it worked (classpath), but in the user's production mods folder the classes were simply missing, so the async download task died and the progress overlay never completed.

**Fix**: shade commons-exec classes into the mod jar via the `jar` task (custom `jarLibs` configuration, `runtimeClasspath.extendsFrom jarLibs` for dev runs). The Fabric original did exactly this with `include (implementation "org.apache.commons:commons-exec:1.6.0")`.
JNA dependency REMOVED: vanilla MC 26.1.2 ships `net.java.dev.jna:jna:5.17.0` as a launcher library (verified in `minecraft_26.1.2_version_manifest.json`); the original never bundled it either. commons-codec/commons-io/commons-lang3/jorbis are also vanilla launcher libraries.

**Files**: `build.gradle`

### 2. Icon shows as missing-texture checkerboard (right-click disc screen)
**Root cause** (from user's latest.log): `Missing resource vinurl:icon referenced from vinurl:icon`.
In 26.1.2, `GuiGraphicsExtractor.blit(Identifier, ...)` → `TextureManager.getTexture()` → `TextureContents.load(ResourceManager, Identifier)` uses the identifier **directly as a resource path** — NO automatic `textures/` prefix and NO `.png` suffix (verified against vanilla: `LogoRenderer.MINECRAFT_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minecraft.png")`). `vinurl:icon` looked for `assets/vinurl/icon` which does not exist.

**Fix**: `ICON_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/icon.png")`.
Also fixed the mods-list logo: `ModListScreen` resolves `logoFile` from the **jar root** (`packResources.getRootResource`), so `neoforge.mods.toml` now has `logoFile="assets/vinurl/textures/icon.png"`.

**Files**: `URLDiscScreen.java`, `neoforge.mods.toml`

### 3. Config resets to defaults on restart (/vinurl config)
**Root causes** (two independent bugs):
a) In NeoForge 26.1.2, `ModConfigSpec.ConfigValue.set()` only updates in-memory config — javadoc: *"Directly sets the value, without firing events or writing the config to disk. Make sure to call `ModConfigSpec#save()` eventually."* Nothing called `save()`, so nothing was ever written to `vinurl-client.toml`.
b) `URL_WHITELIST` was declared with `builder.define("urlWhitelist", Collections.emptyList())`. `define(path, defaultValue)` builds the validator `defaultValue.getClass().isAssignableFrom(o.getClass())` → `Collections$EmptyList.class.isAssignableFrom(ArrayList.class)` is FALSE, so the ArrayList read back from the TOML file always failed validation. On every startup the config loader logged "Incorrect key general.urlWhitelist was corrected from [] to its default, []" and rewrote the file, wiping saved values (user log: "Configuration file ... vinurl-client.toml is not correct. Correcting").

**Fix**: every `.set()` now followed by `.save()` (ConfigScreen x4, ClientEvent whitelist), and `URL_WHITELIST = builder.defineListAllowEmpty("urlWhitelist", List.of(), () -> "", o -> o instanceof String)` (field type `ConfigValue<List<? extends String>>`; needed `new ArrayList<String>(current)` in ClientEvent to avoid wildcard-capture compile error).

**Files**: `ClientConfig.java`, `ConfigScreen.java`, `ClientEvent.java`

### 4. No audio would ever play (latent, found while diagnosing #1/#2)
**Root cause**: port's `sounds.json` referenced `"name": "vinurl:placeholder_sound"` with no OGG file (log: `File vinurl:sounds/placeholder_sound.ogg does not exist`). The original referenced `fabric-sound-api-v1:empty`, a REAL silent OGG bundled by Fabric API. Without a real file, `SoundEngine.play()` resolves the event's sound to `SoundManager.EMPTY_SOUND` and **returns NOT_STARTED before ever calling `SoundInstance.getStream()`** (verified in decompiled `SoundEngine.play()`) — so even a successful download would have been silent (log: "Unable to play empty soundEvent: vinurl:placeholder_sound").

**Fix**: generated a 1-second silent mono 44.1kHz OGG (`assets/vinurl/sounds/placeholder_sound.ogg`) with the same role as `fabric-sound-api-v1:empty`. `FileSound.getStream()` still overrides the audio data with the downloaded WAV.

**Files**: `assets/vinurl/sounds/placeholder_sound.ogg` (new)

### 5. Dedicated server: "Attempted to load class net.minecraft.client.gui.Gui"
**Root cause**: port merged both mixins into the common `mixins` array of `vinurl.mixins.json`; the original declares `InGameHudMixin` under `"client"`. On a dedicated server the mixin tried to apply and loaded a client-only class.

**Fix**: moved `InGameHudMixin` to the `"client"` section; also bumped `compatibilityLevel` JAVA_13 → JAVA_25 (matches origin). Server log is now 0 errors.

**Files**: `vinurl.mixins.json`

## Issues Fixed — Previous Session (2.6.0)

### 1. Config resets to defaults on restart (/vinurl config)
**Root cause**: `ClientEvent.handlePlaySound()` called `CONFIG.urlWhitelist().add(baseUrl.toString())` which modifies the list in memory but does NOT call `.set()` on the `ModConfigSpec.ConfigValue`, so the change is never written to `vinurl-client.toml`.

**Fix**: Changed to read current list via `ClientConfig.URL_WHITELIST.get()`, copy to `ArrayList`, add the URL, then call `ClientConfig.URL_WHITELIST.set(updated)` to persist to disk.

**Files**: `ClientEvent.java`

### 2. Whitelist key not showing in message
**Root cause**: `KeyListener.getHotKey()` returned `acceptKey.getName()` which is the translation key string `"key.vinurl.accept"`, not the actual key name `"Y"`. The message showed "Press key.vinurl.accept to whitelist ..." instead of "Press Y to whitelist ...".

**Fix**: Changed to `acceptKey.getTranslatedKeyMessage().getString()` which returns the actual key display name (e.g., "Y").

**Files**: `KeyListener.java`

### 3. Mod icon not showing in URLDiscScreen GUI
**Root cause**: The icon PNG was at `assets/vinurl/icon.png`. Minecraft's TextureManager looks for textures at `assets/<namespace>/textures/<path>.png`, so identifier `vinurl:icon` resolves to `assets/vinurl/textures/icon.png`. The file was not at the expected location, so the texture was missing/blank.

**Fix**: Moved `assets/vinurl/icon.png` → `assets/vinurl/textures/icon.png`. The blit call already uses correct corner coordinates (`context.blit(ICON_ID, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f)`).

**Files**: `assets/vinurl/icon.png` → `assets/vinurl/textures/icon.png`

## Issues Fixed (Previous Sessions)

### 1. Simulate button stuck at "calculating..." / no sound
**Root cause**: `commons-exec` `ClassNotFoundException` — dependency not visible at runtime through NeoForge ModuleClassLoader. Also missing `checkForExecutable()` calls before running yt-dlp/deno.

**Fix**: Changed `commons-exec` and `jna` from `implementation` to `compileOnly` + `runtimeOnly` in `build.gradle`. Added `Executable.YT_DLP.checkForExecutable()` and `Executable.DENO.checkForExecutable()` calls in simulate button handler.

**Files**: `build.gradle`, `URLDiscScreen.java`

### 2. No sound playback
**Root cause A**: `ClientEvent.handlePlaySound()` was a TODO stub — did nothing when server sent `PlaySoundRecord` payload.
**Root cause B**: `FileSound` used `PLACEHOLDER_SOUND` with no audio data.
**Root cause C**: Downloaded .ogg files couldn't be played by JavaSound (needs .wav).

**Fix**: Implemented `handlePlaySound()`/`handleStopSound()` with download flow. Added `SoundManager.convertToWav()` using ffmpeg. Added `SoundManager.WavAudioStream` wrapping JavaSound `AudioInputStream`. Overrode `FileSound.getStream()` to return WAV audio stream.

**Files**: `ClientEvent.java`, `SoundManager.java`, `FileSound.java`

### 3. NPE on VinURLClient.CLIENT (null static Minecraft field)
**Root cause**: `public static final Minecraft CLIENT = Minecraft.getInstance()` initialized at class-load time returns null before Minecraft is ready.

**Fix**: Removed static `CLIENT` field. All usages replaced with lazy `Minecraft.getInstance()` at point of use.

**Files**: `VinURLClient.java`, `URLDiscScreen.java`, `Commands.java`, `ProgressOverlay.java`, `SoundManager.java`

### 4. Done button does nothing / ESC key doesn't exit menu
**Root cause**: Same as #3 — NPE prevented `onClose()` from being reached.

**Fix**: Same as #3.

**Files**: `URLDiscScreen.java`

### 5. Icon blit coordinates wrong
**Root cause**: `GuiGraphicsExtractor.blit(Identifier, x0, y0, x1, y1, u0, u1, v0, v1)` takes corner coordinates (x0,y0,x1,y1), not (x,y,width,height). Also `u1=0.0f` equaled `u0=0.0f`, zero UV width.

**Fix**: Changed to `context.blit(ICON_ID, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f)`.

**Files**: `URLDiscScreen.java`

## API Changes Found (Fabric → NeoForge MC 26.1.2)

| Fabric (26.1) | NeoForge (26.1.2) |
|---|---|
| `owo-network` `NETWORK_CHANNEL.clientHandle().send()` | `Minecraft.getInstance().getConnection().send(ServerboundCustomPayloadPacket)` |
| `owo-config` `ConfigScreen.create()` | Manual vanilla `Screen` + `ModConfigSpec.ConfigValue.set()` |
| `Screen.keyPressed(int,int,int)` | `Screen.keyPressed(KeyEvent)` |
| `Screen.render(GuiGraphics,int,int,float)` | `Screen.extractRenderState(GuiGraphicsExtractor,int,int,float)` |
| `BaseUIModelScreen` (owo-ui) | Vanilla `Screen` + `Button.builder` + `EditBox` |
| `GLFW.GLFW_KEY_ESCAPE` | `KeyEvent.key() == 256` |
| `CompoundTag.getBoolean()` returns boolean | Returns `Optional<Boolean>`, use `.orElse(false)` |
| `CreativeModeTabs.TOOLS_AND_UTILITIES` (public) | Construct `ResourceKey` directly |
| `fabric-api` mixin annotation processor | Mixins load at runtime via `[[mixins]]` in `neoforge.mods.toml` |
| `VinURLClient.CLIENT = Minecraft.getInstance()` static field | Call `Minecraft.getInstance()` lazily at point of use |
| `NETWORK_CHANNEL.registerClientbound(...)` lambda | `PayloadRegistrar.playToClient(...)` with method ref |
| `SoundInstance.getStream()` default loads from sounds.json | Must override for custom audio data (downloaded .ogg files) |
| `GuiGraphics.blit(Identifier, x, y, width, height, u, v, uWidth, vWidth)` | `GuiGraphicsExtractor.blit(Identifier, x0, y0, x1, y1, u0, u1, v0, v1)` — corners, not width/height |
| `KeyMapping.getName()` returns translation key | `KeyMapping.getTranslatedKeyMessage().getString()` returns actual key name |
| Texture path: `assets/<ns>/<path>.png` | Texture path: `assets/<ns>/textures/<path>.png` |
| `GuiGraphics.blit(id)` auto-resolves `textures/<path>.png` | 26.1.2: `blit` Identifier is the literal resource path — must be `textures/icon.png` (with extension), see `TextureContents.load()` |
| `owo-config` writes config on change | `ModConfigSpec.ConfigValue.set()` only updates memory; MUST call `save()` afterwards |
| `owo-config` List<String> field | `define(path, List)` validator = default's exact class → rejects ArrayList from TOML; use `defineListAllowEmpty(path, default, () -> "", o -> o instanceof String)` |
| Fabric `include(...)` nests library jars | NeoForge JarJar only nests *mods*; plain libs must be shaded into the jar (jar task `from zipTree`) |
| `fabric-sound-api-v1:empty` silent OGG (bundled by Fabric API) | Provide own silent `placeholder_sound.ogg`; empty events make `SoundEngine.play()` bail BEFORE `getStream()` |
| Fabric mod logo via `fabric.mod.json` "icon" | NeoForge `logoFile` in `neoforge.mods.toml`, resolved from JAR ROOT (`packResources.getRootResource`) |
| Mixin config: all mixins in `mixins` | Client-only mixins must go in the `"client"` array (DedicatedServer dist-cleaner error otherwise) |
| JNA bundled dependency | Vanilla MC 26.1.2 ships JNA 5.17.0 (+ jorbis, commons-codec/io/lang3) as launcher libraries — do NOT bundle |

## Design Decisions
- Replaced owo-lib with NeoForge `CustomPacketPayload`/`StreamCodec` + `ModConfigSpec` + vanilla widgets (owo-lib has no NeoForge variant)
- Client config registered via `ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)` in `VinURL` constructor
- Mixins loaded at runtime via `[[mixins]]` in `neoforge.mods.toml`; client-only mixins in the `"client"` section
- `commons-exec` SHADED into the jar (`jarLibs` configuration + `jar` task) — JarJar cannot nest non-mod jars; `runtimeClasspath.extendsFrom jarLibs` keeps it visible in dev runs
- JNA NOT bundled (vanilla launcher library 5.17.0; original didn't bundle it either)
- Sound playback: ffmpeg converts .ogg → .wav for JavaSound (`WavAudioStream`); silent `placeholder_sound.ogg` ships in assets so `SoundEngine.play()` doesn't bail on an empty event before `getStream()` runs

## Release Artifacts
- Jar: `build/libs/vinurl-2.6.0.jar` (includes `assets/vinurl/textures/icon.png`)
- Tag: `v2.6.0-mc26.1.2-release-fix2` (pending)
- Commit: `8993de2` + pending fixes commit

## Pending Verification
- User testing of: icon display, whitelist key message, download completion, config persistence
- `runClient` cannot be tested: LWJGL aarch64/x64 mismatch