# Building

You need a JDK 25. Everything else is downloaded.

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The jar is written to `build/libs`.

## Tests

| Command | What it does |
| --- | --- |
| `./gradlew test` | Unit tests |
| `./gradlew runClientGameTest` | Starts the game, plays it and checks what happens. Needs a graphics card |

The client tests are in `src/gametest`. `src/gametest/resources/fabric.mod.json` chooses which ones run:

| Test | What it checks |
| --- | --- |
| `ModuleTest` | Every module switches on and off, and the important ones do their job in a world |
| `InputTest` | The menus, with real clicks and keys |
| `LookTest` | Photographs every screen |
| `PackTest` | Printer and AutoBuild with Litematica. Run with `-Ppack=<folder of mods>` |
| `DocsTest` | Takes the pictures and writes the module list for these pages |

Screenshots go to `build/run/clientGameTest/screenshots`.

## Where things are

| Package | Contents |
| --- | --- |
| `core` | Modules, settings, saving, commands, friends |
| `module`, `build` | The modules |
| `action` | Aiming, placing and inventory actions, one at a time by priority |
| `render` | The ice cavern and the glass blur |
| `ui` | Drawing, menus, fonts, animation |
| `hud` | HUD elements |
| `mixin` | Hooks into the game |
