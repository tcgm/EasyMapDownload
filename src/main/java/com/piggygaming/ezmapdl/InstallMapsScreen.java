package com.piggygaming.ezmapdl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.piggygaming.ezmapdl.FileUtils.*;

@Environment(EnvType.CLIENT)
public class InstallMapsScreen extends Screen {

    private final Screen parent;
    private final MinecraftClient client;
    private final File savesDirectory;
    private List<File> worldFiles;
    private File selectedFile;
    /** The currently selected directory to search for map files. Defaults to Downloads folder. */
    private String selectedDirectory;
    private final List<ButtonWidget> worldButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_WORLDS = 8;
    private WorldScanner worldScanner;
    private int lastWorldCount = 0;

    public InstallMapsScreen(Screen parent) throws IOException {
        super(Text.literal("Select World to Install"));
        this.parent = parent;
        this.client = MinecraftClient.getInstance();
        this.savesDirectory = new File(this.client.runDirectory.getPath() + File.separator + "saves");
        this.selectedDirectory = getDownloadsFolder();
        this.worldFiles = new ArrayList<>();
    }

    private void errorScreen(String errorMSG) {
        EasyMapDownload.LOGGER.warn(errorMSG);
        this.client.setScreen(new ErrorScreen(errorMSG, this.parent));
    }
    private void errorScreen(Exception exception) {
        exception.printStackTrace();
        this.client.setScreen(new ErrorScreen(exception.getStackTrace().toString(), this.parent));
    }

    @Override
    protected void init() {

        try {worldFiles = getAllWorldFiles(selectedDirectory);
            if (selectedFile == null && !worldFiles.isEmpty()) {
                selectedFile = worldFiles.get(0);
            }
        } catch (Exception e) {
            errorScreen(e);
        }

        // Browse button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Browse"), (button) -> {
            browseDirectory();
        }).dimensions(this.width / 2 - 150, 20, 100, 20).build());
        
        // Install button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Install Selected"), (button) -> {
            // Check if file exists when confirming
            if (this.selectedFile == null) {
                errorScreen("No world file selected. Please browse to a folder with world zip files.");
                return;
            }
            
            File newFile = new File(savesDirectory.getPath() + File.separator + selectedFile.getName());
            this.client.setScreen(new LoadingScreen(this.parent));
            if (selectedFile.renameTo(newFile)) {
                try {
                    if (fileNotInRootDir(newFile, "level.dat")) {
                        unzipThread thread = new unzipThread(newFile.getPath(), savesDirectory, this.client);
                        thread.start();
                    } else {
                        File dir = new File(savesDirectory.getPath() + File.separator + newFile.getName().replaceFirst("[.][^.]+$", "")); dir.mkdirs();
                        unzipThread thread = new unzipThread(newFile.getPath(), dir, this.client);
                        thread.start();
                    }
                } catch (Exception e) {
                    errorScreen(e);
                }
            }

        }).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build());
        
        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
            this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 5, this.height - 30, 95, 20).build());
        
        refreshWorldList();
    }
    
    private void startScanning() {
        if (worldScanner != null && worldScanner.isScanning()) {
            worldScanner.stopScanning();
        }
        
        worldScanner = new WorldScanner(selectedDirectory);
        worldScanner.start();
        worldFiles = new ArrayList<>();
        lastWorldCount = 0;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Update world list from scanner
        if (worldScanner != null) {
            List<File> scannedFiles = worldScanner.getFoundWorldFiles();
            if (scannedFiles.size() > lastWorldCount) {
                worldFiles = scannedFiles;
                lastWorldCount = scannedFiles.size();
                
                // Auto-select first world if none selected
                if (selectedFile == null && !worldFiles.isEmpty()) {
                    selectedFile = worldFiles.get(0);
                }
                
                // Refresh the display
                refreshWorldList();
            }
        }
    }
    
    @Override
    public void removed() {
        super.removed();
        // Stop scanning when leaving the screen
        if (worldScanner != null) {
            worldScanner.stopScanning();
        }
    }
    
    private void refreshWorldList() {
        // Clear existing world buttons
        worldButtons.forEach(this::remove);
        worldButtons.clear();
        
        if (worldFiles.isEmpty()) {
            return;
        }
        
        int yPos = 50;
        int buttonIndex = 0;
        
        for (int i = scrollOffset; i < worldFiles.size() && buttonIndex < MAX_VISIBLE_WORLDS; i++) {
            File file = worldFiles.get(i);
            final File currentFile = file;
            boolean isSelected = file.equals(selectedFile);
            
            String displayName = (isSelected ? "► " : "") + file.getName();
            
            ButtonWidget button = ButtonWidget.builder(
                Text.literal(displayName),
                (btn) -> {
                    this.selectedFile = currentFile;
                    refreshWorldList();
                }
            ).dimensions(this.width / 2 - 150, yPos, 300, 20).build();
            
            this.addDrawableChild(button);
            worldButtons.add(button);
            
            yPos += 22;
            buttonIndex++;
        }
        
        // Scroll buttons if needed
        int scrollButtonY = yPos + 5;
        if (worldFiles.size() > MAX_VISIBLE_WORLDS) {
            if (scrollOffset > 0) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("▲ Previous"), (button) -> {
                    scrollOffset = Math.max(0, scrollOffset - MAX_VISIBLE_WORLDS);
                    refreshWorldList();
                }).dimensions(this.width / 2 - 150, scrollButtonY, 145, 20).build());
            }
            
            if (scrollOffset + MAX_VISIBLE_WORLDS < worldFiles.size()) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("▼ Next"), (button) -> {
                    scrollOffset += MAX_VISIBLE_WORLDS;
                    refreshWorldList();
                }).dimensions(this.width / 2 + 5, scrollButtonY, 145, 20).build());
            }
        }
    }

    /**
     * Opens a Minecraft-native directory browser to allow the user to select a directory to search for map files.
     * When a directory is selected, the screen refreshes to show all valid Minecraft world zip files in that directory.
     */
    private void browseDirectory() {
        this.client.setScreen(new DirectoryBrowserScreen(this, selectedDirectory, (newPath) -> {
            this.selectedDirectory = newPath;
            
            // Start new scan for the new directory
            this.selectedFile = null;
            scrollOffset = 0;
            startScanning();
            
            // Rebuild the screen
            this.clearChildren();
            this.init();
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 16777215);
        
        // Display the current directory
        String dirDisplay = "Directory: " + selectedDirectory;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(dirDisplay), this.width / 2, this.height - 45, 11184810);
        
        // Display count and scanning status
        if (worldScanner != null && worldScanner.isScanning()) {
            String scanText = "Scanning... " + worldScanner.getFilesScanned() + "/" + worldScanner.getTotalFiles() + " files (" + worldFiles.size() + " worlds found)";
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(scanText), this.width / 2, 35, 16777045);
        } else if (worldFiles.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No valid world zip files found - click Browse"), this.width / 2, this.height / 2 - 20, 16733525);
        } else {
            String countText = "Found " + worldFiles.size() + " world" + (worldFiles.size() == 1 ? "" : "s");
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(countText), this.width / 2, 35, 11184810);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Scroll through worlds with mouse wheel
        if (worldFiles.size() > MAX_VISIBLE_WORLDS) {
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                refreshWorldList();
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(worldFiles.size() - MAX_VISIBLE_WORLDS, scrollOffset + 1);
                refreshWorldList();
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

}
