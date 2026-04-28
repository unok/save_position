#!/usr/bin/env bash
# MockBukkit を ./.mockbukkit-sha にピン留めされた commit でビルドし
# ローカルの maven リポジトリ (~/.m2) に publish する。
#
# 必要なもの: PATH 上に java (25+) と git。
#  Nix 環境なら:
#    nix-shell -p gradle_9 jdk25 git --run ./scripts/install_mockbukkit.sh
#
# MockBukkit 公式が MC 26 対応モジュールを Maven Central 公開したら不要になる。

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SHA_FILE="$REPO_ROOT/.mockbukkit-sha"
CLONE_DIR="${MOCKBUKKIT_CLONE_DIR:-$REPO_ROOT/.mockbukkit-build}"

if [ ! -f "$SHA_FILE" ]; then
    echo "Error: $SHA_FILE が見つかりません。" >&2
    exit 1
fi

SHA=$(tr -d '[:space:]' < "$SHA_FILE")
SHORT_SHA="${SHA:0:7}"

if [ -d "$CLONE_DIR/.git" ]; then
    echo "[update] $CLONE_DIR"
    git -C "$CLONE_DIR" fetch --tags origin
else
    echo "[clone] -> $CLONE_DIR"
    rm -rf "$CLONE_DIR"
    git clone https://github.com/MockBukkit/MockBukkit.git "$CLONE_DIR"
fi

git -C "$CLONE_DIR" checkout --detach "$SHA"

(
    cd "$CLONE_DIR"
    chmod +x ./gradlew
    ./gradlew publishToMavenLocal -x test -x javadoc --no-daemon --console=plain
)

echo
echo "===================================================================="
echo "MockBukkit @ $SHORT_SHA を mavenLocal に publish しました。"
echo "build.gradle.kts は次の座標を参照します:"
echo "  org.mockbukkit.mockbukkit:mockbukkit-v26.1:dev-${SHORT_SHA}"
echo "===================================================================="
