package com.piggygaming.ezmapdl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileUtils {

    /**
     * Gets the user's actual Downloads folder, checking system settings.
     * Falls back to default locations if the configured path cannot be determined.
     * 
     * @return The path to the Downloads folder
     */
    public static String getDownloadsFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Try Windows Shell folders via powershell for custom Downloads location
            try {
                Process process = Runtime.getRuntime().exec(new String[]{
                    "powershell.exe",
                    "-Command",
                    "[Environment]::GetFolderPath('MyDocuments')".replace("MyDocuments", "UserProfile") + " + '\\Downloads'"
                });
                
                // Try getting from registry
                process = Runtime.getRuntime().exec(new String[]{
                    "powershell.exe", 
                    "-Command",
                    "(New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path"
                });
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String path = reader.readLine();
                reader.close();
                process.waitFor();
                
                if (path != null && !path.trim().isEmpty() && new File(path).exists()) {
                    return path;
                }
            } catch (Exception e) {
                // Fall through to defaults
            }
            
            // Try common Windows locations
            String userProfile = System.getenv("USERPROFILE");
            if (userProfile != null) {
                // Check Downloads folder in user profile
                File downloads = new File(userProfile, "Downloads");
                if (downloads.exists() && downloads.isDirectory()) {
                    return downloads.getAbsolutePath();
                }
            }
        } else if (os.contains("mac")) {
            // macOS
            String home = System.getProperty("user.home");
            File downloads = new File(home, "Downloads");
            if (downloads.exists() && downloads.isDirectory()) {
                return downloads.getAbsolutePath();
            }
        } else {
            // Linux and other Unix-like systems
            String home = System.getProperty("user.home");
            
            // Try XDG user dirs first
            File xdgConfig = new File(home, ".config/user-dirs.dirs");
            if (xdgConfig.exists()) {
                try (BufferedReader reader = new BufferedReader(new java.io.FileReader(xdgConfig))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("XDG_DOWNLOAD_DIR=")) {
                            String path = line.substring(17).replace("\"", "").replace("$HOME", home);
                            File downloads = new File(path);
                            if (downloads.exists() && downloads.isDirectory()) {
                                return downloads.getAbsolutePath();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fall through to defaults
                }
            }
            
            // Default Linux Downloads
            File downloads = new File(home, "Downloads");
            if (downloads.exists() && downloads.isDirectory()) {
                return downloads.getAbsolutePath();
            }
        }
        
        // Final fallback
        return System.getProperty("user.home") + File.separator + "Downloads";
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName().replaceAll("[:]",""));

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void unzipFile(String fileZip, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    /**
     * Gets all valid Minecraft world zip files from a directory.
     * @param directoryFilePath The directory to search
     * @return List of all valid world zip files, sorted by modification date (newest first)
     */
    public static List<File> getAllWorldFiles(String directoryFilePath) throws IOException {
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        
        if (files == null || files.length == 0) {
            return List.of();
        }
        
        // Filter to only .zip files first to reduce processing
        File[] zipFiles = Arrays.stream(files)
            .filter(f -> getFileExtension(f).equals(".zip"))
            .toArray(File[]::new);
        
        if (zipFiles.length == 0) {
            return List.of();
        }
        
        // Sort by modification date (newest first)
        Arrays.sort(zipFiles, Comparator.comparingLong(File::lastModified).reversed());
        
        // Collect all valid world files
        return Arrays.stream(zipFiles)
            .filter(file -> {
                try {
                    return zipfileContains(file, "level.dat");
                } catch (Exception e) {
                    // Skip corrupted or invalid zip files
                    EasyMapDownload.LOGGER.debug("Skipping invalid zip file: " + file.getName());
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    public static File getLastModified(String directoryFilePath) throws IOException {
        List<File> worldFiles = getAllWorldFiles(directoryFilePath);
        return worldFiles.isEmpty() ? null : worldFiles.get(0);
    }
    
    public static boolean fileNotInRootDir(File zip, String targetFile) {
        return zipfileContains(zip, "/" + targetFile);
    }

    public static List<String> listContents(File file){
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            List<String> fileContent = zipFile.stream().map(ZipEntry::getName).collect(Collectors.toList());
            zipFile.close();
            return fileContent;
        }
        catch (IOException ioException) {
            // Silently skip corrupted or invalid zip files
            // System.out.println("Error opening zip file: " + file.getName() + " - " + ioException.getMessage());
        }
        return null;
    }

    public static boolean zipfileContains(File zipfile, String targetFile) {
        List<String> list = listContents(zipfile);
        if (list == null) {
            return false; // Invalid or corrupted zip file
        }
        for (String file : list) {
            if (file.contains(targetFile)) {
                return true;
            }
        }
        return false;
    }

}
