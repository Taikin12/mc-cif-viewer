# mc-cif-viewer agent notes

## Build and verification

- Requires Java 25 and Gradle 9.8.0.
- Shared code lives in the `common/` submodule (included with `includeBuild("common")`,
  package `dev.taikin.miviewer.common`) and is shaded into this JAR.
- Run `./gradlew test shadowJar` before a release or server deployment.
- After updating the `common` submodule, build and test `../mc-mol-viewer` as well.
- Build artifact: `build/libs/mc-cif-viewer-<version>.jar`.
- Generate the resource pack with `python3 resourcepack/build_pack.py`.

## Shared wands

Wands are created with `dev.taikin.miviewer.common.WandItems`:

- `Material.STICK` + `miviewer:wand-type` / `miviewer:wand-target` PDC
- item models `miviewer:wand_menu/view/atom` (FRAME belongs to McMolView)
- target `CIF` for this plugin; a model click switches the shared wand to CIF
- `WandItems.refreshAppearance` migrates legacy `cifviewer:wand-type` wands

See `../AGENTS.md` for the full workspace contract and common-first rules.

## CI and releases

- `.github/workflows/build.yml` builds on push/PR; `release.yml` runs on `v*`
  tags and attaches the shaded JAR to a GitHub Release.
- Both workflows check out the public `common` submodule. The
  `MIVIEWER_COMMON_TOKEN` secret is only required if the common repo becomes
  private (workflows fall back to `github.token`).

## Server testing and deployment

- The backend `mc-backend` on host `mc` (`192.168.100.13`) is the plugin test target.
- `mc-test` is the Velocity failover/holding server for when `main` is restarting; it is not the regular plugin test target.
- Use `scripts/deploy-main.sh` to build, copy the JAR and sample CIFs, and **reload the plugin via PlugManX** (`plugman reload CifViewer` over RCON; `scripts/rcon.py`). Use `--no-build` only when the JAR has already been built.
- Use `scripts/deploy-main.sh --restart` only when `server.properties` or the resource pack changed; that path restarts `mc-backend`. A main restart briefly moves connected players to `test` through VelocityReconnect and returns them to `main` when it is ready.
- Batch deployment and GitHub pushes at feature checkpoints rather than for every small change.
- Server-side CIF files and shared models are in `plugins/CifViewer/cif/` and `plugins/CifViewer/models.yml`.

## Resource pack

- `resourcepack/build_pack.py` produces the CIF-only pack `build/cifviewer-resourcepack.zip`.
- The server uses one shared pack for all viewers: `../scripts/deploy-resourcepack.sh`
  builds `viewers.zip` in the parent workspace (CIF + mc-mol-viewer assets), copies it to
  Dynmap, and updates `resource-pack` / `resource-pack-sha1` in `server.properties`.
- It is served at `http://192.168.100.13:8123/viewers.zip`. Use `--restart` when the pack changes.
- Never copy the CIF-only ZIP over the served pack; it removes the molecular assets.
