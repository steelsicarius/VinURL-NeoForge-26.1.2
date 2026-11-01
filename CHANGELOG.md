# Changelog (NeoForge port)

All notable changes to the NeoForge 26.1.2 port. The original Fabric mod's history is in the upstream CHANGELOG.md.

## Version: 2.6.1
- port: NeoForge 26.1.2 port of VinURL 2.6.0 (Fabric 26.1), full content parity
- bugfix: mod icon now renders in the disc screen (26.1.2 resolves `blit()` identifiers as direct resource paths; id must include `textures/` and the `.png` extension) and in the mods list (`logoFile` resolves from the jar root)
- bugfix: download and simulate no longer hang forever — `commons-exec` is now shaded into the mod jar (it was missing at runtime; `RuntimeException: ClassNotFoundException: org.apache.commons.exec.CommandLine`)
- bugfix: config options are now persisted across restarts — every `ConfigValue.set()` is followed by `save()` (NeoForge 26.1.2 semantics), and `urlWhitelist` uses `defineListAllowEmpty` so the TOML value passes validation instead of being "corrected" back to defaults on startup
- bugfix: added a silent placeholder OGG so the placeholder sound event is playable — without it, `SoundEngine.play()` aborted on the empty sound event before the custom audio stream was ever attached
- bugfix: `InGameHudMixin` moved to the `client` mixin section; dedicated servers no longer log a client-class load error
