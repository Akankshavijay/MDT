package main.java.dashboard;

import main.java.logging.LogManager;
import main.java.logging.LogMetadataManager;
import main.java.robotManagement.RobotManager;
import main.java.robotManagement.Robot;
import main.java.storageManagement.Bin;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.WarehouseTask;
import main.java.Simulation;
import main.java.chargingManagement.ChargingManager;
import main.java.chargingManagement.ChargingStation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class Dashboard extends JFrame {

	private static final long serialVersionUID = 1L;

	private final RobotManager robotManager;
	private final StorageManager storageManager;
	private final TaskManager taskManager;
	private final LogManager logger;

	private ChargingManager chargingManager;

	private JTable robotTable;
	private JTable taskTable;
	private JTable binTable;
	private JTable stationTable;
	private JTable chargingQueueTable;
	private MapPanel mapPanel;

	private JTextArea dischargeQueueArea;
	private JTextArea chargedQueueArea;

	private JProgressBar binUsageBar;
	private JLabel lowBatteryLabel;

	private final LogMetadataManager logMetadata;

	private JList<String> subsystemList;
	private JList<String> logFileList;

	private final Timer refreshTimer = new Timer(true);

	public Dashboard(RobotManager robotManager, StorageManager storageManager, TaskManager taskManager,
			LogManager logger) {

		super("Warehouse Dashboard");

		this.robotManager = robotManager;
		this.storageManager = storageManager;
		this.taskManager = taskManager;
		this.logger = logger;

		this.logMetadata = new LogMetadataManager(logger);
		this.chargingManager = robotManager.getChargingManager();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1500, 850);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout(10, 10));

		add(buildMainPanel(), BorderLayout.CENTER);
		startAutoRefresh();
	}

	private JPanel buildMainPanel() {
		JTabbedPane tabs = new JTabbedPane();
		
		tabs.add("Map", buildMapPanel());
		tabs.add("Robots", buildRobotPanel());
		tabs.add("Tasks", buildTaskPanel());
		tabs.add("Statistics", buildStatsPanel());
		tabs.add("Charging", buildChargingPanel());
		tabs.add("Logs", buildLogsPanel());

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(tabs, BorderLayout.CENTER);

		return panel;
	}
	
	private JPanel buildMapPanel() {
		mapPanel = new MapPanel();

	    JPanel container = new JPanel(new BorderLayout());
	    container.add(mapPanel, BorderLayout.CENTER);

	    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

	    JButton addBinBtn = new JButton("Add Bin");
	    addBinBtn.addActionListener(e -> createBinDialog());

	    JButton addRobotBtn = new JButton("Add Robot");
	    addRobotBtn.addActionListener(e -> createRobotDialog());

	    JButton addStationBtn = new JButton("Add Station");
	    addStationBtn.addActionListener(e -> createStationDialog());

	    JLabel speedLabel = new JLabel("Simulation speed:");
	    JComboBox<String> speedBox = new JComboBox<>(new String[]{"1x", "2x", "4x", "8x"});
	    speedBox.setSelectedItem("1x");
	    speedBox.addActionListener(e -> {
	        String sel = (String) speedBox.getSelectedItem();
	        int speed = Simulation.SPEED_1X;
	        if ("2x".equals(sel)) speed = Simulation.SPEED_2X;
	        else if ("4x".equals(sel)) speed = Simulation.SPEED_4X;
	        else if ("8x".equals(sel)) speed = Simulation.SPEED_8X;
	        Simulation.setSimulationSpeed(speed);
	        logger.log("Dashboard", "Simulation speed set to " + sel);
	    });

	    controls.add(addBinBtn);
	    controls.add(addRobotBtn);
	    controls.add(addStationBtn);
	    controls.add(Box.createHorizontalStrut(20));
	    controls.add(speedLabel);
	    controls.add(speedBox);

	    container.add(controls, BorderLayout.SOUTH);

	    return container;
	}

	private class MapPanel extends JPanel {
	    private static final int CELL_SIZE = 40;
	    private static final int MARGIN = 40;

	    @Override
	    protected void paintComponent(Graphics g) {
	        super.paintComponent(g);
	        Graphics2D g2 = (Graphics2D) g;
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	        g2.setColor(Color.WHITE);
	        g2.fillRect(0, 0, getWidth(), getHeight());

	        g2.setColor(new Color(230, 230, 230));
	        for (int x = MARGIN; x < getWidth(); x += CELL_SIZE) {
	            g2.drawLine(x, 0, x, getHeight());
	        }
	        for (int y = MARGIN; y < getHeight(); y += CELL_SIZE) {
	            g2.drawLine(0, y, getWidth(), y);
	        }

	        g2.setColor(new Color(180, 200, 255));
	        for (Bin b : storageManager.getBins().values()) {
	            int px = MARGIN + b.getX() * CELL_SIZE;
	            int py = MARGIN + b.getY() * CELL_SIZE;
	            int size = CELL_SIZE / 2;
	            int x = px - size / 2;
	            int y = py - size / 2;

	            g2.setColor(b.isOccupied() ? new Color(100, 150, 255) : new Color(200, 220, 255));
	            g2.fillRect(x, y, size, size);
	            g2.setColor(Color.BLACK);
	            g2.drawRect(x, y, size, size);
	            g2.drawString(b.getId(), x, y - 4);
	        }

	        for (Robot r : robotManager.getRobots().values()) {
	            int px, py;
	            try {
	                px = MARGIN + r.getX() * CELL_SIZE;
	                py = MARGIN + r.getY() * CELL_SIZE;
	            } catch (Exception ex) {
	                continue;
	            }

	            int size = CELL_SIZE / 2;
	            int x = px - size / 2;
	            int y = py - size / 2;

	            g2.setColor(new Color(200, 255, 200));
	            g2.fillOval(x, y, size, size);
	            g2.setColor(Color.BLACK);
	            g2.drawOval(x, y, size, size);
	            g2.drawString(r.getId(), x, y - 4);
	        }

	        if (chargingManager != null) {
	            try {
	                Field f = ChargingManager.class.getDeclaredField("stations");
	                f.setAccessible(true);
	                @SuppressWarnings("unchecked")
	                java.util.List<Object> stations = (java.util.List<Object>) f.get(chargingManager);

	                g2.setColor(new Color(255, 220, 180));
	                for (Object s : stations) {
	                    int sx = (int) s.getClass().getMethod("getX").invoke(s);
	                    int sy = (int) s.getClass().getMethod("getY").invoke(s);
	                    String id = (String) s.getClass().getMethod("getId").invoke(s);

	                    int px = MARGIN + sx * CELL_SIZE;
	                    int py = MARGIN + sy * CELL_SIZE;
	                    int size = CELL_SIZE / 2;

	                    int[] xs = { px, px - size / 2, px + size / 2 };
	                    int[] ys = { py - size / 2, py + size / 2, py + size / 2 };
	                    g2.setColor(new Color(255, 220, 180));
	                    g2.fillPolygon(xs, ys, 3);
	                    g2.setColor(Color.BLACK);
	                    g2.drawPolygon(xs, ys, 3);
	                    g2.drawString(id, px - size / 2, py - size);
	                }
	            } catch (Exception ignored) {
	            }
	        }
	    }

	    @Override
	    public Dimension getPreferredSize() {
	        return new Dimension(800, 600);
	    }
	}

	private JPanel buildRobotPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		robotTable = new JTable(
				new DefaultTableModel(new String[] { "ID", "Type", "Battery", "Status", "Current Task" }, 0));
		panel.add(new JScrollPane(robotTable), BorderLayout.CENTER);

		JPanel bottom = new JPanel(new GridLayout(2, 1));

		JPanel q = new JPanel(new GridLayout(1, 2, 10, 10));
		dischargeQueueArea = new JTextArea();
		dischargeQueueArea.setEditable(false);
		dischargeQueueArea.setBorder(BorderFactory.createTitledBorder("🔋 Charging Queue"));

		chargedQueueArea = new JTextArea();
		chargedQueueArea.setEditable(false);
		chargedQueueArea.setBorder(BorderFactory.createTitledBorder("⚡ Fully Charged Robots"));

		q.add(new JScrollPane(dischargeQueueArea));
		q.add(new JScrollPane(chargedQueueArea));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton showAll = new JButton("Show All Robots");
		showAll.addActionListener(e -> showAllRobots());

		JButton showOne = new JButton("Show Robot Details");
		showOne.addActionListener(e -> showOneRobot());

		buttons.add(showAll);
		buttons.add(showOne);

		bottom.add(q);
		bottom.add(buttons);

		panel.add(bottom, BorderLayout.SOUTH);
		return panel;
	}

	private void showAllRobots() {
		StringBuilder sb = new StringBuilder();
		for (Robot r : robotManager.getRobots().values()) {
			sb.append("ID: ").append(r.getId()).append("\n").append("Battery: ").append(r.getBattery()).append("%\n")
					.append("Status: ").append(r.getStatus()).append("\n").append("Task: ").append(r.getCurrentTask())
					.append("\n").append("----------------------\n");
		}
		JOptionPane.showMessageDialog(this, sb.toString(), "All Robots", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showOneRobot() {
		int row = robotTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a robot first.");
			return;
		}

		String id = robotTable.getValueAt(row, 0).toString();
		Robot r = robotManager.getRobots().get(id);
		if (r == null)
			return;

		JOptionPane
				.showMessageDialog(this,
						"ID: " + r.getId() + "\n" + "Battery: " + r.getBattery() + "%\n" + "Status: " + r.getStatus()
								+ "\n" + "Task: " + r.getCurrentTask(),
						"Robot Details", JOptionPane.INFORMATION_MESSAGE);
	}

	private JPanel buildTaskPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		taskTable = new JTable(new DefaultTableModel(new String[] { "Task ID", "Type", "Item", "Bin", "State" }, 0));
		panel.add(new JScrollPane(taskTable), BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton showAll = new JButton("Show All Tasks");
		showAll.addActionListener(e -> showAllTasks());

		JButton showOne = new JButton("Show Task Details");
		showOne.addActionListener(e -> showOneTask());

		buttons.add(showAll);
		buttons.add(showOne);

		panel.add(buttons, BorderLayout.SOUTH);

		return panel;
	}

	private void showAllTasks() {
		StringBuilder sb = new StringBuilder();
		for (WarehouseTask t : taskManager.getTasksSnapshot()) {
			sb.append(t).append("\n--------------------\n");
		}
		JOptionPane.showMessageDialog(this, sb.toString(), "All Tasks", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showOneTask() {
		int row = taskTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a task first.");
			return;
		}

		String id = taskTable.getValueAt(row, 0).toString();
		for (WarehouseTask t : taskManager.getTasksSnapshot()) {
			if (t.getId().equals(id)) {
				JOptionPane.showMessageDialog(this, t.toString(), "Task Details", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
		}
	}

	private JPanel buildStatsPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		binUsageBar = new JProgressBar();
		binUsageBar.setStringPainted(true);

		lowBatteryLabel = new JLabel("Low battery robots: 0");

		top.add(new JLabel("Bin Utilization:"));
		top.add(binUsageBar);
		top.add(Box.createVerticalStrut(10));
		top.add(lowBatteryLabel);

		panel.add(top, BorderLayout.NORTH);

		binTable = new JTable(new DefaultTableModel(new String[] { "Bin ID", "Status", "Item" }, 0));
		JScrollPane scroll = new JScrollPane(binTable);
		scroll.setBorder(BorderFactory.createTitledBorder("📦 Bin Status"));

		panel.add(scroll, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton showAll = new JButton("Show All Bins");
		showAll.addActionListener(e -> showAllBins());

		JButton showOne = new JButton("Show Bin Details");
		showOne.addActionListener(e -> showOneBin());

		buttons.add(showAll);
		buttons.add(showOne);

		panel.add(buttons, BorderLayout.SOUTH);

		return panel;
	}

	private void showAllBins() {
		StringBuilder sb = new StringBuilder();
		for (Bin b : storageManager.getBins().values()) {
			sb.append("Bin: ").append(b.getId()).append("\n").append("Occupied: ").append(b.isOccupied()).append("\n")
					.append("Item: ").append(b.getItem().orElse(null)).append("\n").append("------------------\n");
		}
		JOptionPane.showMessageDialog(this, sb.toString(), "All Bins", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showOneBin() {
		int row = binTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a bin first.");
			return;
		}

		String id = binTable.getValueAt(row, 0).toString();
		Bin b = storageManager.getBins().get(id);

		JOptionPane.showMessageDialog(this,
				"Bin: " + b.getId() + "\n" + "Occupied: " + b.isOccupied() + "\n" + "Item: " + b.getItem().orElse(null),
				"Bin Details", JOptionPane.INFORMATION_MESSAGE);
	}

	private JPanel buildChargingPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));

		stationTable = new JTable(new DefaultTableModel(
				new String[] { "Station ID", "Status", "Robot", "Time Remaining (min)", "X", "Y" }, 0));
		JScrollPane stationPane = new JScrollPane(stationTable);
		stationPane.setBorder(BorderFactory.createTitledBorder("⚡ Charging Stations"));

		chargingQueueTable = new JTable(
				new DefaultTableModel(new String[] { "Robot ID", "Battery %", "Status", "Queue Position" }, 0));
		JScrollPane queuePane = new JScrollPane(chargingQueueTable);
		queuePane.setBorder(BorderFactory.createTitledBorder("🔋 Waiting Queue"));

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stationPane, queuePane);
		split.setResizeWeight(0.6);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> {
			updateChargingStationTable();
			updateChargingQueueTable();
		});

		JButton stationDetailsBtn = new JButton("Show Station Details");
		stationDetailsBtn.addActionListener(e -> showChargingStationDetails());

		JButton robotDetailsBtn = new JButton("Show Robot in Station");
		robotDetailsBtn.addActionListener(e -> showRobotInStation());

		JButton queueRobotDetailsBtn = new JButton("Show Queue Robot Details");
		queueRobotDetailsBtn.addActionListener(e -> showQueueRobotDetails());

		buttons.add(refreshBtn);
		buttons.add(stationDetailsBtn);
		buttons.add(robotDetailsBtn);
		buttons.add(queueRobotDetailsBtn);

		panel.add(split, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);

		return panel;
	}

	private void updateChargingStationTable() {
		if (chargingManager == null || stationTable == null)
			return;

		DefaultTableModel model = (DefaultTableModel) stationTable.getModel();
		model.setRowCount(0);

		try {
			Field f = ChargingManager.class.getDeclaredField("stations");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<Object> stations = (List<Object>) f.get(chargingManager);

			for (Object station : stations) {

				String id = (String) station.getClass().getMethod("getId").invoke(station);
				Object status = station.getClass().getMethod("getStatus").invoke(station);
				Robot robot = (Robot) station.getClass().getMethod("getCurrentRobot").invoke(station);

				Object timeObj = station.getClass().getMethod("timeRemainingMinutes").invoke(station);
				double time = (timeObj instanceof Number) ? ((Number) timeObj).doubleValue() : 0.0;

				int x = (int) station.getClass().getMethod("getX").invoke(station);
				int y = (int) station.getClass().getMethod("getY").invoke(station);

				model.addRow(new Object[] { id, status.toString(), (robot == null ? "-" : robot.getId()),
						String.format("%.2f", time), x, y });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateChargingQueueTable() {
		if (chargingManager == null || chargingQueueTable == null)
			return;

		DefaultTableModel model = (DefaultTableModel) chargingQueueTable.getModel();
		model.setRowCount(0);

		try {
			Field f = ChargingManager.class.getDeclaredField("queue");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Robot> queue = (Queue<Robot>) f.get(chargingManager);

			int pos = 0;
			for (Robot r : queue) {
				model.addRow(new Object[] { r.getId(), r.getBattery() + "%", r.getStatus(), pos++ });
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showChargingStationDetails() {
		if (stationTable == null)
			return;
		int row = stationTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a charging station first.");
			return;
		}

		String id = stationTable.getValueAt(row, 0).toString();
		String status = stationTable.getValueAt(row, 1).toString();
		String robot = stationTable.getValueAt(row, 2).toString();
		String time = stationTable.getValueAt(row, 3).toString();
		String x = stationTable.getValueAt(row, 4).toString();
		String y = stationTable.getValueAt(row, 5).toString();

		JOptionPane.showMessageDialog(this,
				"Station ID: " + id + "\n" + "Status: " + status + "\n" + "Robot: " + robot + "\n" + "Time Remaining: "
						+ time + " minutes\n" + "Location: (" + x + "," + y + ")",
				"Charging Station Details", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showRobotInStation() {
		if (stationTable == null)
			return;
		int row = stationTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a station first.");
			return;
		}

		String robotId = stationTable.getValueAt(row, 2).toString();
		if ("-".equals(robotId)) {
			JOptionPane.showMessageDialog(this, "No robot is charging in this station.");
			return;
		}

		Robot r = robotManager.getRobots().get(robotId);
		if (r == null) {
			JOptionPane.showMessageDialog(this, "Robot not found in RobotManager.");
			return;
		}

		JOptionPane.showMessageDialog(this,
				"Robot ID: " + r.getId() + "\n" + "Battery: " + r.getBattery() + "%\n" + "Status: " + r.getStatus()
						+ "\n" + "Current Task: " + r.getCurrentTask(),
				"Robot Charging Details", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showQueueRobotDetails() {
		if (chargingQueueTable == null)
			return;
		int row = chargingQueueTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Select a robot in the queue first.");
			return;
		}

		String robotId = chargingQueueTable.getValueAt(row, 0).toString();
		Robot r = robotManager.getRobots().get(robotId);
		if (r == null) {
			JOptionPane.showMessageDialog(this, "Robot not found in RobotManager.");
			return;
		}

		JOptionPane.showMessageDialog(this,
				"Robot ID: " + r.getId() + "\n" + "Battery: " + r.getBattery() + "%\n" + "Status: " + r.getStatus()
						+ "\n" + "Current Task: " + r.getCurrentTask(),
				"Charging Queue Robot Details", JOptionPane.INFORMATION_MESSAGE);
	}

	private JPanel buildLogsPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));

		subsystemList = new JList<>();
		subsystemList.setBorder(BorderFactory.createTitledBorder("Subsystems"));
		subsystemList.addListSelectionListener(e -> refreshLogsOfSelectedSubsystem());

		logFileList = new JList<>();
		logFileList.setBorder(BorderFactory.createTitledBorder("Log Files"));

		JPanel lists = new JPanel(new GridLayout(1, 2, 10, 10));
		lists.add(new JScrollPane(subsystemList));
		lists.add(new JScrollPane(logFileList));

		JPanel buttons = new JPanel(new GridLayout(1, 7, 10, 10));

		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> refreshLogSubsystems());

		JButton deleteBtn = new JButton("Delete");
		deleteBtn.addActionListener(e -> deleteLog());

		JButton moveBtn = new JButton("Move");
		moveBtn.addActionListener(e -> moveLogToOtherSubsystem());

		JButton archiveBtn = new JButton("Archive");
		archiveBtn.addActionListener(e -> archiveSubsystem());

		JButton openBtn = new JButton("Open");
		openBtn.addActionListener(e -> openLogFile());

		JButton subsystemDetails = new JButton("Subsystem Info");
		subsystemDetails.addActionListener(e -> showSubsystemDetails());

		JButton fileDetails = new JButton("File Info");
		fileDetails.addActionListener(e -> showLogFileDetails());

		buttons.add(refreshBtn);
		buttons.add(deleteBtn);
		buttons.add(moveBtn);
		buttons.add(archiveBtn);
		buttons.add(openBtn);
		buttons.add(subsystemDetails);
		buttons.add(fileDetails);

		panel.add(lists, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);

		refreshLogSubsystems();
		return panel;
	}

	private void refreshLogSubsystems() {
		logMetadata.refreshRegistry();
		Set<String> subs = logMetadata.listSubsystems();
		subsystemList.setListData(subs.toArray(String[]::new));
		logFileList.setListData(new String[0]);
	}

	private void refreshLogsOfSelectedSubsystem() {
		String subsystem = subsystemList.getSelectedValue();
		if (subsystem == null) {
			logFileList.setListData(new String[0]);
			return;
		}
		List<String> files = new ArrayList<>();

		try {
			Field f = LogMetadataManager.class.getDeclaredField("logRegistry");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, String> reg = (Map<String, String>) f.get(logMetadata);

			for (var e : reg.entrySet()) {
				if (e.getValue().equals(subsystem)) {
					files.add(e.getKey());
				}
			}
		} catch (Exception ignored) {
		}

		logFileList.setListData(files.toArray(String[]::new));
	}

	private void deleteLog() {
		String file = logFileList.getSelectedValue();
		if (file != null) {
			logMetadata.deleteLog(file);
			refreshLogsOfSelectedSubsystem();
		}
	}

	private void moveLogToOtherSubsystem() {
		String file = logFileList.getSelectedValue();
		if (file == null)
			return;

		String newSubsystem = JOptionPane.showInputDialog(this, "Move to subsystem:", "Move Log",
				JOptionPane.PLAIN_MESSAGE);

		if (newSubsystem != null && !newSubsystem.isBlank()) {
			logMetadata.moveLog(file, newSubsystem);
			refreshLogSubsystems();
		}
	}

	private void archiveSubsystem() {
		String subsystem = subsystemList.getSelectedValue();
		if (subsystem != null) {
			logMetadata.archiveSubsystem(subsystem);
		}
	}

	private void openLogFile() {
		String subsystem = subsystemList.getSelectedValue();
		String file = logFileList.getSelectedValue();

		if (subsystem == null || file == null) {
			JOptionPane.showMessageDialog(this, "Select a subsystem and a file first.");
			return;
		}

		File logFile = new File("target/logs/" + subsystem + "/" + file);

		if (!logFile.exists()) {
			JOptionPane.showMessageDialog(this, "File does not exist.");
			return;
		}

		StringBuilder content = new StringBuilder();
		try (Scanner sc = new Scanner(logFile)) {
			while (sc.hasNextLine()) {
				content.append(sc.nextLine()).append("\n");
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Failed to open log: " + e.getMessage());
			return;
		}

		JTextArea area = new JTextArea(content.toString());
		area.setEditable(false);
		area.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JFrame viewer = new JFrame("Log Viewer — " + file);
		viewer.setSize(800, 600);
		viewer.add(new JScrollPane(area));
		viewer.setLocationRelativeTo(this);
		viewer.setVisible(true);
	}

	private void showSubsystemDetails() {
		String subsystem = subsystemList.getSelectedValue();
		if (subsystem == null) {
			JOptionPane.showMessageDialog(this, "Select a subsystem.");
			return;
		}

		JOptionPane.showMessageDialog(this,
				"Subsystem: " + subsystem + "\n" + "Files: " + logFileList.getModel().getSize(), "Subsystem Info",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void showLogFileDetails() {
		String subsystem = subsystemList.getSelectedValue();
		String file = logFileList.getSelectedValue();

		if (subsystem == null || file == null) {
			JOptionPane.showMessageDialog(this, "Select a file first.");
			return;
		}

		File logFile = new File("target/logs/" + subsystem + "/" + file);

		JOptionPane.showMessageDialog(this, "File: " + file + "\n" + "Size: " + logFile.length() + " bytes\n"
				+ "Path:\n" + logFile.getAbsolutePath(), "File Info", JOptionPane.INFORMATION_MESSAGE);
	}

	private void startAutoRefresh() {
		refreshTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(() -> {
					updateRobotTable();
					updateQueues();
					updateTaskTable();
					updateStats();
					updateBinTable();
					updateChargingStationTable();
					updateChargingQueueTable();
	                if (mapPanel != null) {
	                    mapPanel.repaint();
	                }
				});
			}
		}, 0, 2000);
	}
	
	private void createBinDialog() {
	    String id = JOptionPane.showInputDialog(this, "Bin ID:", "New Bin", JOptionPane.PLAIN_MESSAGE);
	    if (id == null || id.isBlank()) return;

	    String sx = JOptionPane.showInputDialog(this, "X coordinate (int):", "0");
	    String sy = JOptionPane.showInputDialog(this, "Y coordinate (int):", "0");
	    try {
	        int x = Integer.parseInt(sx);
	        int y = Integer.parseInt(sy);
	        Bin b = new Bin(id, x, y);

	        try {
	            StorageManager.class.getMethod("addBin", Bin.class).invoke(storageManager, b);
	        } catch (NoSuchMethodException nsme) {
	        	storageManager.getBins().put(id, b);
	        }

	        logger.log("Dashboard", "Created new Bin " + id + " at (" + x + "," + y + ")");
	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(this, "Invalid coordinates: " + ex.getMessage());
	    }
	}

	private void createRobotDialog() {
	    String id = JOptionPane.showInputDialog(this, "Robot ID:", "New Robot", JOptionPane.PLAIN_MESSAGE);
	    if (id == null || id.isBlank()) return;

	    String sx = JOptionPane.showInputDialog(this, "X coordinate (int):", "0");
	    String sy = JOptionPane.showInputDialog(this, "Y coordinate (int):", "0");
	    String sb = JOptionPane.showInputDialog(this, "Initial battery (0-100):", "100");

	    try {
	        int x = Integer.parseInt(sx);
	        int y = Integer.parseInt(sy);
	        int battery = Integer.parseInt(sb);
	        if (battery < 0) battery = 0;
	        if (battery > 100) battery = 100;

	        Robot r = new Robot(id, x, y, battery, "RobotManager", logger);
	        robotManager.addRobot(r);

	        logger.log("Dashboard", "Created new Robot " + id + " at (" + x + "," + y + ") battery=" + battery + "%");
	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
	    }
	}

	private void createStationDialog() {
	    if (chargingManager == null) {
	        JOptionPane.showMessageDialog(this, "No ChargingManager configured.");
	        return;
	    }

	    String id = JOptionPane.showInputDialog(this, "Station ID:", "New Station", JOptionPane.PLAIN_MESSAGE);
	    if (id == null || id.isBlank()) return;

	    String sx = JOptionPane.showInputDialog(this, "X coordinate (int):", "0");
	    String sy = JOptionPane.showInputDialog(this, "Y coordinate (int):", "0");

	    try {
	        int x = Integer.parseInt(sx);
	        int y = Integer.parseInt(sy);

	        ChargingStation station = new ChargingStation(id, x, y, "ChargingManager", logger);
	        chargingManager.addStation(station);

	        logger.log("Dashboard", "Created new ChargingStation " + id + " at (" + x + "," + y + ")");
	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(this, "Invalid coordinates: " + ex.getMessage());
	    }
	}


	private void updateRobotTable() {
		DefaultTableModel model = (DefaultTableModel) robotTable.getModel();
		model.setRowCount(0);

		for (Robot r : robotManager.getRobots().values()) {
			model.addRow(new Object[] { r.getId(), "Robot", r.getBattery() + "%", r.getStatus(),
					r.getCurrentTask() == null ? "None"
							: r.getCurrentTask().getType() + " (" + r.getCurrentTask().getId() + ")" });
		}
	}

	private void updateQueues() {
		Queue<Robot> q = chargingManager.getQueue();

		StringBuilder sb = new StringBuilder();
		for (Robot r : q) {
			sb.append(r.getId()).append(" → ").append(r.getBattery()).append("%\n");
		}
		dischargeQueueArea.setText(sb.isEmpty() ? "No robots in queue." : sb.toString());

		StringBuilder charged = new StringBuilder();
		for (Robot r : robotManager.getRobots().values()) {
			if (r.getBattery() >= 95 && r.getStatus() == Robot.Status.READY) {
				charged.append(r.getId()).append(" (").append(r.getBattery()).append("%)\n");
			}
		}
		chargedQueueArea.setText(charged.isEmpty() ? "None" : charged.toString());
	}

	private void updateTaskTable() {
		DefaultTableModel model = (DefaultTableModel) taskTable.getModel();
		model.setRowCount(0);

		for (WarehouseTask t : taskManager.getTasksSnapshot()) {
			model.addRow(new Object[] { t.getId(), t.getType(), t.getItemType(), t.getBinId(), t.getState() });
		}
	}

	private void updateStats() {
		Map<String, Bin> bins = storageManager.getBins();

		long used = bins.values().stream().filter(Bin::isOccupied).count();
		long total = bins.size();

		int percent = total == 0 ? 0 : (int) (100.0 * used / total);

		binUsageBar.setValue(percent);
		binUsageBar.setString(used + " / " + total + " occupied");

		long lowBattery = robotManager.getRobots().values().stream().filter(r -> r.getBattery() <= 20).count();

		lowBatteryLabel.setText("Low battery robots: " + lowBattery);
	}

	private void updateBinTable() {
		DefaultTableModel model = (DefaultTableModel) binTable.getModel();
		model.setRowCount(0);

		for (Bin b : storageManager.getBins().values()) {
			model.addRow(new Object[] { b.getId(), b.isOccupied() ? "Occupied" : "Free",
					b.getItem().map(i -> i.getId() + " (" + i.getType() + ")").orElse("-") });
		}
	}

	@Override
	public void dispose() {
		refreshTimer.cancel();
		super.dispose();
	}

	public static void launchDashboard(RobotManager robotManager, StorageManager storageManager,
			TaskManager taskManager, LogManager logger) {
		SwingUtilities.invokeLater(() -> {
			Dashboard dash = new Dashboard(robotManager, storageManager, taskManager, logger);
			dash.setVisible(true);
		});
	}
}
