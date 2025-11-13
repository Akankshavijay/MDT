package main.java.dashboard;

import main.java.logging.LogManager;
import main.java.robotManagement.RobotManager;
import main.java.storageManagement.Bin;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.WarehouseTask;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class Dashboard extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private final RobotManager robotManager;
    private final StorageManager storageManager;
    private final TaskManager taskManager;
    private final LogManager logger;

    private final Timer refreshTimer = new Timer(true);

    public Dashboard(RobotManager robotManager,
    					StorageManager storageManager,
    					TaskManager taskManager,
    					LogManager logger) {
    	
        super("Warehouse Dashboard");
        this.robotManager = robotManager;
        this.storageManager = storageManager;
        this.taskManager = taskManager;
        this.logger = logger;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(1300, 750);
        setLocationRelativeTo(null);

        add(buildMainPanel(), BorderLayout.CENTER);
        startAutoRefresh();
    }

    private JPanel buildMainPanel() {
    	JPanel mainPanel = new JPanel(new BorderLayout());
    	// todo
		return mainPanel;
	}

	// ---------- AGV PANEL ----------
    private JPanel buildRobotPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        // todo
        return mainPanel;
    }

    // ---------- TASK PANEL ----------
    private JPanel buildTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // todo
        return panel;
    }

    // ---------- STATISTICS PANEL ----------
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        return panel;
    }

    private void startAutoRefresh() {
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                	// todo: updates
                });
            }
        }, 0, 2000); // refresh every 2 seconds
    }

    // ---------- UPDATE SECTIONS ----------
    private void updateRobotTable() {
    	if (robotManager == null) return;
    	// todo
    }

    private void updateTaskTable() {
        if (taskManager == null) return;
        // todo
    }

    @Override
    public void dispose() {
        super.dispose();
        refreshTimer.cancel();
    }

    public static void launchDashboard(RobotManager robotManager,
                                       StorageManager storageManager,
                                       TaskManager taskManager,
                                       LogManager logger) {
        SwingUtilities.invokeLater(() -> {
        	Dashboard dash = new Dashboard(robotManager, storageManager, taskManager, logger);
            dash.setVisible(true);
        });
    }
}
