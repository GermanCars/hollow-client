# Modules

158 modules and 36 HUD elements, with every setting. Defaults are shown first, or in bold where there is a choice.

[Combat](#combat) · [Player](#player) · [Movement](#movement) · [Render](#render) · [World](#world) · [Misc](#misc) · [HUD elements](#hud-elements) · [Client settings](#client-settings)

This page is written by `DocsTest`, so it always matches the client.

## Combat

### AimAssist

Eases your aim towards the nearest enemy.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 5 (2 to 10) |
| Speed | 4 (1 to 20) |
| Field of view | 90 (10 to 360) |
| Mobs | off |

</details>

### AnchorAura

Places, charges and sets off respawn anchors near enemies.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Target range | 8 (4 to 12) |
| Minimum damage | 7 (0 to 20) |
| Self damage cap | 6 (0 to 20) |
| Delay | 2 (0 to 10) |
| Mobs | off |

</details>

### ArmorAlert

Warns you when armor is about to break.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Below durability % | 15 (1 to 50) |

</details>

### AutoCity

Mines a block of the nearest enemy's surround so crystals reach them.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 5 (3 to 6) |
| Switch off when done | on |

</details>

### AutoLog

Leaves the server before you die.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Below health | 6 (0 to 36) |
| Below totems | 0 (0 to 10) |
| When a stranger appears | off |
| Switch off after | on |

</details>

### AutoTotem

Keeps a totem in your offhand.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Mode | **Always**, Smart |
| Smart health | 14 (1 to 36) |

</details>

### AutoTrap

Boxes the nearest enemy in with obsidian.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 5 (2 to 7) |
| Blocks per tick | 2 (1 to 6) |

</details>

### AutoWeapon

Switches to your best weapon when you attack.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Prefer | **Most damage**, Sword, Axe |
| Axe against shields | on |

</details>

### AutoWeb

Puts cobwebs on enemies.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 4.5 (2 to 6) |

</details>

### AutoXP

Throws experience bottles at your feet to mend worn gear.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Below durability % | 80 (10 to 99) |
| Stop with enemies within | 0 (0 to 16) |
| Delay | 1 (0 to 10) |

</details>

### BowAim

Aims a drawn bow at the enemy nearest your crosshair.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 60 (10 to 120) |
| Mobs | off |

</details>

### BowRelease

Lets arrows go as soon as the bow is drawn far enough.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Charge ticks | 20 (3 to 20) |

</details>

### Criticals

Makes every hit a critical hit.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Mode | **Packet**, Small jump |

</details>

### CrystalAura

Places and breaks end crystals where they hurt enemies most and you least.

<details><summary>9 settings</summary>

| Setting | Default and range |
| --- | --- |
| Place range | 4.5 (1 to 6) |
| Break range | 3 (1 to 6) |
| Target range | 10 (4 to 16) |
| Minimum damage | 6 (0 to 20) |
| Self damage cap | 6 (0 to 20) |
| Place delay | 1 (0 to 10) |
| Break delay | 1 (0 to 10) |
| Auto switch | on |
| Mobs | off |

</details>

### Hitboxes

Makes players easier to hit.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Extra size | 0.3 (0.05 to 1.5) |

</details>

### HoleFill

Fills safe holes near enemies before they reach them.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Enemy distance | 3 (1 to 6) |
| Blocks per tick | 1 (1 to 4) |

</details>

### KeepDistance

Stops you walking any closer to an enemy than your reach needs.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Distance | 2.8 (1 to 6) |

</details>

### KillAura

Attacks whoever is in reach as soon as your weapon is ready.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 3 (1 to 6) |
| Players | on |
| Mobs | off |
| Through walls | off |
| Only with weapon | off |

</details>

### PopCounter

Counts the totems players use and tells you when they die.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Tell me in | **Pop-ups**, Chat |

</details>

### SelfTrap

Closes obsidian around and above you.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Blocks per tick | 2 (1 to 6) |
| Shape | **Full**, Roof only |

</details>

### Surround

Walls your feet in with obsidian.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Blocks per tick | 2 (1 to 8) |
| Centre | off |
| Off when jumping | on |

</details>

### TriggerBot

Attacks whatever your crosshair is on when the weapon is ready.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Players | on |
| Mobs | off |

</details>

### Velocity

Reduces knockback from hits and explosions.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Horizontal % | 0 (0 to 100) |
| Vertical % | 0 (0 to 100) |

</details>

## Player

### AntiAFK

Moves a little so you aren't kicked for idling.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Jump | on |
| Turn | on |
| Swing | off |
| Every seconds | 10 (2 to 120) |

</details>

### AntiHunger

Hides sprinting from the server so you get hungry more slowly.

### AutoArmor

Wears the best armor you are carrying.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Delay | 3 (1 to 20) |
| Keep elytra | on |

</details>

### AutoClicker

Clicks for you.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Button | **Left**, Right |
| Clicks per second | 10 (1 to 20) |
| Only while held | on |

</details>

### AutoDrop

Throws away items you don't want.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Items | list of items, 3 picked |

</details>

### AutoEat

Eats when you are hungry or hurt.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Hunger | 14 (1 to 19) |
| Golden apple health | 10 (0 to 20) |

</details>

### AutoFish

Reels in and casts again when something bites.

### AutoMount

Gets on the nearest mount.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Mounts | list of entity types, 7 picked |

</details>

### AutoPot

Splashes healing potions on yourself when hurt.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Below health | 10 (1 to 19) |

</details>

### AutoRespawn

Respawns without the death screen.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Delay ticks | 10 (0 to 60) |

</details>

### AutoSleep

Uses a bed in reach at night.

### AutoTool

Holds the fastest tool for the block you are mining.

### ChestSwap

Swaps between elytra and chestplate, then switches off.

### FakePlayer

Stands a copy of you in the world to practise on. Only you see it.

### FastBreak

Removes the pause between breaking blocks.

### FastUse

Shortens the wait between right clicks.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Delay | 0 (0 to 4) |
| For | **Blocks and bottles**, Experience bottles, Everything |

</details>

### FullAlert

Tells you when your inventory is full.

### MiddleClick

Middle click adds friends or throws a pearl.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| In the air | **Ender pearl**, Firework, Experience bottle, Nothing |
| Add friends | on |

</details>

### Multitask

Lets you mine while eating or using an item.

### NoInteract

Stops you opening or rotating things by accident.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Containers | on |
| Item frames | on |
| Villagers | off |

</details>

### NoRotate

Keeps your view when the server tries to turn you.

### Offhand

Keeps crystals or golden apples in the offhand when a totem is not needed.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Item | **Crystal**, Golden apple |

</details>

### Reach

Lets you reach further. Most servers allow only a little.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Blocks | 5 (3 to 8) |
| Entities | 3 (3 to 8) |

</details>

### Replenish

Tops up hotbar stacks from your inventory.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Below | 8 (1 to 32) |

</details>

### XCarry

Keeps items in your crafting grid when you close the inventory.

## Movement

### AirJump

Jump again in mid-air.

### AntiLevitation

Ignores levitation.

### AntiVoid

Bounces you back up before the void takes you.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Below the world | 4 (0 to 40) |

</details>

### AutoElytra

Opens your elytra when you fall.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| After falling | 3 (1 to 20) |

</details>

### AutoJump

Jumps whenever you land.

### AutoWalk

Keeps walking forward.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Lock to 45 degrees | off |
| Stop at unloaded chunks | on |

</details>

### Blink

Holds your movement back, then sends it all at once. Short hops only.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Most seconds | 10 (1 to 30) |

</details>

### BoatFly

Flies the boat you are in.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 1.2 (0.1 to 4) |
| Climb speed | 0.4 (0.1 to 1.5) |

</details>

### ClickTP

Sneak and right click a block to jump to it. Short hops only on most servers.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 8 (2 to 60) |

</details>

### ElytraBoost

Uses fireworks to keep your elytra fast.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Every ticks | 30 (10 to 100) |
| Below speed | 1.2 (0.2 to 3) |

</details>

### ElytraFly

Steers an open elytra with the movement keys. Jump climbs, sneak drops.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 1.6 (0.1 to 3) |
| Climb speed | 0.8 (0.1 to 2) |

</details>

### EntitySpeed

Rides horses, pigs and the rest faster.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 1 (0.1 to 4) |

</details>

### FastClimb

Climbs ladders and vines faster.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Up speed | 0.3 (0.1 to 1) |
| Down speed | 0.4 (0.1 to 1) |

</details>

### FastSwim

Moves faster in water and lava.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 0.35 (0.1 to 1.5) |

</details>

### Flight

Lets you fly. Strict servers will pull you back.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Mode | **Velocity**, Creative |
| Speed | 1 (0.1 to 5) |
| Avoid fly kick | on |

</details>

### HighJump

Jumps higher.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Multiplier | 1.5 (1 to 5) |

</details>

### HoleSnap

Pulls you into the nearest safe hole, then switches off.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 4 (1 to 8) |
| Speed | 0.28 (0.1 to 0.6) |

</details>

### IceSpeed

Runs faster on ice.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 0.5 (0.2 to 1.2) |

</details>

### InvMove

Walk about with a menu open.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Sneak too | off |

</details>

### Jesus

Walk on water and lava.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Lava | on |

</details>

### LongJump

Pushes you forward when you jump.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Boost | 0.6 (0.1 to 2) |

</details>

### NoFall

Prevents fall damage.

### NoJumpDelay

Jump again the moment you land.

### NoPush

Players, blocks and water can't push you.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Entities | on |
| Blocks | on |
| Water | on |

</details>

### NoSlow

Keeps full speed while eating, drinking or blocking.

### NoWeb

Cobwebs, berry bushes and the like don't slow you.

### Parkour

Jumps at the very edge of blocks.

### ReverseStep

Drops straight down when you walk off a block.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Up to height | 3 (1 to 10) |
| Speed | 1 (0.2 to 5) |

</details>

### SafeWalk

Stops you walking off edges.

### Sneak

Keeps sneaking.

### Speed

Moves you faster.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Mode | **Strafe**, Bunny hop, Ground |
| Speed | 1.2 (0.5 to 3) |

</details>

### Spider

Climbs any wall you walk into.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 0.2 (0.05 to 1) |

</details>

### Sprint

Always sprints.

### Step

Walks straight up full blocks.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Height | 1 (1 to 2.5) |

</details>

### YawLock

Keeps you facing straight, for highways.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Snap to | **45 degrees**, 90 degrees |

</details>

## Render

### Ambience

Changes the time of day for you only.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Set the time | on |
| Time of day | 18000 (0 to 24000) |

</details>

### BlockESP

Finds the blocks you pick and shows them through walls.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Blocks | list of blocks, 5 picked |
| Chunks | 5 (1 to 12) |
| Most shown | 400 (50 to 2000) |
| Colour | theme colour |
| Use each block's map colour | on |
| Tracers | off |

</details>

### BlockOutline

Outlines the block you point at, gliding from one to the next.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Colour | theme colour |
| Line width | 2 (1 to 6) |
| Fill | 0.15 (0 to 0.8) |
| Glide between blocks | on |

</details>

### BreakESP

Shows the blocks others are mining.

### CameraTweaks

Sets how far the third person camera sits and lets it pass walls.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Third person distance | 4 (1 to 20) |
| Through walls | on |

</details>

### ChunkBorders

Outlines the chunk you are in.

### CityESP

Shows the surround blocks of enemies that can be mined.

### Crosshair

Your own crosshair.

<details><summary>7 settings</summary>

| Setting | Default and range |
| --- | --- |
| Shape | **Cross**, Diamond, Dot, Shard |
| Size | 5 (1 to 14) |
| Gap | 2 (0 to 10) |
| Thickness | 1.4 (0.5 to 5) |
| Colour | theme colour |
| Dark outline | on |
| Open up after attacking | on |

</details>

### DamageNumbers

Damage and healing float up from whoever it happened to.

### ESP

Draws boxes around players and more, through walls.

<details><summary>8 settings</summary>

| Setting | Default and range |
| --- | --- |
| Style | **Box**, Corners, Frame |
| Players | on |
| Monsters | off |
| Animals | off |
| Crystals | on |
| Health bar | on |
| Line width | 2 (0.5 to 5) |
| Fill | 0.15 (0 to 0.6) |

</details>

### Freecam

Flies the camera away while you stand still.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 1 (0.1 to 5) |

</details>

### Freelook

Look around without turning.

### Fullbright

Lights up the dark.

### Glow

Outlines players and more through walls.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Players | on |
| Mobs | off |
| Items | off |

</details>

### HUD

Shows the HUD. Arrange it on the HUD page.

### HandView

Moves and resizes the items in your hands.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Sideways | 0 (-1 to 1) |
| Up | 0 (-1 to 1) |
| Forward | 0 (-1 to 1) |
| Scale | 1 (0.3 to 2) |
| Tilt | 0 (-90 to 90) |
| Turn | 0 (-90 to 90) |

</details>

### HitMarker

Marks your hits at the crosshair.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Sound | on |

</details>

### HoleESP

Marks safe holes near you.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 8 (4 to 16) |

</details>

### ItemESP

Shows dropped items and what they are.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Names | on |
| Boxes | on |

</details>

### JumpRings

A ring spreads from under you when you jump.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1.1 (0.4 to 3) |

</details>

### LightOverlay

Marks floors dark enough for monsters.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 12 (4 to 24) |

</details>

### LogoutSpots

Marks where players were when they left the server.

### LookLines

Shows where other players are looking.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Length | 4 (1 to 30) |

</details>

### Nametags

Bigger name tags with health, gear and ping.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 2) |
| Gear | on |
| Ping | on |
| Distance | on |
| Totem pops | on |
| Myself | off |

</details>

### NoRender

Hides things that get in the way.

<details><summary>13 settings</summary>

| Setting | Default and range |
| --- | --- |
| Hurt tilt | on |
| Fire on screen | on |
| Water on screen | on |
| Suffocation | on |
| Totem animation | off |
| Pumpkin and frost | on |
| Nausea and portal | on |
| Dark edges | off |
| Fog | off |
| Explosion particles | off |
| Weather | off |
| Boss bars | off |
| Scoreboard | off |

</details>

### PlayerArrows

Arrows point to players who are off screen.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Radius | 46 (20 to 120) |
| Size | 6 (3 to 14) |

</details>

### PopEffects

Rings burst out when someone uses a totem.

### Snowfall

Gentle snow around you, wherever you are.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Amount | 4 (1 to 20) |

</details>

### StorageESP

Shows chests, shulkers and other storage through walls.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Chests | on |
| Ender chests | on |
| Shulker boxes | on |
| Barrels, hoppers and furnaces | on |
| Spawners | on |
| Tracers | off |

</details>

### TargetRing

A ring travels up and down the nearest enemy.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 8 (3 to 30) |
| Mobs | off |

</details>

### Tracers

Draws lines to players and more.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Players | on |
| Monsters | off |
| Items | off |
| From | **Crosshair**, Bottom |
| Width | 1 (0.5 to 4) |
| Opacity | 0.75 (0.1 to 1) |

</details>

### Trail

Leaves a line where you have been.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Points | 400 (20 to 2000) |

</details>

### Trajectories

Shows where arrows, pearls and potions will land.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Colour | theme colour |

</details>

### VoidESP

Marks gaps in the bedrock floor.

### Waypoints

Shows saved places. Type .waypoint add and a name to save one.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 2) |
| Hide within | 6 (0 to 50) |
| Show across Nether and Overworld | on |

</details>

### Zoom

Zooms in while you hold the key.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Zoom | 4 (1.5 to 20) |
| Scroll to change | on |
| Smooth camera | on |

</details>

## World

### AirPlace

Places blocks in mid-air where the server allows it.

### AutoBreed

Feeds animals so they breed.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 4 (1 to 5) |

</details>

### AutoBuild

Walks around the schematic so Printer can finish it.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Search range | 24 (8 to 48) |

</details>

### AutoDoor

Opens wooden doors in front of you and shuts them behind.

### AutoFarm

Harvests ripe crops and plants them again.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Replant | on |
| Bone meal | off |

</details>

### AutoNametag

Puts name tags on mobs.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Range | 4 (1 to 5) |
| Monsters too | off |

</details>

### AutoShear

Shears sheep.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Range | 4 (1 to 5) |

</details>

### AutoSign

Writes signs for you as you place them.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Line 1 | `Hollow Client` |
| Line 2 | `was here` |
| Line 3 | text |
| Line 4 | text |

</details>

### AutoTorch

Puts a torch at your feet when it gets dark.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Light below | 5 (1 to 12) |

</details>

### AutoTunnel

Digs a tunnel the way you are facing.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Width | 1 (1 to 5) |
| Height | 2 (2 to 4) |
| Walk forward | on |
| Fill the floor | on |

</details>

### BoneMealer

Bone meals saplings and plants around you.

### ChestStealer

Empties or fills chests you open.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Delay | 1 (0 to 10) |
| Take | **Everything**, Only these, All but these |
| Direction | **Take**, Store |
| Items | list of items, 0 picked |
| Close when done | on |

</details>

### Flamethrower

Sets monsters near you on fire with flint and steel.

### LiquidFill

Fills liquids around you with blocks.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Water | on |
| Lava | on |
| Sources only | on |

</details>

### Nuker

Breaks blocks around you.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Break | **Everything**, Only these, All but these |
| Shape | **All around**, Above my feet |
| Blocks | list of blocks, 3 picked |
| Pick the best tool | on |

</details>

### Printer

Places the blocks of your Litematica schematic as you walk near them.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Blocks per tick | 1 (1 to 4) |
| Delay | 2 (0 to 20) |
| Refill hotbar | on |

</details>

### Scaffold

Places blocks under your feet.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Don't fall off | on |
| Climb while jumping | on |

</details>

### SpawnProof

Covers dark floors so nothing spawns there.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Use | list of items, 5 picked |
| Light below | 1 (1 to 8) |

</details>

### StashFinder

Tells you about chunks full of chests and saves them to the log.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Containers in a chunk | 8 (2 to 60) |

</details>

### Timer

Runs the game faster or slower.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Speed | 1.1 (0.1 to 5) |

</details>

### VeinMiner

Mines the rest of a vein or tree after you break one block.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Most blocks | 48 (4 to 200) |
| Blocks | list of blocks, 28 picked |

</details>

## Misc

### AutoMessage

Says something now and then. Check that the server allows it.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Message | text |
| Every minutes | 10 (1 to 120) |

</details>

### AutoReconnect

Joins again after you are disconnected.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| After seconds | 5 (1 to 60) |

</details>

### BetterTab

More players and real ping in the player list.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Ping numbers | on |
| Highlight friends | on |
| Most players | 160 (20 to 400) |

</details>

### BrandSpoof

Tells servers you use another client.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Brand | `vanilla` |

</details>

### ChatTag

Adds Hollow Client to the end of your chat messages.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Tag | `Hollow Client` |
| Divider | **|**, », - |

</details>

### ChatTimes

Shows when each chat message arrived.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Seconds | off |

</details>

### DeathSpot

Remembers where you died.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Save a waypoint | on |

</details>

### FriendAlert

Tells you when friends join or leave the server.

### LagBack

Tells you when the server pulls you back.

### Macro 1

Sends a message or /command when you press its key.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Sends | text |

</details>

### Macro 2

Sends a message or /command when you press its key.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Sends | text |

</details>

### Macro 3

Sends a message or /command when you press its key.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Sends | text |

</details>

### Mentions

Plays a sound when someone says your name or words you choose.

<details><summary>1 setting</summary>

| Setting | Default and range |
| --- | --- |
| Words, with commas between | text |

</details>

### NoToasts

Hides the game's own pop-ups, such as advancements.

### Panic

Switches every module off at once.

### PearlAlert

Tells you when someone throws an ender pearl and which way.

### SoundFilter

Mutes sounds that get annoying.

<details><summary>8 settings</summary>

| Setting | Default and range |
| --- | --- |
| Wither | on |
| Ender dragon | on |
| Thunder | on |
| Portals | on |
| Pistons | off |
| End portal opening | on |
| Villagers | off |
| Explosions | off |

</details>

### VisualRange

Tells you when players come into view.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Tell me in | **Pop-ups**, Chat |
| Leaving too | on |
| Friends too | off |
| Sound | on |

</details>

## HUD elements

### Armor

Worn armor with durability.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Layout | **Across**, Down |
| Durability | **Percent**, Points left, Bar only, Nothing |
| Flash when low | on |

</details>

### Biome

The biome you are in.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### CPS

Your clicks per second.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Clock

The time where you are.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Combo

Hits landed without being hit.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Compass

A strip showing which way you face.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Coordinates

Where you are, and the matching place in the Nether or Overworld.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Other dimension | on |
| Direction | on |
| Hide numbers | off |
| Decimals | 1 (0 to 2) |

</details>

### Effects

Active effects and the time left on them.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Elytra

Durability left on your elytra.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### FPS

Frames per second.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Facing

Direction and angle.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Held items

What you hold, with count and durability.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Hole

Whether you are standing in a safe hole.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Inventory

Your inventory at a glance.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Item counts

How many of the items you pick you are carrying.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Items | list of items, 5 picked |
| Layout | **Across**, Down |
| Show when none | on |

</details>

### Key list

Modules with a key, and whether they are on.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Keystrokes

The movement keys and clicks you are pressing.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | off |

</details>

### Lag warning

Shows when the server stops answering.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Light

Block light at your feet. Monsters need 0.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Memory

Memory the game is using.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Module list

Modules that are on, sliding in and out.

<details><summary>6 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | off |
| Order | **Longest first**, Alphabetical |
| Colours | **Flowing**, Theme, White |
| Side bar | on |
| Details | on |

</details>

### Nearby players

Players you can see, with distance and health.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Most shown | 6 (1 to 15) |

</details>

### Ping

Delay to the server.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Players online

Players on the server.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Radar

A small map of who is around.

<details><summary>8 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Range | 48 (16 to 128) |
| Width | 76 (40 to 160) |
| Players | on |
| Monsters | on |
| Animals | off |
| Turn with me | on |

</details>

### Saturation

Hidden hunger reserve.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Server

The server you are on.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Session

Time since you joined.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Speed

How fast you are moving.

<details><summary>3 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Unit | **km/h**, Blocks per second |

</details>

### TPS

How fast the server is running. 20 is full speed.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Target

Health and gear of whoever you are fighting.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Range | 14 (4 to 40) |
| Mobs | off |

</details>

### Totem pops

Totems the players around you have used up.

<details><summary>2 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |

</details>

### Watermark

The Hollow Client name.

<details><summary>4 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | off |
| Version | on |
| Letter height | 14 (8 to 30) |

</details>

### Welcome

Your name.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### World day

Days passed in the world.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Label colour | theme colour |
| Value colour | colour |
| Show label | on |

</details>

### Your text

Any words you like.

<details><summary>5 settings</summary>

| Setting | Default and range |
| --- | --- |
| Size | 1 (0.5 to 3) |
| Background | on |
| Text | `Hollow Client` |
| Font | **Display**, Bold |
| Colour | theme colour |

</details>

## Client settings

### Interface

Look and behaviour of Hollow Client.

<details><summary>35 settings</summary>

| Setting | Default and range |
| --- | --- |
| Preset | **Glacier**, Aurora, Ember, Crystal, Moss, Gold, Blood, Bone, Custom |
| Accent | colour |
| Second colour | colour |
| Panel colour | colour |
| Rainbow | off |
| Glass darkness | 0.78 (0.1 to 1) |
| Frost | 1 (0 to 1.6) |
| Corners | **Rounded**, Chipped |
| Corner size | 7 (0 to 10) |
| Text font | **Urbanist**, Unbounded, Minecraft |
| Font in the whole game | on |
| Enabled modules | **Fill**, Edge, Diamond |
| Menu size | 1 (0.7 to 1.5) |
| Animation speed | 1 (0.3 to 3) |
| Reduce motion | off |
| Hollow start menu | on |
| Scene | **Ice cavern**, Snowy mountains |
| Cavern detail | **Medium**, High, Low |
| Aurora | on |
| Snowfall | 1 (0 to 2) |
| Show my skin | on |
| Restyle game buttons | on |
| Restyle tooltips | on |
| Glass inventories | on |
| Glass hotbar | on |
| Glass chat | on |
| Hollow loading screen | on |
| Name on pause screen | on |
| Name in window title | on |
| Snow in module menu | on |
| Menu sounds | on |
| Toggle pop-ups | on |
| Toggle messages in chat | off |
| Dot commands | on |
| Rotate | on |

</details>

