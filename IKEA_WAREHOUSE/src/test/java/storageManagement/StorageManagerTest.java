package test.java.storageManagement;

import java.io.*;

import main.java.taskManager.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import main.java.logging.LogManager;
import main.java.storageManagement.Bin;
import main.java.storageManagement.Item;
import main.java.storageManagement.StorageManager;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StorageManagerTest {
	LogManager logger = new LogManager();
	StorageManager storage = new StorageManager("StorageManager", logger);

	private Bin binA, binB;

	@BeforeEach
	void setUp() {
		logger = new LogManager();
		binA = new Bin("A", 1, 1);
		binB = new Bin("B", 2, 2);
		storage = new StorageManager("StorageTest", logger, Arrays.asList(binA, binB));
	}

	@Test
	void reservesFreeBinForStore_preferredFirst() throws Exception {
		Optional<Bin> reserved = storage.findAndReserveBinForStore("A", "T1");
		assertTrue(reserved.isPresent());
		assertEquals("A", reserved.get().getId());
		assertEquals(Bin.Status.RESERVED, reserved.get().getStatus());
	}

	@Test
	void cannotReserveSameBinTwice() throws Exception {
		assertTrue(storage.findAndReserveBinForStore("A", "T1").isPresent());
		Optional<Bin> second = storage.findAndReserveBinForStore("A", "T2");
		assertTrue(second.isEmpty(), "Second reservation should fail on the same bin");
	}

	@Test
	void commitsStoreToOccupied() throws Exception {
		Bin r = storage.findAndReserveBinForStore("A", "T1").orElseThrow();
		storage.applyAfterRobot(TaskType.STORE, r.getId(), "T1", new Item("I-1", "TYPE"));
		assertEquals(Bin.Status.OCCUPIED, r.getStatus());
		assertTrue(r.getItem().isPresent());
		assertEquals("I-1", r.getItem().get().getId());
	}

	@Test
	void releaseReservationMakesBinFreeAgain() throws Exception {
		Bin r = storage.findAndReserveBinForStore(null, "T1").orElseThrow();
		assertEquals(Bin.Status.RESERVED, r.getStatus());
		storage.releaseReservation(r.getId(), "T1");
		assertEquals(Bin.Status.FREE, r.getStatus());
	}

	@Test
	void retrieveFromOccupiedFreesTheBin() throws Exception {
		Bin r = storage.findAndReserveBinForStore("A", "T1").orElseThrow();
		storage.applyAfterRobot(TaskType.STORE, r.getId(), "T1", new Item("I-1", "TYPE"));
		assertEquals(Bin.Status.OCCUPIED, r.getStatus());

		storage.applyAfterRobot(TaskType.RETRIEVE, r.getId(), "T2", null);
		assertEquals(Bin.Status.FREE, r.getStatus());
		assertTrue(r.getItem().isEmpty());
	}
}