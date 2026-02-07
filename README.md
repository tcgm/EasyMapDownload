# EasyMapDownload
Tired of installing minecraft maps manually? This fabric mod adds a button to the select world screen that allows you to instantly install minecraft worlds from any folder on your computer.

## Features
- **Asynchronous scanning** - Progressively scans directories over time without freezing the game
- **Live progress display** - Shows scan progress and worlds as they're found
- **World selection list** - View and select from all valid world zip files in a directory
- **One-click world installation** - Install Minecraft worlds with a single click
- **Native directory browser** - Browse directories using a Minecraft-native interface (no external windows)
- **Smart Downloads detection** - Automatically finds your configured Downloads folder (even if customized)
- **Efficient scanning** - Filters to .zip files only and gracefully handles thousands of files
- **Drive browser** - View all system drives with available space
- **Mouse wheel scrolling** - Scroll through long lists of worlds
- **Quick navigation** - Navigate with folder buttons, type paths directly, or open in Windows Explorer

### Cannot find a valid zip file?
Make sure you have a zip file of a minecraft world in your selected folder. The mod will automatically detect the most recently modified world zip file that contains a `level.dat` file.

Use the **Browse** button to select a different directory if your world files are not in the Downloads folder.

![Gif of the mod being used to install a world ](https://i.gyazo.com/c6b6950d3dbe656e442bb01d639c7d97.gif)

## Building and Testing

### Build the Mod
```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

The built mod jar will be in `build/libs/`

### Install for Testing
1. Build the mod using the command above
2. Copy `build/libs/[modname]-[version].jar` to your Minecraft `.minecraft/mods` folder
3. Make sure you have Fabric Loader and Fabric API installed
4. Launch Minecraft 1.21 with Fabric
5. Go to the Select World screen to see the new "Install Map" button

### Development Testing
```bash
# Run Minecraft client directly from development environment
gradlew.bat runClient
```

This will launch Minecraft with your mod automatically loaded for testing.

## Changelog

### Recent Changes
- **Added directory browsing** - Users can now browse and select any directory to search for world files
- **UI improvements** - Display current directory path on screen
- **Dynamic file detection** - Screen refreshes when browsing to a new directory
