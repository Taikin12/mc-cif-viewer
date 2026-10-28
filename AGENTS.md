# mc-cif-viewer agent notes

## Build and verification

- Requires Java 25 and Gradle 9.8.0.
- Run `./gradlew test shadowJar` before a release or server deployment.
- Build artifact: `build/libs/mc-cif-viewer-<version>.jar`.
- Generate the resource pack with `python3 resourcepack/build_pack.py`.

## Server testing and deployment

- The backend `mc-backend` on host `mc` (`192.168.100.13`) is the plugin test target.
- `mc-test` is the Velocity failover/holding server for when `main` is restarting; it is not the regular plugin test target.
- Use `scripts/deploy-main.sh` to build, copy the JAR and sample CIFs, and restart `mc-backend`. Use `--no-build` only when the JAR has already been built.
- A main restart briefly moves connected players to `test` through VelocityReconnect and returns them to `main` when it is ready.
- Batch deployment and GitHub pushes at feature checkpoints rather than for every small change.
- Server-side CIF files and shared models are in `plugins/CifViewer/cif/` and `plugins/CifViewer/models.yml`.

## Resource pack

- The generated file is `build/cifviewer-resourcepack.zip`.
- On the current server it is served by Dynmap at `http://192.168.100.13:8123/cifviewer.zip`.
- When changing the pack, update the deployed ZIP and `resource-pack-sha1` in the main server's `server.properties` together.
