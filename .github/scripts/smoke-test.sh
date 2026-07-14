#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/smoke
rm -f eula.txt server.properties

set +e
./gradlew --no-daemon "$@" :viafabric-mc189:runServer 2>&1 | tee build/smoke/server.log
gradle_status=${PIPESTATUS[0]}
set -e

# Minecraft's Log4j configuration emits ANSI sequences even with Gradle's plain
# console. Validate a normalized copy so component/message boundaries are stable.
sed -E 's/\x1B\[[0-9;]*[[:alpha:]]//g' build/smoke/server.log > build/smoke/server.clean.log

log=build/smoke/server.clean.log

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
  if ! grep -Fq "${marker}" "${log}"; then
    echo "Startup marker not found: ${marker}" >&2
    exit 1
  fi
done

if grep -Eq "ClassTweakerFormatException|InvalidMixinException|MixinApplyError|InjectionError" "${log}"; then
  echo "A mapping or mixin transformation failure was detected." >&2
  exit 1
fi

known_osl_shutdown="tried to destroy the config manager for the world scope while it was not set up"
if grep -Fq "Exception stopping the server" "${log}" && ! grep -Fq "${known_osl_shutdown}" "${log}"; then
  echo "The server encountered an unexpected shutdown exception." >&2
  exit 1
fi

if [[ ${gradle_status} -ne 0 ]] && ! grep -Fq "${known_osl_shutdown}" "${log}"; then
  echo "The Minecraft startup smoke test failed with Gradle status ${gradle_status}." >&2
  exit "${gradle_status}"
fi

echo "Minecraft 1.8.9 reached the expected EULA boundary with Via and mixins initialized."
if grep -Fq "${known_osl_shutdown}" "${log}"; then
  echo "The known OSL pre-world shutdown exception was recognized and does not fail this smoke test."
else
  echo "The server exited cleanly at the expected pre-EULA boundary."
fi
