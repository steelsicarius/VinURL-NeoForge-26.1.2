# VinURL Porting to NeoForge MC 26.1.2

## Current Status
- **Phase**: Bugfix / Release candidate
- **Commit**: eb0f1fc, tag v2.6.0-mc26.1.2-release
- **Build**: PASSING
- **runServer (port 25599)**: PASSING
- **Known issues (this session)**: config NPE, simulate ClassNotFoundException, Done/ESC NPE — all root cause identified

## Issues Found & Fixed (this session)

### 1. `VinURLClient.CLIENT` is null → NullPointerException
**Root cause**: `public static final Minecraft CLIENT = Minecraft.getInstance();` is a static field initialized at class-load time. `Minecraft.getInstance()` returns null if Minecraft isn't fully initialized yet when the class is loaded.

**Affected files**:
- `URLDiscScreen.java` — uses `VinURLClient.CLIENT.execute()` and `VinURLClient.CLIENT.getConnection()` in Done button and keyPressed(ESC/ENTER)
- `Commands.java` — uses `CLIENT.execute()` and `CLIENT.setScreen()` in openConfig
- `ProgressOverlay.java` — uses `CLIENT.font`, `CLIENT.getWindow()`
- `SoundManager.java` — uses `CLIENT.getSoundManager()`, `CLIENT.gui`

**Fix**: Replace all `VinURLClient.CLIENT` / `CLIENT` usages with `Minecraft.getInstance()` called at the point of use. Remove the static `CLIENT` field from `VinURLClient.java`.

**API change**: None (Fabric also used `Minecraft.getInstance()` via static field; NeoForge requires calling it lazily at point of use).

### 2. commons-exec ClassNotFoundException
**Root cause**: `implementation "org.apache.commons:commons-exec:1.6.0"` in build.gradle doesn't make the dependency visible to NeoForge's `ModuleClassLoader` at runtime. NeoForge isolates mod classloaders; `implementation` is compile-time only, `runtimeOnly` is needed for runtime visibility.

**Affected**: `URLDiscScreen.simulate` button — calls `Executable.YT_DLP.getCommandLine()` → `new CommandLine(FILE_PATH)` → ClassNotFoundException.

**Fix**: Change `implementation` → `runtimeOnly` for `commons-exec` and `jna` in build.gradle.

### 3. Simulate button doesn't advance / no sound
**Root cause**: Same as #2 — commons-exec not found, so the yt-dlp duration check never runs.

**Fix**: Fix #2.

### 4. Done button does nothing
**Root cause**: Same as #1 — NPE on `VinURLClient.CLIENT.getConnection()`.

**Fix**: Fix #1.

### 5. ESC key doesn't exit menu
**Root cause**: Same as #1 — NPE on `VinURLClient.CLIENT.getConnection()` before `onClose()` is reached.

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

## Pending Tasks
- [ ] Apply fixes for issues #1-#5 above
- [ ] Rebuild and verify `./gradlew build` passes
- [ ] Test `./gradlew runServer` on port 25599
- [ ] Test `./gradlew runClient` (if graphical environment available)
- [ ] Final commit and tag

## Design Decisions
- Replaced owo-lib with NeoForge `CustomPacketPayload`/`StreamCodec` + `ModConfigSpec` + vanilla widgets (owo-lib has no NeoForge variant)
- Client config registered via `ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)`
- Mixins loaded at runtime via `[[mixins]]` in `neoforge.mods.toml` (annotation processor can't access deobfuscated MC classes)
- `commons-exec` and `jna` kept as direct dependencies (same as Fabric origin)
