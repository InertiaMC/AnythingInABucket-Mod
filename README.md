# AnythingInABucket

Catch any mob in a bucket and release it anywhere later, exactly as it was.

[Download on Modrinth](https://modrinth.com/mod/anythinginabucket) · Also available as a
[plugin](https://github.com/InertiaMC/AnythingInABucket-Plugin) for Paper, Spigot, Bukkit, Purpur and Folia.

## Features

- Catch any mob with a Capture Bucket and release it by right-clicking a block.
- The whole mob is kept: name, health, variant, owner, saddle, armour, chest, villager trades and more.
- Filled buckets show the mob's details in their lore.
- Copied buckets do nothing, so mobs can't be duplicated.
- Pets can only be caught by their owner, and claim mods are respected.
- Per-mob permissions and dispenser releases.
- Server-side: players don't need the mod. Works in singleplayer too.

## Requirements

- Minecraft 1.21 to 26.3
- Fabric (with Fabric API), NeoForge or Forge
- Pick the jar for your loader and version: `AnythingInABucket-<loader>-1.0.0+<minecraft>.jar`

## Commands

| Command | Permission |
|---|---|
| `/bucket help` | `aiab.command.help` |
| `/bucket give <player> [amount]` | `aiab.command.give` |
| `/bucket admin info` | `aiab.command.admin.info` |
| `/bucket admin reload` | `aiab.command.admin.reload` |

`/aiab` works too.

## Permissions

Works with LuckPerms or any permissions mod. Without one, the defaults below apply.

| Permission | Default |
|---|---|
| `aiab.catch` | everyone |
| `aiab.catch.<mob>` (e.g. `aiab.catch.villager`) | everyone |
| `aiab.release` | everyone |
| `aiab.bypass.owner` (catch other players' pets) | op |
| Commands other than `help` | op |

## Configuration

`config/anythinginabucket/config.yml`, with comments. It uses the same settings as the plugin, so the two configs are
interchangeable.

## Resource pack

Optional. Each filled bucket shows the mob inside it instead of a spawn egg. Get it on
[Modrinth](https://modrinth.com/resourcepack/anythinginabucket-resource-pack). Players with the mod installed already
have the art.

## Building

```bash
./gradlew :26.3-fabric:build
```

Needs JDK 25 (and 21 for 1.21.x). Build one `<minecraft>-<loader>` project at a time. The bucket art comes from the
[plugin repo](https://github.com/InertiaMC/AnythingInABucket-Plugin): clone it and pass its folders, for example
`-PartPack=../AnythingInABucket-Plugin/resourcepack -PartNumbers=../AnythingInABucket-Plugin/src/main/resources/numbers.txt`.
