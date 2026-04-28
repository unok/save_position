# SavePosition

Paper サーバ向けプラグイン。プレイヤーが座標を名前付きで保存・表示・削除し、必要なら他のプレイヤーと共有できる。

- 対象: **Paper 1.26.1.2 系** (Java 25 以上)
- 保存先: `plugins/SavePosition/positions.yml`

## インストール

### 1. JAR を入手する

#### A. GitHub Actions の Artifact からダウンロード（推奨）

1. このリポジトリの **Actions** タブを開く
2. 緑チェックの最新の `Build` ワークフロー実行を開く
3. 下の **Artifacts** セクションから `SavePosition-jar` をクリックしてダウンロード（zip）
4. zip を展開すると `SavePosition-x.y.z.jar` が出てくる

> Artifact の保持期間はデフォルト 30 日。古い場合はワークフローを再実行（`workflow_dispatch`）してください。

#### B. 自前でビルドする

[ビルド](#ビルド) を参照。

### 2. Paper サーバへ配置

1. 取得した `SavePosition-*.jar` を Paper サーバの `plugins/` ディレクトリに置く
2. サーバを **再起動** する（`/reload` は非推奨）
3. 起動ログに `[SavePosition] SavePosition を有効化しました。` が出れば成功

## コマンド

| コマンド | 動作 |
|---|---|
| `/pos save <名前>` | 現在地を `名前` で保存（既存名は上書き、共有フラグは維持） |
| `/pos list` | 自分の保存一覧 |
| `/pos show <名前>` | 指定した保存地点の座標を表示 |
| `/pos delete <名前>` | 削除（`remove` / `del` も可） |
| `/pos share <名前>` | 共有 ON |
| `/pos unshare <名前>` | 共有 OFF |
| `/pos shared` | サーバ全員の共有座標一覧 |
| `/pos shared <プレイヤー名>` | 指定プレイヤーの共有座標一覧 |

別名: `/position`, `/savepos`  
権限: `saveposition.use`（default: `true`）

## ビルド

### Nix 環境（このリポジトリのデフォルト想定）

```bash
./build_on_nix.sh             # build (デフォルト)
./build_on_nix.sh test        # テストのみ
./build_on_nix.sh clean build # クリーンビルド
```

`nix-shell -p gradle_9 jdk25` で一時的に Java 25 + Gradle 9 を用意してビルドする。

### 通常環境（Java 25 と git が PATH にある場合）

初回のみ MockBukkit をローカル準備する必要がある。

```bash
./scripts/install_mockbukkit.sh   # 数分。~/.m2 へ MockBukkit を publish
./gradlew build
```

### MockBukkit について

MockBukkit 公式は MC 26 対応モジュールを Maven Central に公開していないため、`dev/26.1.1` ブランチをローカルでビルドして `mavenLocal` に publish する運用。

- 利用する commit SHA は **`.mockbukkit-sha`** に書かれている（フル SHA、改行可）
- `build.gradle.kts` は同ファイルから先頭 12 文字を読んで `mockbukkit-v26.1:dev-XXXXXXXXXXXX` という座標を組み立てる
- `scripts/install_mockbukkit.sh` は MockBukkit の clone 内で `git config core.abbrev 12` を設定して publish するので、shallow / full clone の差で artifact 名がブレない
- 上流の更新を取り込みたいときは `.mockbukkit-sha` を編集してから `install_mockbukkit.sh` を再実行

## CI（GitHub Actions）

`.github/workflows/build.yml` で push / PR / `workflow_dispatch` 時に以下を実行:

1. JDK 25 (Temurin) を `actions/setup-java` で導入
2. `~/.gradle/caches`、`~/.gradle/wrapper` をキャッシュ（キー: gradle ファイル + `.mockbukkit-sha`）
3. `~/.m2/repository/org/mockbukkit` をキャッシュ（キー: `.mockbukkit-sha`）
4. キャッシュミス時のみ `scripts/install_mockbukkit.sh` を実行
5. `./gradlew build` を実行
6. `build/libs/*.jar` を **`SavePosition-jar`** という Artifact として upload（30 日保持）
7. テストレポート（`build/reports/tests/test`）を **`test-report`** として upload（7 日保持）

`.mockbukkit-sha` を更新しない限り、MockBukkit のビルドはキャッシュから即取得される。

## ライセンス

未指定。必要に応じて追加してください。
