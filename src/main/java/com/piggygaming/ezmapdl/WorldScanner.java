package com.piggygaming.ezmapdl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Asynchronous world file scanner that progressively scans a directory
 * for valid Minecraft world zip files without blocking the main thread.
 */
public class WorldScanner extends Thread {
    
    private final String directoryPath;
    private final List<File> foundWorldFiles;
    private volatile boolean isScanning;
    private volatile boolean shouldStop;
    private int filesScanned;
    private int totalFiles;
    
    public WorldScanner(String directoryPath) {
        this.directoryPath = directoryPath;
        this.foundWorldFiles = new ArrayList<>();
        this.isScanning = false;
        this.shouldStop = false;
        this.filesScanned = 0;
        this.totalFiles = 0;
        this.setDaemon(true);
        this.setName("WorldScanner-Thread");
    }
    
    @Override
    public void run() {
        isScanning = true;
        
        try {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles(File::isFile);
            
            if (files == null || files.length == 0) {
                return;
            }
            
            // Filter to only .zip files first
            File[] zipFiles = Arrays.stream(files)
                .filter(f -> FileUtils.getFileExtension(f).equals(".zip"))
                .toArray(File[]::new);
            
            totalFiles = zipFiles.length;
            
            if (zipFiles.length == 0) {
                return;
            }
            
            // Sort by modification date (newest first)
            Arrays.sort(zipFiles, Comparator.comparingLong(File::lastModified).reversed());
            
            // Scan through all zip files progressively
            for (File file : zipFiles) {
                if (shouldStop) {
                    break;
                }
                
                filesScanned++;
                
                try {
                    if (FileUtils.zipfileContains(file, "level.dat")) {
                        synchronized (foundWorldFiles) {
                            foundWorldFiles.add(file);
                        }
                    }
                } catch (Exception e) {
                    // Skip corrupted or invalid zip files
                    EasyMapDownload.LOGGER.debug("Skipping invalid zip file: " + file.getName());
                }
                
                // Small delay to avoid overwhelming the system
                if (filesScanned % 10 == 0) {
                    Thread.sleep(10);
                }
            }
        } catch (Exception e) {
            EasyMapDownload.LOGGER.error("Error during world scanning", e);
        } finally {
            isScanning = false;
        }
    }
    
    /**
     * Stops the scanning process.
     */
    public void stopScanning() {
        shouldStop = true;
    }
    
    /**
     * Gets a copy of the currently found world files.
     * Thread-safe.
     */
    public List<File> getFoundWorldFiles() {
        synchronized (foundWorldFiles) {
            return new ArrayList<>(foundWorldFiles);
        }
    }
    
    /**
     * Returns true if the scanner is currently scanning.
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Returns the number of files scanned so far.
     */
    public int getFilesScanned() {
        return filesScanned;
    }
    
    /**
     * Returns the total number of files to scan.
     */
    public int getTotalFiles() {
        return totalFiles;
    }
    
    /**
     * Returns the scanning progress as a percentage (0-100).
     */
    public int getProgress() {
        if (totalFiles == 0) return 0;
        return (int) ((filesScanned / (float) totalFiles) * 100);
    }
}
