#!/usr/bin/env bash
# Nix 環境で SavePosition プラグインをビルドする補助スクリプト。
# Java 25 / Gradle 9 を nix-shell で一時的に用意してビルドする。
set -euo pipefail

cd "$(dirname "$0")"

GRADLE_ARGS=("$@")
if [ ${#GRADLE_ARGS[@]} -eq 0 ]; then
    GRADLE_ARGS=(build)
fi

if [ -x ./gradlew ]; then
    GRADLE_CMD=(./gradlew)
else
    GRADLE_CMD=(gradle)
fi

exec nix-shell -p gradle_9 jdk25 --run \
    "${GRADLE_CMD[*]} ${GRADLE_ARGS[*]} --no-daemon --console=plain"
