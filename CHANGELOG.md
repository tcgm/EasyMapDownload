# Changelog

All notable changes to EasyMapDownload will be documented in this file.

## [Unreleased]

### Added
- **Asynchronous world scanning**: Scans directories progressively in background thread without blocking UI
- **Live progress display**: Shows "Scanning... X/Y files (Z worlds found)" while scanning
- **World selection list**: Shows all valid world zip files found in directory, not just the most recent
- **Scrollable world list**: Browse through multiple worlds with mouse wheel or navigation buttons
- **World counter**: Displays how many valid worlds were found
- **Native directory browser**: Custom Minecraft-based directory browsing screen (no AWT/Swing)
- **Multiple navigation methods**: Click folders, type/paste paths, or open in Windows Explorer
- **Scroll support**: Mouse wheel scrolling through long directory lists
- **Drive/root browsing**: Easy access to all drives on your system with size display
- **Smart Downloads detection**: Automatically detects user's configured Downloads folder (Windows/Mac/Linux)
- **Current directory display**: Screen shows the currently selected directory path
- **Dynamic file detection**: Screen automatically updates as new worlds are found

### Changed
- **Scanning now asynchronous**: No longer blocks UI when scanning large directories
- Screen now lists ALL valid world files instead of just the most recent one
- Users can select which world to install from a list
- Title changed to "Select World to Install" to reflect new functionality
- Optimized file scanning to filter .zip files first before validation
- World list updates automatically as scanning progresses
- Replaced JFileChooser (Swing) with native Minecraft GUI to avoid HeadlessException crashes
- Downloads folder detection now checks system configuration instead of hardcoded paths
- Error handling improved for cases when no valid zip file is found in selected directory
- Drive browser shows drive letter and available space

### Fixed
- **HeadlessException crash**: Solved java.awt.HeadlessException when clicking Browse button
- **Drives button now working**: Fixed logic to properly show all system drives when clicked
- **Custom Downloads folder**: Now respects Windows custom Downloads folder settings
- **NullPointerException with corrupted zips**: Gracefully skips invalid or corrupted zip files instead of crashing
- **Unsupported compression methods**: Handles zip files with unsupported compression (like method 9) without errors
- Files can now be browsed in native Minecraft environment without external dependencies

### Technical Details
- Added `WorldScanner` class for asynchronous background scanning
- Scanner runs in daemon thread with progress tracking
- Added `tick()` override to update UI as scan progresses
- Added `removed()` override to stop scanner when leaving screen
- Scanner includes small delays every 10 files to avoid overwhelming system
- Added `getAllWorldFiles()` method to return list of all valid world files
- Added `DirectoryBrowserScreen` class for native directory navigation
- Added `showDrives` flag to control drive listing mode
- Added `getDownloadsFolder()` utility method with PowerShell integration for Windows
- Changed `InstallMapsScreen` to display scrollable list of worlds with selection
- Added `worldFiles` list and `selectedFile` tracking
- Added `refreshWorldList()` method to rebuild world button list
- Enhanced drive display to show capacity information
- Made `getFileExtension()` public for use by WorldScanner

## [1.1.1] - Previous Release
- Initial functionality with hardcoded Downloads folder support
