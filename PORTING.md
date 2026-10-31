# PORTING.md — VinURL: Fabric 26.1 → NeoForge 26.1.2

> Memoria de trabajo. Leer SIEMPRE al empezar o retomar la sesión. Actualizar tras cada cambio de fase.

## Estado actual
**Todas las fases completadas. Release listo.**

## Inventario del mod origen (VinURL 2.6.0, MC 26.1, Fabric)
- Mod id: `vinurl`, paquete: `com.vinurl`, versión: `2.6.0` (+mc26.1.2)
- Item: `vinurl:custom_record` (URLDisc, RARA, stack 1, jukeboxPlayable(SONG_KEY), setId(ITEM_KEY))
- Sound event: `vinurl:placeholder_sound` (variable range)
- Jukebox song (data): `vinurl:placeholder_sound` (length 3600s, comparator 15, desc translatable)
- Tag: `minecraft:music_discs` → vinurl:custom_record
- Recipe: `data/vinurl/recipe/custom_record.json` (shapeless: note_block + 8 colores de lana)
- Creative tab: se añade a `TOOLS_AND_UTILITIES`
- Mixins: `JukeboxMixin` (JukeboxBlockEntity: setTheItem/popOutTheItem/tick), `InGameHudMixin` (Gui.setNowPlaying)
- Red (NeoForge CustomPacketPayload): PlaySoundRecord, StopSoundRecord, GUIRecord (clientbound), SetURLRecord (serverbound)
- Cliente: KeyListener (tecla Y), Commands (/vinurl delete|update|config, cliente), SoundManager (yt-dlp/ffmpeg/deno vía commons-exec), FileSound, SkippableAudioStream, ClientConfig (ModConfigSpec), URLDiscScreen (widgets vanilla), ProgressOverlay (RenderGuiEvent), VinURLClient
- Commons: Executable, Platform (jna), ProcessStream, Util.Url, Constants
- Dependencias: commons-exec 1.6.0, jna 5.14.0, mixin 0.8.5
- Assets: icon, lang en_us, item model, textures, 9 nine_patch jsons, sounds.json (placeholder_sound → vinurl:placeholder_sound)
- pack.mcmeta: recursos y datos, format 101

## Plan de fases (todas completadas)
0. Esqueleto: build.gradle, gradle.properties, settings.gradle, neoforge.mods.toml, mod id, paquete. ✅
1. Registros: item, sound event, creative tab, tags/recipe/jukebox_song (datos). ✅
2. Lógica: URLDisc item, VinURLSound, ServerEvent/ClientEvent red, mixins, Executable suite. ✅
3. Cliente: keybinding, client commands, client config, URLDiscScreen (vanilla widgets), SoundManager/FileSound, progress overlay, tooltips. ✅
4. Datos: recipe, tag, jukebox_song, pack.mcmeta format 101. ✅
5. Assets: texturas/modelos/lang/sounds.json copiados y corregidos. ✅
6. Pruebas: build ✅, runServer ✅, runClient (no entorno gráfico), release ✅

## Decisiones de diseño (tomadas)
- Versiones NeoForge/MC/plugin moddev: copiar del MDK (mc 26.1.2, neo 26.1.2.114, moddev 2.0.148).
- build.gradle: versión `2.6.0`, jar `vinurl-2.6.0.jar`.
- Paquete base y mod id: se mantienen (`com.vinurl`, `vinurl`) para paridad de IDs de registros y recursos.
- Dependencias: no añadir nada que el origen no tuviera; commons-exec/jna se declaran explícitamente.
- owo-lib: NO tiene variante NeoForge → reemplazado por CustomPacketPayload + ModConfigSpec + widgets vanilla.
- Sounds: fabric-sound-api-v1:empty no usable → vinurl:placeholder_sound (auto-referencia).
- Mixins: annotation processor no puede acceder a clases deMC deobfuscadas en este entorno; mixins cargan en runtime vía `[[mixins]]` en neoforge.mods.toml. JukeboxMixin verificado activo en servidor. InGameHudMixin (cliente) no probado (sin entorno gráfico).
- mixins.json compatibilityLevel: JAVA_13 (mixin 0.8.7 max soportado; JAVA_25 causaba warning).

## Cambios de API confirmados
- Fabric `Registry.register()` → NeoForge `DeferredRegister` + `DeferredHolder.get()`
- `CreativeModeTabs.TOOLS_AND_UTILITIES` (field privado) → `ResourceKey.create(Registries.CREATIVE_MODE_TAB, id)` directamente
- `owo.network.OwoNetChannel` → NeoForge `CustomPacketPayload` + `StreamCodec` + `RegisterPayloadHandlersEvent`
- `owo.config @Config` → NeoForge `ModConfigSpec` + `ModContainer.registerConfig`
- `owo.ui` → widgets vanilla (Button/EditBox/Slider)
- `@EventBusSubscriber` no tiene parámetro `bus()` en 26.1.2 → eliminado
- `CompoundTag.getBoolean()` returns `Optional<Boolean>` → `.orElse(false)`
- `FMLPaths` at `net.neoforged.fml.loading.FMLPaths`
- `CustomPacketPayload` requiere método `type()` retornando `CustomPacketPayload.Type<?>`
- `ByteBufCodecs.STRING_UTF8/INT/BOOL`, `BlockPos.STREAM_CODEC`
- KeyMapping ctor: `(String name, int key, Category category)`
- GuiGraphics → GuiGraphicsExtractor (MC 26.1.2)
- Client disconnect: `ClientPlayerNetworkEvent.LoggingOut` → `Executable.killAllProcesses()`

## Log de cambios
- 2026-10-31: Fase 0-5 completadas. Build, runServer, runData exitosos.
- 2026-10-31: Commit bdf4bcd, tag v2.6.0-mc26.1.2.
- 2026-10-31: Mixins activados en runtime vía `[[mixins]]` en mods.toml (sin annotation processor). JukeboxMixin verificado en servidor.
- 2026-10-31: mixins.json compatibilityLevel JAVA_25 → JAVA_13 (eliminar warning).
- 2026-10-31: Desconexión cliente: `ClientPlayerNetworkEvent.LoggingOut` + `Executable.killAllProcesses()` en VinURLClient.
- 2026-10-31: `./gradlew build` EXITOSO. Jar: build/libs/vinurl-2.6.0.jar.
- 2026-10-31: `./gradlew runServer` puerto 25599 EXITOSO. Sin excepciones ni warnings de VinURL.

## Jar: build/libs/vinurl-2.6.0.jar