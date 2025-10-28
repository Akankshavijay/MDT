package Logging;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogMetadataManager {

    private final File baseDir = new File("logs");
    private final File archiveDir = new File("logs/archive");
    private final Map<String, String> logRegistry = new HashMap<>(); // filename → subsystem

    public LogMetadataManager() {
        if (!baseDir.exists()) baseDir.mkdirs();
        if (!archiveDir.exists()) archiveDir.mkdirs();
    }

    public void registerLog(File logFile, String subsystem) {
        if (logFile.exists()) {
            logRegistry.put(logFile.getName(), subsystem);
            System.out.println("Registered: " + logFile.getName() + " under subsystem " + subsystem);
        }
    }

    // ---------------- MOVE LOG FILE ----------------
    public void moveLog(String fileName, String newSubsystem) {
        try {
            String oldSubsystem = logRegistry.get(fileName);
            if (oldSubsystem == null) {
                System.out.println("Log not found: " + fileName);
                return;
            }

            File oldFile = new File(baseDir + "/" + oldSubsystem + "/" + fileName);
            File newDir = new File(baseDir + "/" + newSubsystem);
            if (!newDir.exists()) newDir.mkdirs();

            File newFile = new File(newDir, fileName);
            Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            logRegistry.put(fileName, newSubsystem);
            System.out.println("Moved " + fileName + " to subsystem " + newSubsystem);
        } catch (IOException e) {
            System.out.println("Error moving file: " + e.getMessage());
        }
    }


    public void deleteLog(String fileName) {
        String subsystem = logRegistry.get(fileName);
        if (subsystem == null) {
            System.out.println("Log not found: " + fileName);
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


    public void archiveSubsystem(String subsystem) {
        File subsystemDir = new File(baseDir, subsystem);
        if (!subsystemDir.exists() || subsystemDir.listFiles() == null) {
            System.out.println("No logs to archive for: " + subsystem);
            return;
        }

        String archiveName = subsystem + "_" + LocalDateTime.now().toString().replace(":", "-") + ".zip";
        File archiveFile = new File(archiveDir, archiveName);

        try (FileOutputStream fos = new FileOutputStream(archiveFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : Objects.requireNonNull(subsystemDir.listFiles())) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    fis.transferTo(zos);
                    zos.closeEntry();
                }
            }
            System.out.println("Archived logs for subsystem: " + subsystem + " → " + archiveFile.getName());
        } catch (IOException e) {
            System.out.println("Error creating archive: " + e.getMessage());
        }
    }


    public void listAll() {
        System.out.println("\n=== Registered Log Metadata ===");
        logRegistry.forEach((file, subsystem) ->
                System.out.println(file + " → " + subsystem));
    }
}

