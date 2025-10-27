package Logging;


import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import Exception_Handler.WarehouseException;

public class LogManager {
    private final Path baseDir = Paths.get("logs");
    private final WarehouseException handler = new WarehouseException();

    public LogManager() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            handler.handleWarehouseOperation("LogManager Init", () -> {
                throw new RuntimeException(e);
            });
        }
    }

    public synchronized void log(String subsystem, String message) {
        String date = LocalDate.now().toString();
        Path subsystemDir = baseDir.resolve(subsystem);

        handler.handleWarehouseOperation("Log Write", () -> {
            try {
                Files.createDirectories(subsystemDir);
                Path logFile = subsystemDir.resolve(date + ".log");
                if (!Files.exists(logFile)) Files.createFile(logFile);

                try (BufferedWriter bw = Files.newBufferedWriter(logFile, StandardOpenOption.APPEND)) {
                    bw.write(LocalTime.now() + " - " + message);
                    bw.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write log: " + e.getMessage());
            }
        });
    }

    public List<String> listSubsystems() {
        try (Stream<Path> s = Files.list(baseDir)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public List<String> listLogs(String subsystem) {
        Path dir = baseDir.resolve(subsystem);
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public String openLog(String subsystem, String date) {
        Path file = baseDir.resolve(subsystem).resolve(date.endsWith(".log") ? date : date + ".log");
        if (!Files.exists(file)) return "No log found for this date.";
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "Failed to open log: " + e.getMessage();
        }
    }
}
