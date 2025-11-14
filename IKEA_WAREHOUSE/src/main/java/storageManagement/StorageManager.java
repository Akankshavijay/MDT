package main.java.storageManagement;

import main.java.Manager;
import main.java.communication.WarehouseMessage;
import main.java.exceptionHandler.StorageException;
import main.java.exceptionHandler.WarehouseException;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;
import main.java.robotManagement.RobotTask;
import main.java.taskManager.TaskType;

import java.io.*;
import java.util.*;

public class StorageManager extends Manager {
	private final Map<String, Bin> bins = new HashMap<>();

	public StorageManager(String systemName, LogManager logger) {
		super(systemName, logger);
		onInitialize();
	}

	public StorageManager(String systemName, LogManager logger, Collection<Bin> initialBins) {
		super(systemName, logger);
		for (Bin b : initialBins) {
			bins.put(b.getId(), b);
		}
		logger.log(systemName, "StorageManager initialized with " + bins.size() + " bins");
	}

	public Optional<Bin> findFreeBinForStore(String preferredBinId) {
		if (preferredBinId != null) {
			Bin b = bins.get(preferredBinId);
			if (b != null && !b.isOccupied())
				return Optional.of(b);
			return Optional.empty();
		}

		return bins.values().stream().filter(b -> !b.isOccupied()).findFirst();
	}

	public Optional<Bin> findAndReserveBinForStore(String preferredBinId, String taskId) {
		if (preferredBinId != null) {
			Bin b = bins.get(preferredBinId);
			if (b != null && b.tryReserve(taskId))
				return Optional.of(b);
			return Optional.empty();
		}

		for (Bin b : bins.values()) {
			if (b.tryReserve(taskId))
				return Optional.of(b);
		}
		return Optional.empty();
	}

	public void releaseReservation(String binId, String taskId) throws StorageException {
		Bin b = requireBin(binId);
		b.releaseReservation(taskId);
	}

	public Optional<Bin> findOccupiedBinForRetrieve(String binId) {
		if (binId == null)
			return Optional.empty();

		Bin b = bins.get(binId);
		if (b != null && b.isOccupied())
			return Optional.of(b);
		return Optional.empty();
	}

	public boolean canExecute(TaskType type, String binId) {
		switch (type) {
		case STORE:
			return findFreeBinForStore(binId).isPresent();
		case RETRIEVE:
			return findOccupiedBinForRetrieve(binId).isPresent();
		default:
			return false;
		}
	}

	public Bin requireBin(String binId) throws StorageException {
		Bin b = bins.get(binId);
		if (b == null)
			throw new StorageException("Bin not found: " + binId);
		return b;
	}

	public void applyAfterRobot(TaskType type, String binId, String taskId, Item item) throws StorageException {
		try {
			switch (type) {
			case STORE: {
				Bin b = requireBin(binId);
				b.commitStore(taskId, item);
				logger.log(systemName, "Item " + item + " stored to " + binId);
				break;
			}
			case RETRIEVE: {
				Bin b = requireBin(binId);
				Item taken = b.commitRetrieve();
				logger.log(systemName, "Item " + taken + " retrieved from " + binId);
				break;
			}
			default:
				throw new StorageException("Unsupported task type " + type);
			}
		} catch (IllegalStateException | IllegalArgumentException e) {
			throw new StorageException(e.getMessage(), e);
		}
	}

	public boolean isBinOccupied(String binId) {
		Bin bin = bins.get(binId);
		return bin != null && bin.isOccupied();
	}

	public Optional<Item> getItem(String binId) {
		Bin bin = bins.get(binId);
		return bin != null ? bin.getItem() : Optional.empty();
	}

	public void addBin(Bin bin) {
		bins.put(bin.getId(), bin);
	}
	
	public Map<String, Bin> getBins() {
		return bins;
	}

	@Override
	protected void loopOnce() {

	}
}
