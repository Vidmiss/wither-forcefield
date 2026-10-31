# Wither Forcefield

Client-side Fabric mod that draws the Wither's armor swirl as a forcefield around worn enchanted netherite armor.

## Branches

This branch (`main`) is a version-neutral template: the license, the Gradle build setup and the mod metadata, with no
mod code. Each Minecraft version lives in its own branch:

- `mc-26.3`: Minecraft 26.3
- `mc-26.2`: Minecraft 26.2

## Starting a new version

    git switch main
    git switch -c mc-NEW_VERSION
    # edit gradle.properties (minecraft_version, fabric_api_version, loader_version) and fabric.mod.json
    git checkout mc-26.3 -- src/client README.md
    ./gradlew --no-configuration-cache runClient

Fix any compile errors (Minecraft's rendering code changes between versions), then push the branch.

## License

Vidmiss Non-Commercial License: free for personal and other non-commercial use, with credit to Vidmiss. No selling or
monetizing it or anything derived from it, and derivatives must use the same license. See `LICENSE` for the full text.
