package Simulation;

import Logging.LogMetadataManager;

import java.util.Scanner;

public class LogManagerConsoleUI implements Runnable {
    private final LogMetadataManager logManager;
    private volatile boolean running = true;

    public LogManagerConsoleUI(LogMetadataManager logManager) {
        this.logManager = logManager;
    }

    @SuppressWarnings("resource")
	@Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== 🧾 Log Manager Console UI ===");
        System.out.println("Commands: list | list [subsystem] | move [file] [subsystem] | delete [file] | archive [subsystem] | refresh | exit");

        while (running) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ");

            if (parts[0].equalsIgnoreCase("list")) {
                if (parts.length == 1) logManager.listAll();
                else logManager.listBySubsystem(parts[1]);
            }
            else if (parts[0].equalsIgnoreCase("move") && parts.length == 3)
                logManager.moveLog(parts[1], parts[2]);
            else if (parts[0].equalsIgnoreCase("delete") && parts.length == 2)
                logManager.deleteLog(parts[1]);
            else if (parts[0].equalsIgnoreCase("archive") && parts.length == 2)
                logManager.archiveSubsystem(parts[1]);
            else if (parts[0].equalsIgnoreCase("refresh"))
                logManager.refreshRegistry();
            else if (parts[0].equalsIgnoreCase("exit")) {
                running = false;
                System.out.println("Exiting Log Manager UI...");
            } else {
                System.out.println("Unknown command. Try again.");
            }
        }
    }
}
