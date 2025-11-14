package main.java.logging;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogMetadataManager {

	private final File baseDir;
	private final File archiveDir;

	private final Map<String, String> logRegistry = new HashMap<>();


	public LogMetadataManager(LogManager logManager) {

		this.baseDir = new File("target/logs");
		this.archiveDir = new File(baseDir, "archive");

		if (!baseDir.exists())
			baseDir.mkdirs();
		if (!archiveDir.exists())
			archiveDir.mkdirs();

		autoRegisterExistingLogs();
	}

	private void autoRegisterExistingLogs() {
		File[] subsystemDirs = baseDir.listFiles(File::isDirectory);
		if (subsystemDirs == null)
			return;

		for (File subsystemDir : subsystemDirs) {
			if (subsystemDir.getName().equals("archive"))
				continue;

			File[] logFiles = subsystemDir.listFiles();
			if (logFiles == null)
				continue;

			for (File logFile : logFiles) {
				String name = logFile.getName();
				if (name.endsWith(".log") || name.endsWith(".json")) {
					logRegistry.put(name, subsystemDir.getName());
				}
			}
		}
	}

	public void registerLog(File logFile, String subsystem) {
		if (logFile.exists()) {
			logRegistry.put(logFile.getName(), subsystem);
			System.out.println("Registered: " + logFile.getName() + " under subsystem " + subsystem);
		}
	}

	public void moveLog(String fileName, String newSubsystem) {
		try {
			String oldSubsystem = logRegistry.get(fileName);
			if (oldSubsystem == null) {
				System.out.println("Log not found in registry: " + fileName);
				return;
			}

			File oldFile = new File(baseDir + "/" + oldSubsystem + "/" + fileName);
			File newDir = new File(baseDir + "/" + newSubsystem);

			if (!newDir.exists())
				newDir.mkdirs();

			File newFile = new File(newDir, fileName);
			Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			logRegistry.put(fileName, newSubsystem);

			System.out.println("Moved: " + fileName + " → " + newSubsystem);

		} catch (IOException e) {
			System.out.println("Error moving log: " + e.getMessage());
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
			System.out.println("Deleted: " + fileName);
		} else {
			System.out.println("Unable to delete: " + fileName);
		}
	}

	public void archiveSubsystem(String subsystem) {
		File subsystemDir = new File(baseDir, subsystem);
		File[] files = subsystemDir.listFiles();

		if (files == null || files.length == 0) {
			System.out.println("No logs found to archive for subsystem: " + subsystem);
			return;
		}

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		File archiveFile = new File(archiveDir, subsystem + "_" + timestamp + ".zip");

		try (FileOutputStream fos = new FileOutputStream(archiveFile); ZipOutputStream zos = new ZipOutputStream(fos)) {

			for (File f : files) {
				try (FileInputStream fis = new FileInputStream(f)) {
					zos.putNextEntry(new ZipEntry(f.getName()));
					fis.transferTo(zos);
					zos.closeEntry();
				}
			}

			System.out.println("Archived subsystem logs → " + archiveFile.getAbsolutePath());

		} catch (IOException e) {
			System.out.println("Archive error: " + e.getMessage());
		}
	}

	public Set<String> listSubsystems() {
		return new HashSet<>(logRegistry.values());
	}

	public void refreshRegistry() {
		logRegistry.clear();
		autoRegisterExistingLogs();
		System.out.println("Registry refreshed. Total logs: " + logRegistry.size());
	}

	public List<String> listLogsOfSubsystem(String subsystem) {
		List<String> result = new ArrayList<>();

		for (var e : logRegistry.entrySet()) {
			if (e.getValue().equals(subsystem)) {
				result.add(e.getKey());
			}
		}
		return result;
	}
}
