# VinURL Porting to NeoForge MC 26.1.2

## Current Status
- **Phase**: Release — investigating remaining regressions
- **Commit**: 7db0cc0, tag v2.6.0-mc26.1.2-release-fix1
- **Build**: PASSING (`./gradlew build`)
- **runServer (port 25599)**: PASSING — VinURL 2.6.0 loaded, no exceptions
- **runClient**: NOT TESTED — LWJGL platform mismatch (aarch64 JVM, x64 natives only)

## Current Status
- **Phase**: Release — all user-reported issues fixed
- **Commit**: pending
- **Build**: PASSING (`./gradlew build`)
- **runServer (port 25599)**: PASSING — VinURL 2.6.0 loaded, no exceptions
- **runClient**: NOT TESTED — LWJGL platform mismatch (aarch64 JVM, x64 natives only)

## Issues Fixed in this session

### 1. Simulate button stuck at "calculating..."
**Root cause**: `URLDiscScreen.simulate` button called `Executable.YT_DLP.getCommandLine()` without first calling `checkForExecutable()`. The executable download runs asynchronously in `VinURLClient.onClientSetup()`, so yt-dlp/deno may not exist yet when the user clicks Simulate.

**Fix**: Added `Executable.YT_DLP.checkForExecutable()` and `Executable.DENO.checkForExecutable()` calls before `getCommandLine()` in the simulate button handler, with error feedback if executables aren't available.

**Files**: `URLDiscScreen.java`

### 2. No sound playback
**Root cause A**: `ClientEvent.handlePlaySound()` was a TODO stub — did nothing when server sent `PlaySoundRecord` payload.
**Root cause B**: `FileSound` extended `AbstractTickableSoundInstance` with `PLACEHOLDER_SOUND` which has no audio data (sounds.json maps to missing `assets/vinurl/sounds/placeholder_sound.ogg`).

**Fix**: 
- Implemented `ClientEvent.handlePlaySound()` and `handleStopSound()` (ported from Fabric logic): parse URL, create FileSound, check if download in progress, check if audio file exists locally, download if needed with whitelist/key-press flow.
- Added `SoundManager.convertToWav()` using ffmpeg (already downloaded) to convert .ogg → .wav after download completes.
- Added `SoundManager.WavAudioStream` class implementing `net.minecraft.client.sounds.AudioStream` wrapping JavaSound `AudioInputStream`.
- Overrode `FileSound.getStream()` to return `CompletableFuture<AudioStream>` from the downloaded WAV file.
- Replaced `VinURLClient.CLIENT` references in SoundManager with `Minecraft.getInstance()` and `VinURLClient.CONFIG` static import.

**Files**: `ClientEvent.java`, `SoundManager.java`, `FileSound.java`

### 3. Missing mod icon (black/blank in GUI)
**Root cause**: `URLDiscScreen.extractRenderState()` called `context.blit(ICON_ID, iconX, iconY, iconSize, iconSize, 0.0f, 0.0f, 1.0f, 1.0f)`. But `GuiGraphicsExtractor.blit(Identifier, int, int, int, int, float, float, float, float)` takes **corner coordinates** (x0, y0, x1, y1) not (x, y, width, height). Also `u1=0.0f` equaled `u0=0.0f`, meaning zero UV width.

**Fix**: Changed to `context.blit(ICON_ID, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f)`.

**Files**: `URLDiscScreen.java`

## API Changes Found (Fabric → NeoForge MC 26.1.2)

| Fabric (26.1) | NeoForge (26.1.2) |
|---|---|
| `owo-network` `NETWORK_CHANNEL.clientHandle().send()` | `Minecraft.getInstance().getConnection().send(ServerboundCustomPayloadPacket)` |
| `owo-config` `ConfigScreen.create()` | Manual vanilla `Screen` with `ModConfigSpec.ConfigValue.set()` |
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

## Design Decisions
- Replaced owo-lib with NeoForge `CustomPacketPayload`/`StreamCodec` + `ModConfigSpec` + vanilla widgets (owo-lib has no NeoForge variant)
- Client config registered via `ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)`
- Mixins loaded at runtime via `[[mixins]]` in `neoforge.mods.toml`
- `commons-exec` and `jna` as `compileOnly` + `runtimeOnly` (NeoForge ModuleClassLoader visibility)
- `commons-codec`, `commons-io`, `jorbis` are transitive dependencies available at runtime
- Sound playback uses ffmpeg (already bundled) to convert .ogg → .wav for JavaSound compatibility

## Issues Fixed (this session)

### 1. `VinURLClient.CLIENT` is null → NullPointerException
**Root cause**: `public static final Minecraft CLIENT = Minecraft.getInstance();` is a static field initialized at class-load time. `Minecraft.getInstance()` returns null if Minecraft isn't fully initialized yet when the class is loaded.

**Affected files**:
- `URLDiscScreen.java` — Done button and keyPressed(ESC/ENTER) used `VinURLClient.CLIENT.getConnection()`
- `Commands.java` — openConfig used `CLIENT.execute()` / `CLIENT.setScreen()`
- `ProgressOverlay.java` — renderText used `CLIENT.font` / `CLIENT.getWindow()`
- `SoundManager.java` — playSound/stopSound used `CLIENT.getSoundManager()` / `CLIENT.gui`

**Fix**: Replaced all `VinURLClient.CLIENT` / `CLIENT` usages with `Minecraft.getInstance()` called lazily at point of use. Removed the static `CLIENT` field from `VinURLClient.java`.

**Result**: /vinurl config works, Done button works, ESC key exits menu.

### 2. commons-exec ClassNotFoundException
**Root cause**: `implementation` scope doesn't make dependencies visible to NeoForge's `ModuleClassLoader` at runtime. NeoForge isolates mod classloaders; `compileOnly` + `runtimeOnly` is needed.

**Affected**: `URLDiscScreen.simulate` button — `Executable.YT_DLP.getCommandLine()` → `new CommandLine(FILE_PATH)` → ClassNotFoundException.

**Fix**: Changed `commons-exec` and `jna` from `implementation` to `compileOnly` + `runtimeOnly` in build.gradle.

**Result**: Simulate button runs yt-dlp duration check correctly.

### 3. Simulate button doesn't advance / no sound
**Root cause**: Same as #2 — commons-exec not found, so yt-dlp never runs.

**Fix**: Fix #2.

### 4. Done button does nothing
**Root cause**: Same as #1 — NPE prevented `onClose()` from being reached.

**Fix**: Fix #1.

### 5. ESC key doesn't exit menu
**Root cause**: Same as #1 — NPE on `VinURLClient.CLIENT.getConnection()`.

**Fix**: Fix #1.

## API Changes Found (Fabric → NeoForge MC 26.1.2)

| Fabric (26.1) | NeoForge (26.1.2) |
|---|---|
| `owo-network` `NETWORK_CHANNEL.clientHandle().send()` | `Minecraft.getInstance().getConnection().send(ServerboundCustomPayloadPacket)` |
| `owo-config` `ConfigScreen.create()` | Manual vanilla `Screen` with `ModConfigSpec.ConfigValue.set()` |
| `Screen.keyPressed(int,int,int)` | `Screen.keyPressed(KeyEvent)` |
| `Screen.render(GuiGraphics,int,int,float)` | `Screen.extractRenderState(GuiGraphicsExtractor,int,int,float)` |
| `BaseUIModelScreen` (owo-ui) | Vanilla `Screen` + `Button.builder` + `EditBox` |
| `GLFW.GLFW_KEY_ESCAPE` | `KeyEvent.key() == 256` |
| `CompoundTag.getBoolean()` returns boolean | Returns `Optional<Boolean>`, use `.orElse(false)` |
| `CreativeModeTabs.TOOLS_AND_UTILITIES` (public) | Construct `ResourceKey` directly |
| `fabric-api` mixin annotation processor | Mixins load at runtime via `[[mixins]]` in `neoforge.mods.toml` |
| `VinURLClient.CLIENT = Minecraft.getInstance()` static field | Call `Minecraft.getInstance()` lazily at point of use |

## Design Decisions
- Replaced owo-lib with NeoForge `CustomPacketPayload`/`StreamCodec` + `ModConfigSpec` + vanilla widgets (owo-lib has no NeoForge variant)
- Client config registered via `ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)`
- Mixins loaded at runtime via `[[mixins]]` in `neoforge.mods.toml`
- `commons-exec` and `jna` as `compileOnly` + `runtimeOnly` (NeoForge ModuleClassLoader visibility)

## Release Artifacts
- Jar: `build/libs/vinurl-2.6.0.jar`
- Tag: `v2.6.0-mc26.1.2-release-fix1`
- Commit: `6fef9ab`
