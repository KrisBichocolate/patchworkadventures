# Patchwork Adventures

The goal of this project is to create tools, datapacks, mods for Java Minecraft 1.21.1 that create an experience similar to the one seen on the Misadventures SMP.

The project is in an early state and many features are still missing.

## Dimensions Mod and Datapack

Adds a resettable resource world as well as infrastructure for instanced dungeons.

See [dimensions/README.md](dimensions/README.md).

## Markers Datapack

The markers datapack provides tools for dealing with marker entities. Marker entities come up a lot when populating dungeons or points of interest and the idea is to simplify their handling.

Video: https://www.youtube.com/watch?v=JDgZCp-Ha_o

After installation, trigger the main menu with
```
/function pwa_markers:menu
```
to create new marker types, get placement tools, go into edit mode or change settings.

### Building

The datapack is written with beet and bolt. Install [beet](https://github.com/mcbeet/beet) and run `beet build` to build it.

## Effects-lib Datapack

Work in progress on making it easier to build marker-based effects. Complex because it has to be a datapack-generator.

## Contact

Mail me at krisbichocolate@proton.me or message krisbichocolate on discord (AvidMC's for instance).
