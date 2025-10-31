package Logging;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class LogManager {
    private final File baseDir = new File("logs");
    private final Map<String, BufferedWriter> writers = new HashMap<>();
    private final Map<String, Long> lastWrite = new HashMap<>();
    private final DateTimeFormatter fileFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final long ROTATION_INTERVAL_MS = 5000;
    private final Pattern jsonPattern = Pattern.compile(".*\\.json$");

    public LogManager() {
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    private synchronized BufferedWriter getWriter(String subsystem) throws IOException {
        long now = System.currentTimeMillis();
        Long last = lastWrite.getOrDefault(subsystem, 0L);
        BufferedWriter writer = writers.get(subsystem);

        if (writer == null || (now - last) > ROTATION_INTERVAL_MS) {
            if (writer != null) writer.close();

            String timestamp = LocalDateTime.now().format(fileFormat);
            File dir = new File(baseDir, subsystem);
            if (!dir.exists()) dir.mkdirs();
            File logFile = new File(dir, subsystem + "_" + timestamp + ".log");
            writer = new BufferedWriter(new FileWriter(logFile, true));

            writers.put(subsystem, writer);
            lastWrite.put(subsystem, now);
        }
        return writer;
    }

    public synchronized void log(String subsystem, String message) {
        try {
            BufferedWriter writer = getWriter(subsystem);
            String line = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " | " + message;
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Logging failed for " + subsystem + ": " + e.getMessage());
        }
    }

    public synchronized void logAGV(String agvId, String message) {
        try {
            File dir = new File(baseDir, "AGV/" + agvId);
            if (!dir.exists()) dir.mkdirs();
            String timestamp = LocalDateTime.now().format(fileFormat);
            File logFile = new File(dir, agvId + "_" + timestamp + ".log");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                bw.write(time + " | " + message);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("AGV log failed: " + e.getMessage());
        }
    }

    public synchronized void saveAsJson(String subsystem, String jsonContent) {
        try {
            String timestamp = LocalDateTime.now().format(fileFormat);
            File dir = new File(baseDir, subsystem);
            if (!dir.exists()) dir.mkdirs();

            File jsonFile = new File(dir, subsystem + "_" + timestamp + ".json");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile))) {
                bw.write(jsonContent);
            }
        } catch (IOException e) {
            System.out.println("JSON save failed for " + subsystem + ": " + e.getMessage());
        }
    }



    public List<String> listLogs(String subsystem) {
        File dir = new File(baseDir, subsystem);
        if (!dir.exists()) return List.of();
        return Arrays.asList(Objects.requireNonNull(dir.list((d, name) -> name.endsWith(".log") || jsonPattern.matcher(name).find())));
    }

    public Set<String> listSubsystems() {
        String[] dirs = baseDir.list();
        return dirs == null ? Set.of() : new HashSet<>(Arrays.asList(dirs));
    }
}
