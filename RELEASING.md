# Release process

1. Confirm CI passes on `main`, including the Minecraft startup smoke test, and perform a real 1.8.9 client launch if
   client-only mixins changed.
2. Move the relevant entries from `Unreleased` into a `## [X.Y.Z] - YYYY-MM-DD` section in `CHANGELOG.md` and set
   `release_version=X.Y.Z` in `gradle.properties`.
3. Create and push an annotated tag with no `v` prefix:

   ```bash
   git tag -a X.Y.Z -m "ViaFabricLegacy X.Y.Z"
   git push origin X.Y.Z
   ```

4. The `Release` workflow verifies that the tag and Gradle version match, builds with Java 25, validates the remapped
   jar, publishes a GitHub release from that changelog section, and attaches both jars plus `SHA256SUMS`.
5. Confirm JitPack resolves
   `com.github.Adi0nt.ViaFabricLegacy:viafabric-mc189:X.Y.Z` before announcing the release.

Never move or replace a published release tag. Increment `release_version` after a release so development builds have
a distinct base version and embedded commit hash.
