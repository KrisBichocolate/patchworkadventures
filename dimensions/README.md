# Patchwork Adventures: Dimensions

This consists of a Fabric mod for Minecraft 1.21.1 as well as a companion datapack.

It adds
- a resettable resource world and travel to and from it
- infrastructure for instanced dungeons
- control over nether portal destinations
- a minecraft:noise-derived world generator that supports `structure_overrides`


# The Resource World

## Commands

- `/rw`: Teleports the current player to a random location in the resource world. If called from the overworld, remembers the player's location for `/overworld`. Only usable in the resource world, its nether and the overworld.
- `/overworld`: Teleports the current player to the overworld location they last used `/rw` from.
- `/reset_resource_world`: Admin (level 2) only. Deletes the resource world and its nether and regenerates it. Any players inside effectively use `/overworld`.

## Configuration

Some config can be done via `/data` command on `storage pwa_dimensions:config`.

- `overworld`: name of the overworld dimension. Defaults to "minecraft:overworld".
- `resourceworld`: name of the resource world dimension. Defaults to "pwa_dimensions:rw".
- `resourceworld_nether`: name of the resource world nether dimension. Defaults to "pwa_dimensions:rw_nether".

TODO: config for spreadplayers?

Configuration about nether portals is also in `storage pwa_dimensions:config`:

- `worlds.<world-name>.nether_portal_target_world`: The name of the dimension that nether portals should teleport players to. If the world doesn't exist, nether portals don't light and don't activate. Example: `worlds."pwa_dimensions:rw".nether_portal_target_world = "pwa_dimensions:rw_nether"`.

The resource world generation settings are read from the `pwa_dimensions:resource_world_preset` world_preset. Its `minecraft:overworld` entry is for the resource world and its `minecraft:the_nether` entry is for the resource world nether.

To support disabling structures in the resource world, there is a new generator type `pwa_dimensions:noise`, which is exactly like `minecraft:noise` but has an extra `structure_overrides` setting that works exactly like the flat generator.

## Admin

- Use the multiworld mod `/mw` command to list and manage dimensions.
- WARNING: If you delete a world with `/mw delete`, it'll be permanently gone immediately.
- The normal `/gamerule` command does not work on custom dimensions. Use `/mw gamerule`.
- The player's overworld location (location that `/overworld` will teleport to) is stored with `/playeranchor` under the "overworld" key.

# The Dungeons

## Commands

All commands are admin (level 2) only.


### Templates

Templates are the worlds that dungeon instances are created from.

The `/dungeon template` command has a few subcommands:

- `/dungeon template create <new template name>`

  Creates a new template edit and teleports the current player into it. Use `/dungeon template save_and_finish` when done.

  Currently it always creates a minecraft:flat world.

- `/dungeon template edit <existing template name>`

  Clones the given template and teleports the current player inside. Use `/dungeon template save_and_finish` when done to store the edited template under a new name.

- `/dungeon template save_and_finish <new template name>`

  Finishes the current template edit by copying it to a new dimension and deleting the edit dimension.

- `/dungeon template delete <existing template name>`

  IMMEDIATELY and PERMANENTLY DELETES the template!

### Instances

Instances are created from templates on demand. Players go there and when
everyone has left, they are deleted again.

- `/dungeon instance create <template name>`

  Create a new instance as a copy of the template and run the instantiation_function.

  Stores return values in `storage pwa_dimensions:return_value`:
  - `new_world`: full name to new dimension
  - `new_instance`: name of the new instance
  - `template`: name of the template used to instantiate
  - `template_config`: copy of the template's configuration

- `/dungeon instance delete <instance name>`

  IMMEDIATELY and PERMANENTLY DELETES the instance!

- `/dungeon instance keepalive add <players>`

  Adds the players to the players to the keepalive list for the current instance.
  Instances where all players have been removed from the keepalive list are deleted.

- `/dungeon instance keepalive remove <players>`

  Removes the players from the current instance's keepalive list. If the list is empty
  permanently delete the instance.

### Player anchors

This is a helper for storing full locations (dimension, position, yaw) on a player entity. It's needed to store the location that a player should be teleported to when exiting an instance. (it's also used to store the `/overworld` target location)

(One'd love to store the location by spawning a marker, but there's no way to spawn a marker that can't be unloaded.)

- `/playeranchor list <players>`

  Show all anchor names for the players.

- `/playeranchor set <players> <anchor name> <entity>`

  Set the player's anchor with the given name to the location of the entity.

- `/playeranchor unset <players> <anchor name>`

  Unset an anchor for the players.

- `/playeranchor tp <players> <anchor name>`

  Teleport the players to the named anchor.

## Configuration

Templates are configured in in `storage pwa_dimensions:templates`.

- `<template name>.instantiation_function`: Name of a function to call inside the instance when this template gets instantiated.

  Example: "pwa_dimensions:instance/set_redstone_blocks", which is a datapack-provided function that places a redstone block at each marker tagged `pwa_dimensions_instance_redstone`. Make use of `/forceload` in the template to ensure the chunks you need are loaded!

Instances store their keepalive data in `storage pwa_dimensions:instances`. Shouldn't need to worry about it.


## Admin

Set the spawn location in a template with `/mw setspawn`.

Use `/mw list` to see all dimensions and whether they're loaded. If something goes wrong, you may need to delete leftover dimensions. Prefer using the `/instance` command for deletions (because it also updates the config data) - and be extra careful.

Template dimensions do not need to be loaded. Feel free to `/mw unload` them.


## Dungeon entrances and exits

The pwa_dimensions datapack supports basic dungeon entrances and exits. Here's how they work:

Only players with the `pwa_dimensions_player` tag can use entrances and exits.

- The marker entity tagged `pwa_dimensions_dungeon_entrance` marks dungeon entrances.

  Set the marker's `data.dungeon_template` to the name of the template of the dungeon it shall instantiate for players. To do that, you can use `/data` commands, axiom or `/function pwa_dimensions:dungeon_entrance/set_closest_entrance_template {template:"mytemplate"}`.

  There's no explicit grouping commands for players. When the entrance countdown completes, all players within range go into the same dungeon instance.

- The marker entity tagged `pwa_dimensions_dungeon_exit_target` marks where players that leave the dungeon (through death or the exit) shall be teleported. The closest exit_target to the entrance will be used.

- The marker entity tagged `pwa_dimensions_dungeon_exit` inside a dungeon template marks the dungeon's exit. Players that get close will be teleported to the exit_target.

## World generator with structure override config

There's a `pwa_dimensions:noise` generator that is exactly like `minecraft:noise`, but also supports the `structure_overrides` argument like `minecraft:flat` does. Example:

```
       "generator": {
         "type": "pwa_dimensions:noise",
         "structure_overrides": [],
         "settings": "minecraft:overworld",
         "biome_source": {
```

# Installation

This is a Fabric mod for Minecraft v1.21.1. It does not work with other versions.

You need:
- Minecraft v1.21.1 with Fabric installed
- Fabric API mod
- My custom version of the multiworld mod.
- The pwa_dimensions mod.
- The pwa_dimensions datapack.

Recommended:
- The pwa_markers datapack for placing marker entities.

# Technical

This mod depends on
- fabric, fabric api
- fantasy library fixes
  - backport of v0.6.7 to minecraft 1.21.1, because it has world deletion/unloading fixes
  - as yet unmerged bugfix for force-loaded chunks
- multiworld library fixes/rework
  - reworked /mw create command for more control
  - adding clone/load/unload/delete commands
  - reworked dimension config storage
