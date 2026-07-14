#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/smoke
rm -f eula.txt server.properties

set +e
./gradlew --no-daemon "$@" :viafabric-mc189:runServer 2>&1 | tee build/smoke/server.log
gradle_status=${PIPESTATUS[0]}
set -e

if [[ ${gradle_status} -ne 0 ]]; then
  echo "The Minecraft startup smoke test failed with Gradle status ${gradle_status}." >&2
  exit "${gradle_status}"
fi

required_markers=(
  "Loading Minecraft 1.8.9 with Fabric Loader 0.19.3"
  "Initializing MixinExtras"
  "ViaBackwards) Registering protocols"
  "ViaRewind) Registering protocols"
  "ViaVersion detected server version: 1.8.x (47)"
  "Starting minecraft server version 1.8.9"
  "You need to agree to the EULA"
)

for marker in "${required_markers[@]}"; do
  if ! grep -Fq "${marker}" build/smoke/server.log; then
    echo "Startup marker not found: ${marker}" >&2
    exit 1
  fi
done

if grep -Eq "ClassTweakerFormatException|InvalidMixinException|MixinApplyError|InjectionError" build/smoke/server.log; then
  echo "A mapping or mixin transformation failure was detected." >&2
  exit 1
fi

echo "Minecraft 1.8.9 reached the expected EULA boundary with Via and mixins initialized."
