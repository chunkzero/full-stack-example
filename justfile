set shell := ["bash", "-euo", "pipefail", "-c"]

# rpp type-checks with the project's TypeScript when it has no bundled compiler.
export RPP_TSC := env_var_or_default("RPP_TSC", justfile_directory() / "node_modules/.bin/tsc")

_default:
    @just --list

setup:
    mise install
    pnpm install --frozen-lockfile

pack:
    rpp -C pack codegen
    rpp -C pack check
    rpp -C pack build
    mkdir -p assets
    cp pack/dist/full-stack-example.zip assets/full-stack-example.zip

codegen:
    chunk codegen

build: pack
    chunk build

dev *ARGS: pack
    scripts/dev.sh {{ ARGS }}

format:
    pnpm format
    ktlint --format '**/*.kt' '**/*.kts' '!**/build/**' '!**/generated/**'

check:
    pnpm format:check
    pnpm lint
    chunk codegen
    pnpm typecheck
    rpp -C pack check

# Build rpp, chunk and Window from sibling checkouts; put rpp and chunk on PATH afterwards.
local-deps root=(justfile_directory() / ".."):
    cargo build --release --locked --manifest-path {{ root }}/rpp/Cargo.toml -p rpp-cli
    cd {{ root }}/chunk && just toolchain && ./gradlew publishToMavenLocal :chunk-gradle-plugin:publishToMavenLocal -x javadoc -x dokkaGeneratePublicationHtml
    cd {{ root }}/window && just plugin && ./jvm/gradlew -p jvm :runtime:publishToMavenLocal :diagnostics-protocol:publishToMavenLocal -PwindowVersion=0.1.0-local
    @echo "Binaries: {{ root }}/rpp/target/release/rpp and {{ root }}/chunk/target/debug/chunk"
