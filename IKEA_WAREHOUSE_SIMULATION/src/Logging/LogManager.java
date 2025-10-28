package Logging;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class LogManager {
    private final File baseDir = new File("logs");
    private final Map<String, BufferedWriter> writers = new HashMap<>();
    private final DateTimeFormatter fileFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final Pattern timePattern = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{2})\\.log$"); // match timestamp suffix
    private final long ROTATION_INTERVAL_MS = 5000; // 5 seconds

    private final Map<String, Long> lastWrite = new HashMap<>();

    public LogManager() {
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    private synchronized BufferedWriter getWriter(String subsystem) throws IOException {
        long now = System.currentTimeMillis();
        Long last = lastWrite.getOrDefault(subsystem, 0L);
        BufferedWriter writer = writers.get(subsystem);

        // Rotate file if more than 5s passed
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

    public List<String> listLogs(String subsystem) {
        File dir = new File(baseDir, subsystem);
        if (!dir.exists()) return List.of();
        return Arrays.asList(Objects.requireNonNull(dir.list((d, name) -> name.endsWith(".log"))));
    }

    public String openLog(String subsystem, String regexFilter) {
        File dir = new File(baseDir, subsystem);
        if (!dir.exists()) return "No logs";
        Pattern regex = Pattern.compile(regexFilter);
        StringBuilder sb = new StringBuilder();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (regex.matcher(file.getName()).find()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    sb.append("=== ").append(file.getName()).append(" ===\n");
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                } catch (IOException e) {
                    sb.append("Error reading ").append(file.getName()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public Set<String> listSubsystems() {
        return new HashSet<>(Arrays.asList(Objects.requireNonNull(baseDir.list())));
    }
}
