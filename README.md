# mc-cif-viewer

Minecraft Paper 上で CIF 結晶構造を 3D 表示するプラグインです。  
バニラクライアント + サーバー配布リソースパックで動作し、原子は `ItemDisplay`、結合線・単位胞はカスタムモデルで描画します。

## 特徴

- CIF ファイルの読み込みと対称操作の展開
- 球体 / 空間充填 / ボール＆スティック表示
- vdW / イオン / 共有結合半径の切り替え
- 元素別の色・半径
- 単位胞のワイヤーフレーム
- 原子間の結合線（最近接距離ベースの簡易判定）
- 軸ごとのリピート（超胞）設定
- 回転、原点設定、全体サイズ・原子サイズ調整
- 複数モデルの同時表示（プレイヤーごと、最大 8 個）
- モデル一覧 GUI からの選択・削除
- モデルの永続化（`models.yml`）と再起動後の自動復元
- モデル周辺チャンクの保持（ログアウト後も表示を維持）
- Materials Project からの構造取得
- リソースパック再適用コマンド

## 必要環境

- Java 25
- Paper 26.2+（開発時は Paper API `26.2.build.129-stable` / サーバー 26.3 で確認）
- バニラクライアント + このプラグイン用リソースパック

## ビルド

```bash
./gradlew build
```

生成物:

```text
build/libs/mc-cif-viewer-<version>.jar
```

`-plain.jar` は shadow 前の JAR なので、サーバーには入れないでください。

テスト:

```bash
./gradlew test
```

## インストール

1. JAR を `plugins/` に置いてサーバーを起動します。
2. `plugins/CifViewer/config.yml` が生成されます。
3. CIF ファイルを `plugins/CifViewer/cif/` に置きます。
4. リソースパックを配信し、`server.properties` に設定します。

```properties
resource-pack=http://<host>/cifviewer.zip
resource-pack-sha1=<zip の SHA-1>
require-resource-pack=false
```

リソースパックが未適用の場合、球体・棒モデルは紙表示になります。

## リソースパック

```bash
python3 resourcepack/build_pack.py
```

生成物:

```text
build/cifviewer-resourcepack.zip
```

生成される SHA-1 は再現可能です。配信先の zip と `server.properties` の `resource-pack-sha1` を一致させてください。

## コマンド

| コマンド | 説明 |
| --- | --- |
| `/cif` / `/cif help` | ヘルプ |
| `/cif list [page]` | 利用可能な CIF 一覧 |
| `/cif view <file>` | モデルを追加 |
| `/cif viewat <scale> <repeat> <file>` | 倍率・リピート指定で追加 |
| `/cif fetch <mp-id>` | Materials Project から追加 |
| `/cif fetchat <scale> <repeat> <mp-id>` | 倍率・リピート指定で取得 |
| `/cif cod <id>` | COD から CIF を取得して保存・表示 |
| `/cif codat <scale> <repeat> <id>` | 倍率・リピート指定で COD から取得 |
| `/cif models` | 自分の表示中モデル一覧 |
| `/cif select <id>` | 操作対象モデルを選択 |
| `/cif remove <id>` | 指定モデルを削除 |
| `/cif clear` | 選択中モデルを削除 |
| `/cif clearall` | 全モデルを削除 |
| `/cif wand` | CIF 操作棒を取得 |
| `/cif pack` / `/cif download` | リソースパックを再適用 |
| `/cif reload` | 設定を再読み込み |

エイリアス: `/cifview`

## 権限

| 権限 | 説明 | デフォルト |
| --- | --- | --- |
| `cifviewer.use` | CIF 表示コマンドの利用 | op |

## 操作

`CIF操作棒` を持って操作します。

- 右クリック: メニュー
- モデルをクリック: 操作棒を入手 / メニュー
- 左クリック（空）: 選択軸で 15° 回転（スニークで逆回転）
- 左クリック（ブロック面）: 回転軸を変更
- スニーク + 左クリック（ブロック）: 原点（結晶の 0,0,0）を設定
- スニーク + スクロール: 全体サイズを変更

## GUI

- モデル一覧
- 単位胞 ON/OFF
- ボンド ON/OFF（球体表示時のみ）
- 球体 / 空間充填
- 充填半径: vdW / イオン / 共有結合
- 軸別リピート（X / Y / Z）
- 回転軸
- リソースパック
- リセット / 消去

## 設定

`plugins/CifViewer/config.yml`

| キー | 説明 |
| --- | --- |
| `sources` | CIF を探すディレクトリ |
| `scan-depth` | CIF 探索の深さ |
| `blocks-per-angstrom` | 1 Å あたりのブロック数 |
| `atom-scale` | 原子の基本倍率 |
| `max-atoms` | 1 モデルの最大原子数 |
| `max-repeat` | 軸ごとの最大リピート |
| `max-models` | プレイヤーごとの最大モデル数 |
| `placement-distance` | 初期配置距離 |
| `show-info` | 構造情報の表示 |
| `render-mode` | `sphere` / `circle` / `symbol` |
| `sphere-shadow` | 円表示時の影 |
| `atom-colors` | 元素ごとの色上書き |
| `default-atom-color` | デフォルト色 |
| `materials-project.api-key` | Materials Project API key |
| `cod.base-url` | COD のベース URL |
| `cod.timeout-seconds` | COD 取得のタイムアウト |
| `cod.max-bytes` | COD CIF の最大サイズ |

Materials Project を使う場合、[Materials Project](https://next-gen.materialsproject.org/) で API key を取得し、`materials-project.api-key` に設定して `/cif reload` してください。API key はチャットやリポジトリに書かないでください。

COD は `/cif cod <id>` で取得できます。取得した CIF は `plugins/CifViewer/cif/cod-<id>.cif` に保存され、次回以降はローカルファイルを使用します。COD のデータは CC0 です。

## マルチプレイ

- 表示エンティティはワールド上の共通エンティティです。同じバックエンド・同じワールド・近距離にいるプレイヤーなら誰でも見えます。
- 現在、**操作できるのはモデルの所有者のみ**です。他のプレイヤーは閲覧のみです。
- `モデル一覧` に出るのは自分のモデルだけです。
- 別バックエンド（Velocity の `test` など）にはエンティティは同期されません。
- 外部プレイヤーにカスタムモデルを見せるには、リソースパックを HTTPS で公開する必要があります。

## 既知の制限

- ボリュームデータ（CHGCAR / cube）、isosurface、band/DOS、Fermi 面、ELF、電荷密度などは未対応です。
- 配位多面体、熱振動楕円体、Miller 面、スラブ生成、原子編集は未実装です。
- 結合判定は最近接距離ベースの簡易ヒューリスティックです。周期境界をまたぐ結合は表示範囲内の原子間に限定されます。
- 単位胞は基本セル 1 個分の表示です。
- モデルの所有者・共有編集権限は現在プレイヤー単位です。モデル単位の共有設定は今後追加予定です。

## サンプル CIF

`samples/` に動作確認用の CIF を置いています。TiO2-anatase / TiO2-brookite は
Crystallography Open Database (COD) の CC0 データを使用しており、出典と著者を
`samples/README.md` に記載しています。

## 開発

```text
src/main/java/dev/taikin/mcplugins/cifviewer/
  cif/       CIF パーサ・対称操作
  model/     結晶・格子・元素情報
  render/    Display Entity 描画、セッション、永続化
  gui/       操作用 GUI
  mp/        Materials Project クライアント
resources/
  config.yml
  paper-plugin.yml
samples/     サンプル CIF
resourcepack/ リソースパック生成スクリプト
```

## Repository

https://github.com/Taikin12/mc-cif-viewer
