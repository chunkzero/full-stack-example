# Chunkzero full stack example

Mini golf on Chunk. Players meet in the Oakmere clubhouse, run `/play` and play nine
holes of Oakmere Links against the course record's ghost. Coins and ball skins they earn,
along with their best rounds, persist in a TypeScript backend.

Every piece of the stack is used:

- **Chunk** runs two Kotlin gameplay apps (`lobby` and `golf`) and the backend's
  queries, mutations, live watches and migrations.
- **World Builder** authored both worlds. They ship as Polar files with a `marks.json`
  sidecar holding tees, cups, hole regions, portals, leaderboards and NPC spots.
- **RPP** builds the resource pack.
- **Window** generates typed Kotlin views for the play, shop, profile and scorecard
  menus and the in-round HUD.

## Run

Use mise 2026.10.0 or newer, then run `just setup`. It installs the pinned tools,
including the Chunk and RPP nightlies through the
[`mise-chunkzero`](https://github.com/chunkzero/mise-chunkzero) plugin, on Linux and
macOS (x64/arm64) and Windows (x64). Chunk's Gradle plugin and libraries, Minestom and
the Window runtime resolve from `https://maven.chunkzero.com/nightlies`, and the Window
RPP plugin resolves through `chunkzero/rpp-registry`.

```sh
just dev
```

Join `localhost:25565` with Minecraft Java Edition 26.2 and accept the resource pack.
`just dev --offline-logins` allows offline development connections.

## Play

- **Clubhouse.** The hotbar holds Play, Pro Shop and Profile.
  - The leaderboards for course record, holes in one and coins update live.
  - Walk into the portal or run `/play` to tee off.
  - The practice green lets you try the putter between rounds.
- **Putting.** Right-click to start charging, then right-click again to shoot at the
  current power. The aim guide follows where you look.
  - Going out of bounds adds a stroke and returns the ball.
  - A hole ends at 8 strokes. After 2 minutes it scores par or your strokes so far,
    whichever is higher, plus 2 (capped at 8).
- **Rounds.** Finishing earns coins and XP, with bonuses for aces and under-par holes, and can
  set a personal best or the course record. The record holder's shots replay as a
  ghost for everyone after them. `/scorecard` shows the card mid-round and `/lobby`
  leaves.

## Layout

- `apps/lobby`: clubhouse session, menus, live leaderboards, portal and practice green.
- `apps/golf`: course sessions, rounds, scoring and result recording.
- `shared/`:
  - `golf/`: deterministic ball physics, the putter and ghost replays.
  - `world/`: Polar and marks loading.
  - `ui/`: handwritten Window views.
- `server/`: backend schema, players, shop, rounds, courses and leaderboards.
- `pack/src/window/`: Window UI definitions.
- `worlds/mark-kinds.json`: the mark kinds World Builder uses for these worlds.

`just check` runs formatting, linting, codegen, TypeScript and pack checks. JVM tests
run per module, such as `./gradlew :shared:test :apps:golf:test :apps:lobby:test`.
`just build` builds the pack and a Chunk release. Generated SDKs, Window bindings,
`.chunk` state and build output are not committed.

## Editing worlds

Open a world in [World Builder](https://github.com/chunkzero/minestom-world-builder)
with `worlds/mark-kinds.json` as its mark kinds. Export it, then replace `world.polar`
and `marks.json` under `apps/<app>/src/main/resources/worlds/<name>/`. If hole pars
change, update `server/courses.ts` to match, since the backend scores rounds against
those pars.

## Nightlies

`mise.lock` pins the Chunk and RPP CLI nightlies. Update them with
`mise upgrade chunkzero:chunk-nightly chunkzero:rpp-nightly`.

- **Chunk:** set the CLI's version on the settings plugin in `settings.gradle.kts`.
- **Window:** pin a nightly with `rpp -C pack add window@<version>`, then set the same
  `window` version in `gradle/libs.versions.toml`. The plugin and JVM runtime must
  match exactly.

Commit `mise.lock`, `pack/rpp.lock` and the version changes together. Published
nightlies are immutable and never deleted.
