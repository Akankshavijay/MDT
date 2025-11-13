package test.java.logging;

import main.java.logging.LogManager;
import main.java.logging.LogMetadataManager;

import java.io.File;

public class MetadataTest {
	public static void main(String[] args) {

		LogManager logger = new LogManager();

		LogMetadataManager manager = new LogMetadataManager(logger);

		File logFile = new File("target/logs/StorageSystem/test_log.log");

		logFile.getParentFile().mkdirs();

		try {
			if (!logFile.exists())
				logFile.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 4️⃣ Run operations
		manager.registerLog(logFile, "StorageSystem");
		manager.moveLog("test_log.log", "RobotSystem");
		manager.archiveSubsystem("RobotSystem");
		manager.deleteLog("test_log.log");

	}
}
