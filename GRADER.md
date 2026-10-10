# Links
- GitHub: https://github.com/Drullkus/noodling
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/noodling

# A priori
A piece of [Penne](https://en.wikipedia.org/wiki/Penne) pasta kicked off this generator. See [a-priori.jpg](a-priori.jpg) file for an image of the original development progress.

# Pasta generator instructions
## Using the generator
1. Run `./gradlew runClient` from project root to launch the game with this mod installed. Or download from CurseForge to run from a client with NeoForge installed. 
2. Put your player in Creative Mode. Find the Noodling tab in the Creative Menu. Put the Noodler into your hot bar and make your player equip it.
3. With the Noodler in the player's main hand, aim and hold the Left Mouse Button in the world for more than half a second. This will place a random piece of pasta.

## Customizing the generator
4. With the Noodler in the player's main hand, click the Right Mouse Button to open the Pasta Generator GUI.
5. Drag the GUI window by the top blue row in order to move it around. Will come in handy if it overlaps the bottom of the screen.
6. The seed field holds the seed of the past generation from the Noodler tool. Check-marking the "Lock seed" box will prevent that seed from changing.

## Sharing
7. At the bottom of the GUI is the "Export .nbt" button that lets users export the last generated pasta as a structure .nbt file. This file can be loaded by Minecraft's Structure Blocks via Redstone.
   - Only by Redstone pulse if any dimension of the exported file is wider than 48 blocks.
   - The `/place template ...` command also works without the 48-block limit.

## Additional customization
8. The Pasta shape drop-down menu starts on "Random Pasta". Change it to a particular pasta shape to reveal settings for it.
9. The seed can be locked in for the player to try different values. Trying a custom value will clear the seed field.
