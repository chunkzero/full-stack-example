# Chunkzero full stack example

A grass lobby and sandstone arenas with persistent player coins, based on Chunk's
Kotlin example. Chunk runs gameplay and the TypeScript backend, RPP builds the
resource pack, and Window generates a typed wallet UI whose earn button calls the
same backend mutation as `/coin`.

## Run

Use mise 2026.10.0 or newer, then run `just setup`. `mise.toml` installs the
pinned tools and reads RPP's release definition from `chunkzero/aqua-registry`.
RPP supports Linux and macOS on x64/arm64, and Windows on x64. Mise selects the
native archive automatically. Have `chunk` on `PATH`.

The Window plugin resolves through `chunkzero/rpp-registry`; its matching JVM
runtime resolves from `https://maven.chunkzero.com`.

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
- `shared/src/main/kotlin/dev/chunkzero/example/ui/`: handwritten view and pack request.
- `server/players.ts`: typed player queries and mutations.
- `apps/`: lobby and arena declarations and session providers.
- `gradle/libs.versions.toml` and `pack/rpp.json`: pinned JVM/plugin versions.

`just build` includes the resource pack under `assets/` in the Chunk release. `just check` checks
formatting, linting and TypeScript types. JVM tests use `./gradlew :shared:test`.
Commit `pack/rpp.lock` whenever updating plugin dependencies; keep the Window
plugin and runtime versions aligned. Generated SDKs, UI bindings and local state
are ignored.

## Nightlies

Daily workflows publish immutable prereleases such as
`0.1.0-nightly.20261002.g0123456789ab`, with checksums and the full source commit.
Unchanged sources skip the scheduled release; manual dispatch can publish earlier.

- RPP CLI: GitHub release assets, installed by mise through `chunkzero/aqua-registry`.
- Window plugin: GitHub release archive, registered through a PR in `chunkzero/rpp-registry`.
- Window JVM libraries: the same version published to `https://maven.chunkzero.com`.

RPP follows the latest nightly through `aqua:chunkzero/rpp-nightly`; `mise.lock`
pins the selected build and checksum. Update it with
`mise lock --bump aqua:chunkzero/rpp-nightly`, then run
`mise install` and commit `mise.lock`. The per-tool minimum release age allows
fresh nightlies without requiring a global mise setting. Pin a Window
nightly with `rpp -C pack add window@<version>` after its registry PR merges, then
set the same Window version in `gradle/libs.versions.toml`. Commit the version
changes, `mise.lock`, and `pack/rpp.lock` together. RPP and Window can use different
nightly dates; the Window plugin and JVM runtime must match exactly.

Only the latest 30 nightlies are retained. Older pins cannot be freshly installed
after their releases are pruned.
