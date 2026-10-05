set shell := ["bash", "-euo", "pipefail", "-c"]

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
