# Installing Hollow Client

## What you need

| | Version | Needed? |
| --- | --- | --- |
| Minecraft: Java Edition | 26.3 | Yes |
| [Fabric Loader](https://fabricmc.net/use/installer/) | 0.19.5 or newer | Yes |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Any build for 26.3 | Yes |
| Java | 25 | Yes, usually set up by the launcher |
| [Litematica](https://modrinth.com/mod/litematica) and [MaLiLib](https://modrinth.com/mod/malilib) | For 26.3 | Only for Printer and AutoBuild |

## Prism Launcher

1. **Add Instance**, choose **26.3**, then choose **Fabric** as the mod loader.
2. Open the instance with **Edit** and go to **Mods**.
3. **Download mods** and add Fabric API. Add Litematica and MaLiLib if you want to build schematics.
4. Download `hollow-client-26.3-0.1.0.jar` from [Releases](../../../releases).
5. Drag the jar into the **Mods** page, or use **Add file**.
6. **Launch**.

If Prism says the Java version is wrong, open **Settings** then **Java** in the instance, and let it download Java 25.

## Official launcher

1. Run the [Fabric installer](https://fabricmc.net/use/installer/), choose Minecraft **26.3** and press **Install**.
2. Open the game folder:
   - Windows: press <kbd>Win</kbd> + <kbd>R</kbd> and enter `%appdata%\.minecraft`
   - macOS: `~/Library/Application Support/minecraft`
   - Linux: `~/.minecraft`
3. Open the `mods` folder, or create it.
4. Put the Fabric API jar and `hollow-client-26.3-0.1.0.jar` in it.
5. In the launcher, choose the **fabric-loader-26.3** profile and press **Play**.

## Check that it works

The start menu shows HOLLOW CLIENT next to the Minecraft logo. In a world, <kbd>Right Shift</kbd> opens the menu.

## Updating

Remove the old Hollow Client jar, then add the new one. Two versions at once will stop the game from starting. Your settings are kept.

## Removing

Delete the jar from `mods`. To delete your settings as well, remove `config/hollow`.

## Problems

<details><summary>The game says a mod requires Java 25</summary>

Select Java 25 for this instance or profile in your launcher.
</details>

<details><summary>The game says Fabric API is missing</summary>

Add the Fabric API jar for 26.3 to the `mods` folder.
</details>

<details><summary>The start menu is slow</summary>

Open the menu, choose **Theme** and set **Cavern detail** to **Low**, or set **Scene** to **Snowy mountains**.
</details>

<details><summary>Printer says there is no schematic</summary>

Printer needs Litematica and MaLiLib, and a schematic that is placed and enabled in Litematica.
</details>

<details><summary>I want the normal Minecraft look back for something</summary>

In **Theme**, you can switch off the start menu, buttons, tooltips, inventories, hotbar, chat, loading screen and the font one by one.
</details>
