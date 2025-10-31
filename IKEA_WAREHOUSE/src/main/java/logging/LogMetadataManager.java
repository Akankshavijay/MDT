package main.java.logging;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * LogMetadataManager
 * -------------------
 * Manages metadata and lifecycle for all subsystem log files.
 * Enables a UI or CLI to register, view, move, delete, or archive logs dynamically.
 */
public class LogMetadataManager {

    private final File baseDir = new File("logs");
    private final File archiveDir = new File("logs/archive");
    private final Map<String, String> logRegistry = new HashMap<>();

    public LogMetadataManager() {
        if (!baseDir.exists()) baseDir.mkdirs();
        if (!archiveDir.exists()) archiveDir.mkdirs();
        autoRegisterExistingLogs();
    }

    /** Automatically scan and register existing logs in /logs directory */
    private void autoRegisterExistingLogs() {
        File[] subsystemDirs = baseDir.listFiles(File::isDirectory);
        if (subsystemDirs == null) return;

        for (File subsystemDir : subsystemDirs) {
            if (subsystemDir.getName().equals("archive")) continue; // skip archive folder
            for (File logFile : Objects.requireNonNull(subsystemDir.listFiles())) {
                String name = logFile.getName();
                if (name.endsWith(".log") || name.endsWith(".json")) {
                    logRegistry.put(name, subsystemDir.getName());
                }
            }
        }
    }

    /** Register a specific log file in metadata */
    public void registerLog(File logFile, String subsystem) {
        if (logFile.exists()) {
            logRegistry.put(logFile.getName(), subsystem);
            System.out.println("Registered: " + logFile.getName() + " under subsystem " + subsystem);
        }
    }

    /** Move log file to another subsystem folder */
    public void moveLog(String fileName, String newSubsystem) {
        try {
            String oldSubsystem = logRegistry.get(fileName);
            if (oldSubsystem == null) {
                System.out.println("Log not found in registry: " + fileName);
                return;
            }

            File oldFile = new File(baseDir + "/" + oldSubsystem + "/" + fileName);
            File newDir = new File(baseDir + "/" + newSubsystem);
            if (!newDir.exists()) newDir.mkdirs();

            File newFile = new File(newDir, fileName);
            Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            logRegistry.put(fileName, newSubsystem);
            System.out.println("Moved log file: " + fileName + " → " + newSubsystem);
        } catch (IOException e) {
            System.out.println("Error moving log: " + e.getMessage());
        }
    }

    /** Delete a log file */
    public void deleteLog(String fileName) {
        String subsystem = logRegistry.get(fileName);
        if (subsystem == null) {
            System.out.println("Log not found in registry: " + fileName);
            return;
        }

        File logFile = new File(baseDir + "/" + subsystem + "/" + fileName);
        if (logFile.exists() && logFile.delete()) {
            logRegistry.remove(fileName);
            System.out.println("Deleted log file: " + fileName);
        } else {
            System.out.println("Unable to delete: " + fileName);
        }
    }

    /** Archive all logs for a specific subsystem into ZIP under /logs/archive/ */
    public void archiveSubsystem(String subsystem) {
        File subsystemDir = new File(baseDir, subsystem);
        if (!subsystemDir.exists() || subsystemDir.listFiles() == null) {
            System.out.println("No logs found to archive for subsystem: " + subsystem);
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File archiveFile = new File(archiveDir, subsystem + "_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(archiveFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : Objects.requireNonNull(subsystemDir.listFiles())) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    fis.transferTo(zos);
                    zos.closeEntry();
                }
            }
            System.out.println("✅ Archived subsystem: " + subsystem + " → " + archiveFile.getName());
        } catch (IOException e) {
            System.out.println("Error during archive: " + e.getMessage());
        }
    }

    /** List all registered logs and their subsystem ownership */
    public void listAll() {
        System.out.println("\n=== Registered Log Metadata ===");
        if (logRegistry.isEmpty()) {
            System.out.println("No logs registered yet.");
        } else {
            logRegistry.forEach((file, subsystem) ->
                    System.out.println(file + " → " + subsystem));
        }
    }

    /** List logs for a specific subsystem */
    public void listBySubsystem(String subsystem) {
        System.out.println("\n=== Logs under subsystem: " + subsystem + " ===");
        logRegistry.entrySet().stream()
                .filter(e -> e.getValue().equals(subsystem))
                .forEach(e -> System.out.println(e.getKey()));
    }

    /** Get all subsystems with logs */
    public Set<String> listSubsystems() {
        return new HashSet<>(logRegistry.values());
    }

    /** Refresh registry by rescanning log directories */
    public void refreshRegistry() {
        logRegistry.clear();
        autoRegisterExistingLogs();
        System.out.println("Refreshed log registry. Total logs: " + logRegistry.size());
    }
}
