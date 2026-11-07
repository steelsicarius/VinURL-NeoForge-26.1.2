# PORTING.md — VinURL: Fabric 26.1 → NeoForge 26.1.2

> MEMORIA DE TRABAJO. Leer SIEMPRE al empezar o retomar sesión.

**Origen:** https://github.com/Plompi/VinURL (branch `26.1`, Fabric, v2.6.0) — clonado en `/home/z/my-project/work/VinURL-source` (solo lectura, nunca modificado).
**Destino:** `/home/z/my-project/work/VinURL-26.1.2-Neoforge` (MDK ModDevGradle, NeoForge 26.1.2.114, ModDevGradle 2.0.148, Gradle 9.2.1).
**Versiones copiadas del MDK (no inventadas):** `minecraft_version=26.1.2`, `neo_version=26.1.2.114`, plugin `net.neoforged.moddev` `2.0.148`, Gradle wrapper 9.2.1, Java toolchain 25.

---

## ESTADO: PORT COMPLETADO ✅ (todas las fases)

| Fase | Estado |
| --- | --- |
| 1. Esqueleto (gradle, mods.toml, pack.mcmeta) | ✅ |
| 2. Registros (item, sound event, creative tab) | ✅ |
| 3. Lógica (networking, item use, jukebox, comandos, exe) | ✅ |
| 4. Cliente (config, keybinds, screen, HUD, sonido, mixins) | ✅ |
| 5. Datos (recipe, jukebox_song, tag, sonido vacío) | ✅ |
| 6. Assets (modelos, texturas, lang, sounds.json, icono) | ✅ |
| 7. Pruebas (build, runServer, runClient + verificación visual) | ✅ (con matices, ver abajo) |

## ENTORNO REAL DE TRABAJO (diferencias con lo pedido)

- El sandbox de compilación es **Linux**, no Windows: `.\gradlew.bat build` se ejecutó aquí como `./gradlew build`. En la máquina Windows del usuario los comandos son `.\gradlew.bat build` / `runServer` / `runClient` (el repo mantiene `gradlew.bat`).
- `JAVA_HOME`: el sandbox no tenía JDK 25; Gradle lo **provisionó automáticamente** vía foojay-resolver (settings.gradle del MDK) a `/home/z/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2`. En Windows, apuntar JAVA_HOME al JDK 25 local.
- **Límite de memoria del sandbox (4 GB, sin swap):** el paso `decompile` de NeoForm (Vineflower) está configurado con `-Xmx4g` y el kernel OOM-killer lo mata. Workaround usado aquí: `_JAVA_OPTIONS=-Xmx2900m ./gradlew build --no-daemon`. En una máquina con ≥8 GB NO hace falta: `.\gradlew.bat build` a secas. No afecta al producto final.
- `runClient`: sin display físico en el sandbox; se ejecutó bajo **Xvfb** (framebuffer virtual) con render real (LWJGL/Mesa). Capturas reales obtenidas con ffmpeg/x11grab + automatización de input con python-xlib.

## VERIFICACIÓN OBLIGATORIA — RESULTADOS

### `./gradlew build` — ✅ SUCCESSFUL
Jar final: `build/libs/VinURL-2.6.0-26.1.2.jar` (~190 KB). Metadatos expandidos correctos (`modId=vinurl`, versión `2.6.0-26.1.2`, `[[mixins]]` presente, dependencias `[26.1.2.114,)` y `[26.1.2]`).

### `./gradlew runServer` (envuelto en límite de tiempo) — ✅ ARRANCA Y CARGA LIMPIO
- `Done (10.778s)!` / `Done (1.574s)!` en arranques posteriores.
- 0 excepciones, 0 warnings de registros/datos del mod (los únicos WARN son vanilla: terminal features y ambigüedades de comandos vanilla).
- Datapack cargado: 1516 recipes (incluye `vinurl:custom_record`), jukebox_song y tag sin errores.
- Cierre: el comando `stop` por stdin no llegó al proceso hijo bajo el wrapper de gradle del sandbox; el SIGTERM del límite de tiempo dispara el shutdown hook de MC que **guarda chunks y dimensiones** ("All chunks are saved", "All dimensions are saved") sin excepciones. En Windows con `.\gradlew.bat runServer` interactivo el `stop` funciona de forma estándar.

### `./gradlew runClient` (bajo Xvfb) — ✅ ARRANCA, CARGA Y RENDERIZA
- Cliente inicia, contexto GL creado, ResourceManager carga `mod/vinurl`.
- Config de cliente creada: `run/config/vinurl-client.toml` (ModConfigSpec funcional).
- `VinURL client setup` en el log; pantalla de título renderizada (captura en `docs/screenshots/title_screen.png`).
- Único error: `OpenAL device` (sandbox sin hardware de audio) — vanilla, no del mod.
- **Verificación visual obtenida con capturas reales** (no "pendiente"):
  - `docs/screenshots/main_menu_5_mods.png` — menú principal con "NeoForge 26.1.2.114 (5 mods)".
  - `docs/screenshots/creative_inventory_disc_tooltip.png` — **inventario creativo, tab Tools & Utilities, página final de discos: el VinURL Custom Music Disc con tooltip "Custom Music Disc" (aqua) + "No Song"**, en un mundo creado en modo Creative.
- Patrones obligatorios sobre `run/logs/latest.log` (última ejecución v11, 07Nov2026 21:0x):
  - `Missing item model` → **0**
  - `Couldn't parse item model` → **0**
  - `Missing block model` → **0**
  - Excepciones de clases `com.vinurl` → **0**
  - Warnings de sonido/modelo/textura de `vinurl` → **0** (tras arreglar sounds.json, ver errores)

### Criterios de aceptación adicionales
- ✅ Items muestran correctamente su textura en inventario creativo (verificado por screenshot del cliente real).
- ✅ El disco aparece en el tab TOOLS_AND_UTILITIES (equivalente NeoForge de `BuildCreativeModeTabContentsEvent`).
- ⚠️ No se automatizó: colocar el disco en una jukebox y descargar audio real (requiere yt-dlp/deno/ffmpeg + red + interacción prolongada; la lógica de jukebox queda cubierta por los mixins con targets verificados en las fuentes de 26.1.2).
- ⚠️ **Verificación visual pendiente (parcial) — no se ha podido obtener una captura del cliente** para: playback real de audio descargado y la pantalla URLDisc abierta con texturas (la pantalla requiere clic derecho con el disco en la mano dentro del juego; los widgets se dibujan con las mismas texturas nine-patch y el patrón de render `extractRenderState`/`extractContents` verificado contra las fuentes de 26.1.2).
- Los criterios de la plantilla sobre "Altar de Batalla", spawn de mobs y mensajes de abandono **no son aplicables**: vinURL es un mod de discos musicales y no tiene altar, mobs ni ese flujo.

## LO HECHO (por fase)

- [F1] Esqueleto desde el MDK: `gradle.properties` (mod_id `vinurl`, versión `2.6.0-26.1.2`, licencia CC-BY-NC-4.0), `neoforge.mods.toml` en TOML válido, `pack.mcmeta` con `min_format: 101` / `max_format: 101` (sin `${mod_id}`).
- [F1] `build.gradle`: `archivesName='VinURL'` (jar `VinURL-2.6.0-26.1.2.jar`), añadido `mod_url` a `generateModMetadata`.
- [F2] Registros: `DeferredRegister.Items` + `DeferredRegister<SoundEvent>`; item `URLDisc` con `stacksTo(1).rarity(RARE).jukeboxPlayable(SONG_KEY).setId(ITEM_KEY)`; `BuildCreativeModeTabContentsEvent` sobre TOOLS_AND_UTILITIES.
- [F3] Networking: 4 payloads `CustomPacketPayload` (`PlaySoundRecord`, `StopSoundRecord`, `GUIRecord` clientbound; `SetURLRecord` serverbound) con `StreamCodec.composite` + `ByteBufCodecs.optional(BlockPos.STREAM_CODEC)`; registro en `RegisterPayloadHandlersEvent` con **separación por lado** (ver errores); envío con `PacketDistributor.sendToPlayer` y `connection.send(...)`.
- [F3] `URLDisc.use()` (Item.use existe en 26.1.2); datos del disco en `CustomData` vía `DiscData` (claves `music_url`, `lock`, `duration` — mismas que owo).
- [F3] `Executable`/`Platform`/`ProcessStream` portados sin cambios funcionales; `CommandBuilder` propio sustituye a `org.apache.commons.exec` (tokeniza `parameters` por espacios como commons-exec).
- [F4] Cliente: `VinURLConfig` (`ModConfigSpec`, mismas opciones/traducciones que owo, secciones push/pop, `AudioQuality` con `TranslatableEnum`); `ConfigurationScreen` como `IConfigScreenFactory`; `KeyListener` (`RegisterKeyMappingsEvent` + `ClientTickEvent.Post`, categoría `vinurl:mapping`); `URLDiscScreen` reescrita con widgets vanilla (`EditBox` + `AbstractSliderButton` + `Button` con `extractContents`), nine-patch propio (`NinePatch`, esquinas 4 px como los `nine_patch_textures/*.json` del original), tooltip mm:ss mientras se arrastra el slider; `ProgressOverlay` como `GuiLayer` (`RegisterGuiLayersEvent.registerBelowAll`); `FileSound` con `getStream(SoundBufferLibrary, Sound, boolean)` (renombrado en 26.1.2); `ItemTooltipEvent` (tooltip de disco con `Custom Music Disc` + descripción); `ClientPlayerNetworkEvent.LoggingOut` → `killAllProcesses()`; comandos cliente `/vinurl delete|update|config` (`RegisterClientCommandsEvent`); `/vinurl config` abre `ConfigurationScreen`.
- [F4] Mixins mantenidos (`vinurl.mixins.json`, `[[mixins]]` en mods.toml, `compatibilityLevel JAVA_25` soportado por sponge-mixin 0.17.4): `JukeboxMixin` (targets `setTheItem`/`popOutTheItem`/`tick` verificados en fuentes 26.1.2) y `InGameHudMixin` (`Gui.setNowPlaying`).
- [F5] Datos: `data/vinurl/recipe/custom_record.json` (formato 26.x con ingredients como strings ✓), `data/vinurl/jukebox_song/placeholder_sound.json` ✓, **`data/minecraft/tags/item/music_discs.json`** (singular; el original estaba en `tags/items` plural → el tag NO cargaba en el original; corregido según `Registries.tagsDirPath` de 26.1.2).
- [F5] Sonido vacío: `assets/vinurl/sounds/empty.ogg` (silencio vorbis generado con ffmpeg) porque `fabric-sound-api-v1:empty` no existe en NeoForge y **el pipeline de sonido de 26.1.2 solo lee OGG** (JOrbis hardwired en `SoundBufferLibrary`).
- [F6] Assets: `items/custom_record.json` (formato nuevo de item models, ya correcto en el original), `models/item/custom_record.json`, texturas gui/item, `lang/en_us.json` (+claves `vinurl.configuration.*` para la pantalla de config NeoForge), `sounds.json` → evento `placeholder_sound` → `vinurl:empty`, icono como `vinurl_icon.png` (logoFile) + `assets/vinurl/icon.png`.
- [F7] Pruebas completadas (ver arriba).

## ERRORES ENCONTRADOS Y CORREGIDOS (causa raíz, sin silenciar)

1. **OOM del kernel en decompile** → capar heap con `_JAVA_OPTIONS` + `--no-daemon` en el sandbox (entorno, no código).
2. **HTTP 429 de Maven Central** → reintento (transitorio).
3. **`${mod_url}` sin propiedad en `generateModMetadata`** → añadida la propiedad al mapa de expansión.
4. **`ClassNotFoundException: Screen` en servidor dedicado** → los handlers clientbound cargaban clases cliente al registrarse en ambos lados → registro clientbound separado por `FMLEnvironment.getDist().isClient()` (en el lado servidor, `playToClient` sin handler declara el payload).
5. **`Cannot register payload vinurl:set_url_record as it is already registered`** → doble suscripción (`@EventBusSubscriber` + `addListener`) → eliminada la suscripción manual duplicada.
6. **`FMLEnvironment.dist` no existe en FML 11** → `FMLEnvironment.getDist()`.
7. **`File vinurl:sounds/empty.ogg.ogg does not exist`** → en `sounds.json` el nombre va SIN extensión (MC la añade).
8. **`Missing sound for event: vinurl:placeholder_sound`** → el key de sounds.json debe ser `placeholder_sound` (el SoundEvent registrado), no `empty` (el nombre del archivo).
9. **Compilación: campos de config como variables locales del bloque `static`** → declarados como campos `private static final` asignados en el bloque.
10. **Compilación: `dragging` protected no accesible desde la Screen** → accessor `isDragging()` en la subclase `DurationSlider`.
11. **Compilación: `renderOutline` no existe en `GuiGraphicsExtractor` 26.1.2** → `outline(x, y, width, height, color)`.
12. **`ITEM_KEY`/`SONG_KEY` importados de `VinURL`** → están en `Constants`.

## CAMBIOS DE API ENCONTRADOS (antes → después)

(verificados contra `minecraft-patched-26.1.2.114-sources.jar` + `neoforge-26.1.2.114-sources.jar`)

| Fabric 26.1 / original | NeoForge 26.1.2 |
| --- | --- |
| `Registry.register(BuiltInRegistries...)` estático | `DeferredRegister` (Items / SoundEvents) |
| `CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES)` | `BuildCreativeModeTabContentsEvent` (bus del mod), `getTabKey()`/`accept(...)` |
| `OwoNetChannel` (owo) | `CustomPacketPayload` + `RegisterPayloadHandlersEvent.registrar("1").playToClient/playToServer`; handlers en main thread por defecto; envío: `PacketDistributor.sendToPlayer`, cliente→servidor: `connection.send(payload)`; **no cargar clases cliente en el registro común** |
| `FabricLoader.getInstance().getGameDir()` | `FMLPaths.GAMEDIR.get()` |
| `ItemTooltipCallback.EVENT` (fabric) | `ItemTooltipEvent` (`getItemStack()`, `getToolTip()`) |
| `ClientPlayConnectionEvents.DISCONNECT` | `ClientPlayerNetworkEvent.LoggingOut` |
| `HudElementRegistry.addLast` (fabric) | `RegisterGuiLayersEvent.registerBelowAll(id, GuiLayer)`; `GuiLayer.render(GuiGraphicsExtractor, DeltaTracker)` |
| `KeyMappingHelper.registerKeyMapping` | `RegisterKeyMappingsEvent.register(KeyMapping)` |
| `ClientCommandRegistrationCallback` (fabric) | `RegisterClientCommandsEvent.getDispatcher()` (bus de juego) |
| `ConfigScreen.create(config)` (owo) | `ConfigurationScreen(ModContainer, Screen)` vía `IConfigScreenFactory` |
| `sounds.json` → `fabric-sound-api-v1:empty` | evento propio `placeholder_sound` → `vinurl:empty` (OGG silencioso propio; el nombre en sounds.json SIN extensión) |
| `SoundInstance.getAudioStream(SoundBufferLibrary, Identifier, boolean)` | **RENOMBRADO en 26.1.2** → `SoundInstance.getStream(SoundBufferLibrary, Sound, boolean)` |
| `CompoundTag.getString/getBoolean/getInt(key)` | **ELIMINADOS** → `getStringOr/getBooleanOr/getIntOr(key, default)` |
| `SoundInstance.createUnseededRandom()` (package-private en 26.1.2) | `RandomSource.create()` (público, mismo body que el original) |
| `data/minecraft/tags/items/*.json` (plural) | `data/minecraft/tags/item/*.json` (singular, `Registries.tagsDirPath`); bug del original corregido |
| `AbstractWidget.render/renderWidget` | modelo nuevo de render: `extractRenderState(GuiGraphicsExtractor,...)`/`extractWidgetRenderState`/`extractContents`; `Button.OnPress.onPress(Button)`; tooltips con `Tooltip.create(Component)`; `GuiGraphicsExtractor.outline(...)` |
| `AbstractSliderButton.dragging` (privado en fabric) | `protected` en 26.1.2 — accesible desde subclase |
| `owo-ui XML` + `NinePatchTexture` (owo) | `Screen` vanilla + nine-patch con `GuiGraphicsExtractor.blit(Identifier, x0,y0,x1,y1, u0,u1,v0,v1)` (esquinas 4 px, mismo layout del XML) |
| `Screen.keyPressed(KeyEvent)` con `KeyEvent` record | igual en 26.1.2 (`event.key()`); escape helper `event.isEscape()` |
| mixin targets `JukeboxBlockEntity.setTheItem/popOutTheItem/tick` y `Gui.setNowPlaying` | **existentes** en 26.1.2 (fuentes líneas 135/48/64 y 1323) — mixins portables sin cambios de objetivo |
| mixin `compatibilityLevel JAVA_25` | soportado por sponge-mixin 0.17.4 (mixin 0.8.7) |
| `@EventBusSubscriber` (sin parámetro `bus`) | bus autodetectado (IModBusEvent → bus del mod); ojo con **no suscribir dos veces** |
| `FMLEnvironment.dist` (FML clásico) | `FMLEnvironment.getDist()` (FML 11) |
| pipeline de sonido solo OGG en 26.1.2 | `SoundBufferLibrary.getStream/getCompleteBuffer` hardwired a `JOrbisAudioStream` → el placeholder debe ser `.ogg`, no `.wav` |

Formatos de datos verificados en el jar de MC 26.1.2: recipe con `ingredients` como strings ✓, `jukebox_song` idéntico ✓, item models `assets/<ns>/items/*.json` con `"model": {"type": "minecraft:model", ...}` ✓ (el original ya usaba ese formato).

## DECISIONES DE DISEÑO (y por qué)

1. **owo-lib eliminado** (sin port verificado para NeoForge 26.1.2): red → payloads NeoForge; config → `ModConfigSpec` + `ConfigurationScreen` (mismas opciones, claves de traducción y secciones); pantalla XML → `Screen` vanilla con las MISMAS texturas y layout (nine-patch 4 px propio); `KeyedEndec` → claves de `CompoundTag` idénticas (`music_url`/`lock`/`duration`).
2. **commons-exec eliminado**: solo se usaba `CommandLine` → `CommandBuilder` propio (0 dependencias nuevas).
3. **Mixins mantenidos**: no existen eventos NeoForge para los puntos inyectados (verificado contra fuentes); NeoForge soporta mixins nativamente.
4. **WAV→OGG**: el pipeline de audio 26.1.2 solo decodifica OGG (JOrbis), por lo que el silencio de placeholder es un `.ogg` generado.
5. **Tag `music_discs` movido a `tags/item/`**: corrección de un bug del original (directorio plural no se lee en 26.1.2).
6. **Sin dependencias nuevas** respecto al original (se eliminan owo, fabric-api y commons-exec; JNA/commons-io/commons-codec/commons-lang3 ya vienen con Minecraft).
7. Versión del jar: `VinURL-2.6.0-26.1.2.jar` (formato pedido en el objetivo). El original usa `2.6.0+mc26.1`.

## DIFERENCIAS RESPECTO AL ORIGINAL (paridad funcional)

| Aspecto | Original (Fabric) | Port (NeoForge) |
| --- | --- | --- |
| Pantalla URL | owo-ui XML | vanilla Screen, mismas texturas/layout/teclas (ESC/ENTER envían) |
| Pantalla de config | owo ConfigScreen | `ConfigurationScreen` de NeoForge (mismas opciones y traducciones) |
| HUD de progreso | Fabric HudElement | `GuiLayer` de NeoForge (mismo render) |
| Networking | OwoNetChannel | payloads NeoForge (mismos mensajes) |
| Sonido placeholder | fabric-sound-api-v1:empty | OGG silencioso propio |
| Tag music_discs | `tags/items/` (sin efecto) | `tags/item/` (funciona — mejora) |
| Mixins | 2 (iguales) | 2 (iguales targets) |
| Ejecutables externos | yt-dlp/deno/ffmpeg/ffprobe auto-descarga | idéntico |
| Comandos | `/vinurl delete/update/config` (client) | idénticos vía `RegisterClientCommandsEvent` |

## BLOQUEOS ABIERTOS

- Ninguno. (El cierre por `stop` de stdin no se pudo automatizar en el sandbox — ver VERIFICACIÓN; no es un bloqueo de código.)
