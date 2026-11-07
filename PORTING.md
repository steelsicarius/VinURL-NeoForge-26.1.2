# PORTING.md — VinURL: Fabric 26.1 → NeoForge 26.1.2

> MEMORIA DE TRABAJO. Leer SIEMPRE al empezar o retomar sesión.

**Origen:** https://github.com/Plompi/VinURL (branch `26.1`, Fabric, v2.6.0) — clonado en `/home/z/my-project/work/VinURL-source` (solo lectura).
**Destino:** `/home/z/my-project/work/VinURL-26.1.2-Neoforge` (MDK ModDevGradle, NeoForge 26.1.2.114, ModDevGradle 2.0.148, Gradle 9.2.1).
**Versiones copiadas del MDK (no inventadas):** `minecraft_version=26.1.2`, `neo_version=26.1.2.114`, plugin `net.neoforged.moddev` `2.0.148`, Gradle wrapper 9.2.1, Java toolchain 25.

---

## FASE ACTUAL

Fase 1 (esqueleto) — COMPLETADA. Fase 2 (registros) — EN CURSO.

## ENTORNO REAL DE TRABAJO (diferencias con lo pedido)

- El sandbox de compilación es **Linux**, no Windows: los comandos `.\gradlew.bat build` se ejecutan aquí como `./gradlew build`. En la máquina Windows del usuario siguen siendo `.\gradlew.bat build` (el repo mantiene `gradlew.bat`).
- `JAVA_HOME`: el sandbox no tiene JDK preinstalado para Java 25; Gradle lo **provisiona automáticamente** vía foojay-resolver (settings.gradle del MDK) a `/home/z/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2`. No hay que fijar JAVA_HOME a mano aquí; en Windows, usar el JDK 25 local real.
- **Límite de memoria del sandbox (4 GB, sin swap):** el paso `decompile` de NeoForm (Vineflower) está configurado con `-Xmx4g` y el kernel OOM-killer lo mata. Workaround aplicado al ejecutar builds aquí: `_JAVA_OPTIONS=-Xmx2900m ./gradlew build --no-daemon`. En una máquina normal (≥8 GB) NO hace falta: ejecutar `.\gradlew.bat build` a secas. Documentado, no afecta al producto final.
- `runClient` requiere ventana gráfica; en este sandbox no hay display ni acceso de usuario al cliente → **verificación visual pendiente** (ver sección final).

## LO HECHO

- [F1] Esqueleto creado desde el MDK: `gradle.properties` (mod_id `vinurl`, versión `2.6.0-26.1.2`, license CC-BY-NC-4.0), `neoforge.mods.toml` reescrito en TOML válido (el MDK adjunto trae `[ods]]`/`[ixins]]` corruptos → corregido a `[[mods]]`; `[[mixins]]` declarado para portar los mixins), `pack.mcmeta` con `min_format: 101` / `max_format: 101` (sin `${mod_id}`).
- [F1] `build.gradle`: `archivesName='VinURL'` (jar final `VinURL-2.6.0-26.1.2.jar`), añadido `mod_url` a `generateModMetadata`.
- [F1] `./gradlew build` del esqueleto **compila** (sin fuentes Java aún) y produce los artefactos MC 26.1.2 + **fuentes decompiladas** para verificación de APIs.
- Fuentes decompiladas extraídas a `/home/z/my-project/work/mc-sources` para verificación.

## DECISIONES DE DISEÑO (y por qué)

1. **owo-lib se elimina** (no existe port verificado para NeoForge 26.1.2). Sustituciones, una por dependencia de owo:
   - `OwoNetChannel` → payloads `CustomPacketPayload` de NeoForge (registrar `RegisterPayloadHandlersEvent`).
   - Config owo (`ClientConfig`+wrapper generado) → `ModConfigSpec` de NeoForge + pantalla de config integrada `ConfigurationScreen` (patrón del MDK).
   - Pantalla `BaseUIModelScreen` (XML owo) → `Screen` vanilla con `EditBox`, `AbstractSliderButton` y botones propios con las mismas texturas (nine-patch dibujado a mano, 4 px de esquina, mismo layout).
   - `KeyedEndec`/`Endec` (NBT tipado) → `CompoundTag` vanilla con claves `"music_url"`, `"lock"`, `"duration"` (mismas que usaba owo), defaults equivalentes (`""`, `false`, `0`).
2. **commons-exec se elimina**: solo se usaba `CommandLine`/`addArguments`/`toStrings` → clase propia `CommandBuilder` (sin dependencias). yt-dlp recibe los mismos argumentos.
3. **Los 2 mixins Fabric se mantienen como mixins** (`vinurl.mixins.json`): `JukeboxBlockEntity` (setTheItem/popOutTheItem/tick) y `Gui.setNowPlaying` no tienen equivalente por eventos en NeoForge 26.1.2 (verificado contra fuentes decompiladas; sin gancho de evento). NeoForge soporta mixins declarándolos en `neoforge.mods.toml`.
4. **`fabric-sound-api-v1:empty` no existe en NeoForge** → se genera un WAV silencioso propio `assets/vinurl/sounds/empty.wav` y `sounds.json` apunta a `vinurl:empty`.
5. **JNA, commons-io, commons-codec, commons-lang3** no se añaden como dependencias: ya vienen con Minecraft (verificado en libraries del pipeline).
6. Versión del jar: `VinURL-2.6.0-26.1.2.jar` (formato sugerido por el objetivo). El original usa `2.6.0+mc26.1`.

## CAMBIOS DE API ENCONTRADOS (antes → después)

(verificados contra `minecraft-patched-26.1.2.114-sources.jar` + `neoforge-26.1.2.114-sources.jar`)

| Fabric 26.1 / original | NeoForge 26.1.2 |
| --- | --- |
| `Registry.register(BuiltInRegistries...)` estático | `DeferredRegister` (Items / SoundEvents) |
| `CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES)` | `BuildCreativeModeTabContentsEvent` (bus del mod), `getTabKey()`/`accept(...)` |
| `OwoNetChannel` (owo) | `CustomPacketPayload` + `RegisterPayloadHandlersEvent.registrar("1").playToClient/playToServer`; handlers en main thread por defecto; envío: `PacketDistributor.sendToPlayer`, cliente→servidor: `connection.send(payload)` |
| `FabricLoader.getInstance().getGameDir()` | `FMLPaths.GAMEDIR.get()` |
| `ItemTooltipCallback.EVENT` (fabric) | `ItemTooltipEvent` (`getItemStack()`, `getToolTip()`) |
| `ClientPlayConnectionEvents.DISCONNECT` | `ClientPlayerNetworkEvent.LoggingOut` |
| `HudElementRegistry.addLast` (fabric) | `RegisterGuiLayersEvent.registerBelowAll(id, GuiLayer)`; `GuiLayer.render(GuiGraphicsExtractor, DeltaTracker)` |
| `KeyMappingHelper.registerKeyMapping` | `RegisterKeyMappingsEvent.register(KeyMapping)` |
| `ClientCommandRegistrationCallback` (fabric) | `RegisterClientCommandsEvent.getDispatcher()` (bus de juego) |
| `ConfigScreen.create(config)` (owo) | `ConfigurationScreen(ModContainer, Screen)` vía `IConfigScreenFactory` |
| `sounds.json` → `fabric-sound-api-v1:empty` | `vinurl:empty` (WAV silencioso propio) |
| `SoundInstance.getAudioStream(SoundBufferLibrary, Identifier, boolean)` | **RENOMBRADO en 26.1.2** → `SoundInstance.getStream(SoundBufferLibrary, Sound, boolean)` |
| `CompoundTag.getString/getBoolean/getInt(key)` | **ELIMINADOS** → `getStringOr/getBooleanOr/getIntOr(key, default)` |
| `SoundInstance.createUnseededRandom()` (package-private en 26.1.2) | `RandomSource.create()` (público, mismo resultado que el body del original) |
| `data/minecraft/tags/items/*.json` (plural) | `data/minecraft/tags/item/*.json` (singular, `Registries.tagsDirPath`); el original estaba en plural → el tag no cargaba; corregido en el port |
| `AbstractWidget.render/renderWidget` | modelo nuevo de render: `extractRenderState(GuiGraphicsExtractor,...)`/`extractWidgetRenderState`/`extractContents`; `Button.OnPress.onPress(Button)`; tooltips con `Tooltip.create(Component)` |
| `AbstractSliderButton.dragging` (privado) | ya `protected` en 26.1.2 + AT de NeoForge — accesible desde subclase |
| `owo-ui XML` + `NinePatchTexture` (owo) | `Screen` vanilla + nine-patch dibujado con `GuiGraphicsExtractor.blit(Identifier, x0,y0,x1,y1, u0,u1,v0,v1)` (esquinas 4 px, mismo layout del XML) |
| mixin targets `JukeboxBlockEntity.setTheItem/popOutTheItem/tick` y `Gui.setNowPlaying` | **existentes** en 26.1.2 (líneas 135/48/64 y 1323 de las fuentes decompiladas) — mixins portables sin cambios de objetivo |
| mixin `compatibilityLevel JAVA_25` | soportado por sponge-mixin 0.17.4 (mixin 0.8.7) en NeoForge 26.1.2 |
| `@EventBusSubscriber` (sin parámetro `bus`) | en NeoForge 26.1.2 el bus se autodetecta (IModBusEvent → bus del mod); anotación solo con `modid`/`value` |

Formatos de datos verificados en el jar de MC 26.1.2: recipe con `ingredients` como strings ✓ (igual que el original), `jukebox_song` idéntico ✓.

## ERRORES ABIERTOS / BLOQUEOS

- (resuelto) OOM del kernel en `decompile` — ver ENTORNO REAL.
- (resuelto) HTTP 429 transitorio de repo.maven.apache.org — reintentar funcionó.
- (resuelto) MDK adjunto con `neoforge.mods.toml` corrupto (`[ods]]`, `[ixins]]`) — reescrito.

## PENDIENTE

- Verificación visual del cliente (capturas) — **Verificación visual pendiente — no se ha podido obtener una captura del cliente** (sandbox sin display; se documentará explícitamente al llegar a Fase 7).
- Criterios de aceptación sobre "Altar de Batalla", spawn de mobs y mensajes de abandono: **no aplicables** — vinURL es un mod de discos musicales; no tiene altar, mobs ni ese flujo. Se asume que eran criterios de otra plantilla de port.
