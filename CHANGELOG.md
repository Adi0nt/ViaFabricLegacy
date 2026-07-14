# Changelog

All notable changes to this fork are documented here. Release headings follow the Git tag used by GitHub and JitPack.

## [Unreleased]

### Fixed

- Normalized ANSI-colored Minecraft logs before smoke-test assertions and explicitly recognized the known pre-EULA OSL
  shutdown exception, preventing successful initialization from being reported as a CI failure.

## [0.4.23] - 2026-07-13

### Changed

- Updated the bundled ViaVersion and ViaBackwards libraries to 5.11.0 and ViaRewind to 4.1.3.
- Standardized local, CI, and JitPack builds on Java 25 LTS.
- Pinned Fabric Loom 1.17.14 and Ploceus 1.17.4 for Ornithe Gen 2 reproducibility.
- Made the Gradle artifact version the single source for Fabric mod metadata; development builds now include the Git commit.
- Added tagged GitHub releases, checksummed assets, stable JitPack coordinates, and pull-request verification.

### Fixed

- Removed duplicate unremapped OSL dependencies from the `viafabric-mc189` runtime classpath. This prevents the
  `intermediary`/`named` class-tweaker namespace failure during startup without changing the bundled production mod.

[Unreleased]: https://github.com/Adi0nt/ViaFabricLegacy/compare/0.4.23...main
[0.4.23]: https://github.com/Adi0nt/ViaFabricLegacy/releases/tag/0.4.23
