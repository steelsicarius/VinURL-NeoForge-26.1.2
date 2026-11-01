# VinURL Porting to NeoForge MC 26.1.2

## Current Status
- **Phase**: Release — fixes applied, pending user verification
- **Commit**: 8993de2 + uncommitted fixes (config persistence, whitelist key, icon location)
- **Build**: PASSING (`./gradlew build`)
- **runServer (port 25599)**: PASSING — VinURL 2.6.0 loaded, no exceptions
- **runClient**: NOT TESTED — LWJGL platform mismatch (aarch64 JVM, x64 natives only)

## Issues Fixed — This Session

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

## Design Decisions
- Replaced owo-lib with NeoForge `CustomPacketPayload`/`StreamCodec` + `ModConfigSpec` + vanilla widgets (owo-lib has no NeoForge variant)
- Client config registered via `ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)` in `VinURL` constructor
- Mixins loaded at runtime via `[[mixins]]` in `neoforge.mods.toml`
- `commons-exec` and `jna` as `compileOnly` + `runtimeOnly` (NeoForge ModuleClassLoader visibility)
- Sound playback uses ffmpeg (already bundled) to convert .ogg → .wav for JavaSound compatibility
- ModConfigSpec `ConfigValue.set()` must be called after modifying mutable values (e.g., `List<String>`) to persist to disk

## Release Artifacts
- Jar: `build/libs/vinurl-2.6.0.jar` (includes `assets/vinurl/textures/icon.png`)
- Tag: `v2.6.0-mc26.1.2-release-fix2` (pending)
- Commit: `8993de2` + pending fixes commit

## Pending Verification
- User testing of: icon display, whitelist key message, download completion, config persistence
- `runClient` cannot be tested: LWJGL aarch64/x64 mismatch