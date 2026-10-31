# PORTING.md — VinURL: Fabric 26.1 → NeoForge 26.1.2

> Memoria de trabajo. Leer SIEMPRE al empezar o retomar la sesión. Actualizar tras cada cambio de fase.

## Estado actual
**Fase 0 — Preparar el esqueleto** (en curso)

## Inventario del mod origen (VinURL 2.6.0, MC 26.1, Fabric)
- Mod id: `vinurl`, paquete: `com.vinurl`, versión: `2.6.0` (+mc26.1)
- Item: `vinurl:custom_record` (URLDisc, RARA, stack 1, jukeboxPlayable(SONG_KEY), setId(ITEM_KEY))
- Sound event: `vinurl:placeholder_sound` (variable range)
- Jukebox song (data): `vinurl:placeholder_sound` (length 3600s, comparator 15, desc translatable)
- Tag: `minecraft:music_discs` → vinurl:custom_record
- Recipe: `data/vinurl/recipe/custom_record.json` (shapeless: note_block + 8 colores de lana)
- Creative tab: se añade a `TOOLS_AND_UTILITIES`
- Mixins: `JukeboxMixin` (JukeboxBlockEntity: setTheItem/popOutTheItem/tick), `InGameHudMixin` (Gui.setNowPlaying)
- Red (owo.network): PlaySoundRecord, StopSoundRecord, GUIRecord (clientbound), SetURLRecord (serverbound)
- Cliente: KeyListener (tecla Y), Commands (/vinurl delete|update|config, cliente), SoundManager (yt-dlp/ffmpeg/deno vía commons-exec), FileSound, SkippableAudioStream, ClientConfig (owo.config), URLDiscScreen (owo.ui XML), ProgressOverlay (HudElementRegistry), VinURLClient
- Commons: Executable, Platform (jna), ProcessStream, Util.Url, Constants
- Dependencias origen: fabric-api, fabric-loader, owo-lib 0.13.0+26.1, commons-exec 1.6.0 (incluido), jorbis, jna (vía minecraft)
- Assets: icon, lang en_us, item model, textures, 9 nine_patch jsons, owo_ui XML, sounds.json (placeholder_sound → fabric-sound-api-v1:empty)
- Sin pack.mcmeta en el origen (añadir para NeoForge)

## Plan de fases
0. Esqueleto: build.gradle, gradle.properties, settings.gradle, neoforge.mods.toml (template), mod id, paquete. ← aquí
1. Registros: item, sound event, creative tab (BuildCreativeModeTabContentsEvent), tags/recipe/jukebox_song (datos)
2. Lógica: URLDisc item, VinURLSound, ServerEvent/ClientEvent red (NeoForge SimpleChannel o owo-network), mixins, Executable suite
3. Cliente: keybinding, client commands (si existen en 26.1.2), client config (ModConfigSpec o owo-config), URLDiscScreen (owo-ui o reescrito en widgets vanilla), SoundManager/FileSound, progress overlay (RenderGuiEvent si no hay owo-hud), tooltips
4. Datos: verificar formatos 26.1.2 (recipe, tag, jukebox_song, pack.mcmeta format)
5. Assets: copiar texturas/modelos/lang/sounds.json; sonidos: reemplazar `fabric-sound-api-v1:empty` si no existe en NF
6. Pruebas: build, runServer (puerto 25599), runClient si hay entorno gráfico; release, commit, tag

## Decisiones de diseño (tomadas)
- Versiones NeoForge/MC/plugin moddev: copiar del MDK (mc 26.1.2, neo 26.1.2.114, moddev 2.0.148).
- build.gradle: versión final `2.6.0-mc26.1.2` → jar `vinurl-2.6.0-mc26.1.2.jar`.
- Paquete base y mod id: se mantienen (`com.vinurl`, `vinurl`) para paridad de IDs de registros y recursos.
- Dependencias: no añadir nada que el origen no tuviera; commons-exec/commons-io/commons-codec se declaran explícitamente (en Fabric venían de fabric-api).
- owo-lib: intentar mantener (owo.network, owo.config, owo.ui) si existe variante neoforge para 26.1.2; si no, reemplazar por SimpleChannel + ModConfigSpec + pantalla vanilla.

## Cambios de API confirmados / pendientes
- (pendiente) verificar owo-lib neoforge 26.1.2 en maven.wispforest.io
- (pendiente) verificar eventos cliente de NeoForge 26.1.2: teclas, ticks, comandos cliente, desconexión, tooltips, HUD
- (pendiente) verificar targets de mixin en JukeboxBlockEntity/Gui (setTheItem, popOutTheItem, tick, setNowPlaying)
- (pendiente) verificar pack.mcmeta pack_format de 26.1.2

## Errores abiertos
- Ninguno aún

## Log de cambios
- 2026-10-31: fase 0 iniciada; inventario del origen completado; PORTING.md creado

## Log de cambios
- 2026-10-31: fase 0 completada: build.gradle, gradle.properties, settings.gradle, neoforge.mods.toml creados. Gradle configura sin errores.
- 2026-10-31: iniciando Fase 1 (Registros)

## Cambios de API confirmados
- Fabric `Registry.register()` → NeoForge `DeferredRegister` + `DeferredHolder.get()`
- Fabric `CreativeModeTabs.TOOLS_AND_UTILITIES` (field) → NeoForge: construir `ResourceKey.create(Registries.CREATIVE_MODE_TAB, id)` directamente (campo privado)
- `owo.network.OwoNetChannel` → NeoForge `CustomPacketPayload` + `StreamCodec` + `NetworkRegistry`
- `owo.config @Config` → NeoForge `ModConfigSpec` + `ModContainer.registerConfig`
- `owo.ui` → widgets vanilla (Button/EditBox/Slider) o pantalla dibujada a mano

## Pendientes Fase 1
- Registrar item `vinurl:custom_record` con `DeferredRegister.Items`
- Registrar sound event `vinurl:placeholder_sound`
- Registrar creative tab entry via `BuildCreativeModeTabContentsEvent`
- Datos: recipe, tag, jukebox_song (verificar formatos 26.1.2)

## Log de cambios
- 2026-10-31: fase 0 completada: build.gradle, gradle.properties, settings.gradle, neoforge.mods.toml creados. Gradle configura sin errores.
- 2026-10-31: Fase 1 completada: Constants.java, VinURL.java, URLDisc.java creados y compilan. DeferredRegister para items y sound events, BuildCreativeModeTabContentsEvent para creative tab.

## Cambios de API confirmados
- Fabric `Registry.register()` → NeoForge `DeferredRegister` + `DeferredHolder.get()`
- `CreativeModeTabs.TOOLS_AND_UTILITIES` (field) → `ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"))`
- `CompoundTag.getBoolean()` → returns `Optional<Boolean>` in MC 26.1.2, use `.orElse(false)`
- `FMLPaths` is at `net.neoforged.fml.loading.FMLPaths` (not neoforge.common.util)
- `@Mod` from `net.neoforged.fml.common.Mod`, constructor can inject `IEventBus`
- `FMLCommonSetupEvent` / `FMLClientSetupEvent` from `net.neoforged.fml.event.lifecycle`

## Pendientes Fase 2
- NeoForge networking: CustomPacketPayload records + StreamCodec + RegisterPayloadHandlersEvent
- ServerEvent.register() with playToServer
- ClientEvent handlers with playToClient
- Executable suite (Executable, Platform, ProcessStream, Url)
- Mixins (JukeboxMixin, InGameHudMixin)

## Log de cambios
- 2026-10-31: Fase 2 completada: Payloads.java (CustomPacketPayload records + StreamCodecs), Network.java (RegisterPayloadHandlersEvent subscriber), ServerEvent.java, ClientEvent.java (stub), Executable suite (Url, Platform, ProcessStream, Executable). Todo compila.

## Cambios de API confirmados adicionales
- `CustomPacketPayload` requires `type()` method returning `CustomPacketPayload.Type<?>`
- `@EventBusSubscriber` has only `modid()` and `value()` (no `bus` param in 26.1.2)
- `BlockPos.STREAM_CODEC` is `StreamCodec<ByteBuf, BlockPos>`, use `.cast()` for `RegistryFriendlyByteBuf`
- `CompoundTag.getBoolean()` returns `Optional<Boolean>` (use `.orElse(false)`)
- `ByteBufCodecs.STRING_UTF8` (not `STRING`), `ByteBufCodecs.INT`, `ByteBufCodecs.BOOL`
- `IPayloadContext.enqueueWork(Runnable)` for deferring to main thread
- `IPayloadContext.player()` for serverbound payloads

## Pendientes Fase 3
- Client classes: KeyListener (RegisterKeyMappingsEvent), Commands (RegisterClientCommandsEvent), ClientConfig (ModConfigSpec), SoundManager, FileSound, URLDiscScreen, ProgressOverlay, VinURLClient
- Mixins: JukeboxMixin, InGameHudMixin
- Item tooltip via ItemTooltipEvent
- Disconnect kill processes

## Log de cambios
- 2026-10-31: Fase 3 parcialmente completada: ClientConfig, SoundManager, FileSound, SkippableAudioStream, ProgressEntry, ProgressOverlay, KeyListener, Commands, VinURLClient, VinURLSound, URLDiscScreen placeholder. Todo compila. Build exitoso.
- 2026-10-31: Recursos copiados del origen (assets, data). pack.mcmeta creado. sounds.json corregido (fabric-sound-api-v1:empty → vinurl:placeholder_sound).
- 2026-10-31: Mixins creados pero no activos (falta annotation processor configuración). Se activarán en fase posterior.

## Pendientes
- URLDiscScreen con widgets vanilla (actualmente no implementado)
- Mixins activos (necesitan configuración de annotation processor)
- Desconexión: kill processes en cliente
- runServer prueba en puerto 25599
- runData / runClient si hay entorno gráfico

## Decisiones pendientes
- owo.ui reemplazo: URLDiscScreen aún por implementar con widgets vanilla
- fabric-sound-api-v1:empty: reemplazado por vinurl:placeholder_sound (silencio)

## Log de cambios
- 2026-10-31: ./gradlew build EXITOSO. Jar: build/libs/vinurl-2.6.0.jar
- 2026-10-31: ./gradlew runServer en puerto 25599 EXITOSO. Servidor arranca, carga mod sin excepciones, se pausa tras 60s sin jugadores (comportamiento normal).
- 2026-10-31: Data pack format actualizado a max_format/min_format 101 para compatibilidad MC 26.1.2.
- 2026-10-31: pack.mcmeta creado (recursos y datos).
- 2026-10-31: sounds.json corregido (fabric-sound-api-v1:empty → vinurl:placeholder_sound).

## Estado de contenido portado
COMPLETO:
- Registro de ítem custom_record (DeferredRegister.Items)
- Registro de sound event placeholder_sound
- Creative tab (tools_and_utilities) via BuildCreativeModeTabContentsEvent
- Red NeoForge (CustomPacketPayload, StreamCodec, NetworkRegistry, PacketDistributor)
- Servidor: ServerEvent.SetURLRecord handler
- Ejecutables: Url, Platform, ProcessStream, Executable
- Config cliente: ModConfigSpec (ClientConfig)
- KeyListener: RegisterKeyMappingsEvent, ClientTickEvent.Post
- Comandos: RegisterClientCommandsEvent
- Tooltips: ItemTooltipEvent
- Progreso: RenderGuiEvent.Post → ProgressOverlay
- Ejecutable de sonido: SoundManager, FileSound, SkippableAudioStream

PENDIENTE:
- URLDiscScreen (GUI con widgets vanilla en lugar de owo-ui)
- Mixins activos (JukeboxMixin, InGameHudMixin) - necesitan annotation processor funcional
- Desconexión del cliente: kill processes
- runClient / runData pruebas

## Jar: build/libs/vinurl-2.6.0.jar
