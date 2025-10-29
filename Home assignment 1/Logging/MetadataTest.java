package Logging;


import java.io.File;

public class MetadataTest {
    public static void main(String[] args) {
        LogMetadataManager manager = new LogMetadataManager();

        File logFile = new File("logs/StorageSystem/test_log.log");

        manager.registerLog(logFile, "StorageSystem");
        manager.moveLog("test_log.log", "RobotSystem");
        manager.archiveSubsystem("RobotSystem");
        manager.deleteLog("test_log.log");

        manager.listAll();
    }
}
