# Chunkzero full stack example

Kotlin gameplay and a TypeScript backend on Chunk, with RPP resource packs and Window UIs.

## Tooling

- Use `mise` for the pinned JDK, Node, pnpm, just, and ktlint.
- `just pack` builds resource-pack assets and generated Kotlin before JVM compilation.
- `just build` builds the pack and Chunk release; `just dev` starts the example.
- Use `pnpm format`, `pnpm lint`, and scoped Gradle tasks with `./gradlew`.
- Generated `.chunk`, `.rpp`, build output, and Window bindings are not committed.
- Stop servers started for verification. Preserve unrelated changes.

## Style and Git

Keep code simple and typed. Use Kotlin DSL and the version catalog for JVM dependencies.
Write focused behavior tests. Use Conventional Commits. Do not edit agent instructions
unless asked.

## Glossary

- App: gameplay JVM under `apps/`, declared by `app.ts`.
- Session: one lobby or arena instance managed by Chunk.
- Pack: assets built by RPP from `pack/src/`.
- View: handwritten implementation of a Window-generated Kotlin class.
