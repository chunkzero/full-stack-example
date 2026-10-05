# Chunkzero full stack example

A grass lobby and sandstone arenas with persistent player coins, based on Chunk's
Kotlin example. Chunk runs gameplay and the TypeScript backend, RPP builds the
resource pack, and Window generates a typed wallet UI whose earn button calls the
same backend mutation as `/coin`.

## Run

Use mise 2026.10.0 or newer, then run `just setup`. `mise.toml` installs the
pinned tools, including the `chunk` and `rpp` nightlies through the
[`chunkzero` mise plugin](https://github.com/chunkzero/mise-chunkzero). Both
support Linux and macOS on x64/arm64, and Windows on x64.

The Window plugin resolves through `chunkzero/rpp-registry`. Chunk's Gradle
plugins and libraries, Multistom (Chunk's Minestom build) and the Window runtime
resolve from `https://maven.chunkzero.com/nightlies`.

```sh
just dev
```

Join `localhost:25565` with Minecraft Java Edition 26.2 and accept the required
resource pack. `/wallet` opens the Window UI; click `+1` to earn a persistent coin.
`/coin`, `/travel lobby|arena|large`, `/hello <message>`, and `/population` in the
lobby exercise Chunk's backend and session routing. Coins survive session moves
and restarts. `just dev --offline-logins` allows offline development connections.

The pack endpoint runs on `127.0.0.1:8080` and stops with the dev command. Rebuild
UI assets with `just pack` before restarting after an authoring change. Set
`PACK_URL` to a player-accessible URL when hosting the built pack elsewhere.

## Work on it

- `pack/src/window/wallet.ts`: UI definition.
- `pack/rpp.config.ts`: pack settings and Kotlin generation.
- `shared/src/main/kotlin/com/chunkzero/example/ui/`: handwritten view and pack request.
- `server/players.ts`: typed player queries and mutations.
- `apps/`: lobby and arena declarations and session providers.
- `settings.gradle.kts`, `gradle/libs.versions.toml` and `pack/rpp.json`: pinned JVM/plugin versions.

`just build` includes the resource pack under `assets/` in the Chunk release. `just check` checks
formatting, linting and TypeScript types. JVM tests use `./gradlew :shared:test`.
Commit `pack/rpp.lock` whenever updating plugin dependencies; keep the Window
plugin and runtime versions aligned. Generated SDKs, UI bindings and local state
are ignored.

## Nightlies

Chunkzero projects publish immutable versions: stable `X.Y.Z`, prereleases such as
`X.Y.Z-beta.N`, and nightlies such as
`0.1.0-nightly.20261005025602.g47c5d08bda27` (UTC commit time and commit). Stable
JVM artifacts are served from `https://maven.chunkzero.com`, nightlies from
`https://maven.chunkzero.com/nightlies`. This example pins nightlies.

- Chunk and RPP CLIs: GitHub release assets listed in `chunkzero/mise-registry`,
  installed as `chunkzero:chunk-nightly` and `chunkzero:rpp-nightly`.
- Window plugin: GitHub release archive, registered in `chunkzero/rpp-registry`.
- Chunk Gradle plugins and libraries, and the Window runtime: the same versions on
  the nightly Maven repository.

`mise.lock` pins the selected CLI builds. Update them with
`mise upgrade chunkzero:chunk-nightly chunkzero:rpp-nightly` and commit `mise.lock`.
The Chunk CLI, the `com.chunkzero.chunk.settings` plugin version in
`settings.gradle.kts`, and `chunk` in `gradle/libs.versions.toml` must match. Pin
a Window nightly with `rpp -C pack add window@<version>`, then set the same
version for `window` in `gradle/libs.versions.toml`. Commit the version changes,
`mise.lock`, and `pack/rpp.lock` together.
