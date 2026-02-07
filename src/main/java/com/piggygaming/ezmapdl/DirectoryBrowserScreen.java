package com.piggygaming.ezmapdl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom directory browser screen for Minecraft.
 * Allows users to navigate directories and select a folder without using AWT/Swing.
 */
@Environment(EnvType.CLIENT)
public class DirectoryBrowserScreen extends Screen {
    
    private final Screen parent;
    private final DirectorySelectCallback callback;
    private String currentPath;
    private TextFieldWidget pathField;
    private final List<ButtonWidget> directoryButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean showDrives = false;
    private static final int MAX_VISIBLE_DIRECTORIES = 10;
    
    @FunctionalInterface
    public interface DirectorySelectCallback {
        void onDirectorySelected(String path);
    }
    
    public DirectoryBrowserScreen(Screen parent, String initialPath, DirectorySelectCallback callback) {
        super(Text.literal("Browse for Directory"));
        this.parent = parent;
        this.currentPath = initialPath;
        this.callback = callback;
    }
    
    @Override
    protected void init() {
        // Path text field at the top
        this.pathField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 20, 300, 20, Text.literal("Path"));
        this.pathField.setMaxLength(500);
        this.pathField.setText(currentPath);
        this.addSelectableChild(this.pathField);
        
        // Navigate to typed path button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Go"), (button) -> {
            String path = this.pathField.getText();
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                this.currentPath = dir.getAbsolutePath();
                this.pathField.setText(this.currentPath);
                showDrives = false;
                refreshDirectoryList();
            }
        }).dimensions(this.width / 2 + 155, 20, 40, 20).build());
        
        // Parent directory button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("⬆ Parent"), (button) -> {
            File current = new File(currentPath);
            File parent = current.getParentFile();
            if (parent != null && parent.exists()) {
                this.currentPath = parent.getAbsolutePath();
                this.pathField.setText(this.currentPath);
                showDrives = false;
                refreshDirectoryList();
            }
        }).dimensions(this.width / 2 - 150, 45, 80, 20).build());
        
        // List drives/roots button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Drives"), (button) -> {
            scrollOffset = 0;
            showDrives = true;
            this.pathField.setText("[Drives]");
            refreshDirectoryList();
        }).dimensions(this.width / 2 - 65, 45, 60, 20).build());
        
        // Confirm button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Select This Folder"), (button) -> {
            callback.onDirectorySelected(currentPath);
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 30, 100, 20).build());
        
        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
            this.client.setScreen(parent);
        }).dimensions(this.width / 2 + 5, this.height - 30, 95, 20).build());
        
        // Open in Explorer button (Windows only)
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Open in Explorer"), (button) -> {
                try {
                    Runtime.getRuntime().exec("explorer.exe \"" + currentPath + "\"");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).dimensions(this.width / 2 + 10, 45, 140, 20).build());
        }
        
        refreshDirectoryList();
    }
    
    private void refreshDirectoryList() {
        // Clear existing directory buttons
        directoryButtons.forEach(this::remove);
        directoryButtons.clear();
        
        File current = new File(currentPath);
        File[] files;
        
        if (showDrives) {
            // Show drives/roots
            files = File.listRoots();
        } else {
            files = current.listFiles(File::isDirectory);
        }
        
        if (files == null) {
            files = new File[0];
        }
        
        // Sort directories
        Arrays.sort(files);
        
        int yPos = 75;
        int buttonIndex = 0;
        
        for (int i = scrollOffset; i < files.length && buttonIndex < MAX_VISIBLE_DIRECTORIES; i++) {
            File dir = files[i];
            final String dirPath = dir.getAbsolutePath();
            
            String displayName = showDrives ? (dir.getAbsolutePath() + (dir.getTotalSpace() > 0 ? " (" + dir.getTotalSpace() / 1073741824 + " GB)" : "")) : ("📁 " + dir.getName());
            ButtonWidget button = ButtonWidget.builder(
                Text.literal(displayName),
                (btn) -> {
                    this.currentPath = dirPath;
                    this.pathField.setText(this.currentPath);
                    scrollOffset = 0;
                    showDrives = false;
                    refreshDirectoryList();
                }
            ).dimensions(this.width / 2 - 150, yPos, 300, 20).build();
            
            this.addDrawableChild(button);
            directoryButtons.add(button);
            
            yPos += 22;
            buttonIndex++;
        }
        
        // Scroll buttons if needed
        if (files.length > MAX_VISIBLE_DIRECTORIES) {
            if (scrollOffset > 0) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("▲ Previous"), (button) -> {
                    scrollOffset = Math.max(0, scrollOffset - MAX_VISIBLE_DIRECTORIES);
                    refreshDirectoryList();
                }).dimensions(this.width / 2 - 150, yPos, 145, 20).build());
            }
            
            if (scrollOffset + MAX_VISIBLE_DIRECTORIES < files.length) {
                this.addDrawableChild(ButtonWidget.builder(Text.literal("▼ Next"), (button) -> {
                    scrollOffset += MAX_VISIBLE_DIRECTORIES;
                    refreshDirectoryList();
                }).dimensions(this.width / 2 + 5, yPos, 145, 20).build());
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        
        // Draw path field
        this.pathField.render(context, mouseX, mouseY, delta);
        
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 16777215);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Scroll through directories with mouse wheel
        File current = new File(currentPath);
        File[] files = current.listFiles(File::isDirectory);
        
        if (files != null && files.length > MAX_VISIBLE_DIRECTORIES) {
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                refreshDirectoryList();
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(files.length - MAX_VISIBLE_DIRECTORIES, scrollOffset + 1);
                refreshDirectoryList();
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
