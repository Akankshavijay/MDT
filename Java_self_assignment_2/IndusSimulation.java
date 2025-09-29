public class IndusSimulation {
	    public static void main(String[] args) {
	        // Robotic Objects
	        AGV forklift = new AGV();
	        forklift.setdata("Forklift", 90.0, 0.12, 45, "Station", 2.5f, 2.0f);

	        AGV moverrobot = new AGV();
	        moverrobot.setdata("PalletMover", 80.0, 0.10, 30, "Robot", 2.8f, 2.2f);

	        AGV belt = new AGV();
	        belt.setdata("ConveyorBot", 75.0, 0.14, 50, "Belt", 3.0f, 2.7f);

	        // Warehouse Functions
	        IOperation loading = new IOperation();
	        loading.setData("LOAD", "Load onto trucks", 25);
	        loading.addResource(forklift);
	        loading.addResource(moverrobot);

	        IOperation transport = new IOperation();
	        transport.setData("TRANSPORT", "Move load to storage", 40);
	        transport.addResource(moverrobot);
	        transport.addResource(belt);

	        IOperation sorting = new IOperation();
	        sorting.setData("SORT", "Deliver load at destination", 15);
	        sorting.addResource(forklift);

	        // Warehouse simulation
	        IndustrialProcess warehouseProcess = new IndustrialProcess();
	        warehouseProcess.setId("WAREHOUSE");
	        warehouseProcess.addOperation(loading);
	        warehouseProcess.addOperation(transport);
	        warehouseProcess.addOperation(sorting);

	        // Print Results
	        System.out.println(forklift.getdata());
	        System.out.println(moverrobot.getdata());
	        System.out.println(belt.getdata());
	        
	        System.out.println(transport.getData());
	        System.out.println(loading.getData());
	        System.out.println(sorting.getData());
	        
	        System.out.println(warehouseProcess.print_indus());
	        System.out.println("Total Duration (minutes): " + warehouseProcess.calculateTotalDuration());
	        System.out.println("Unique AGVs Required    : " + warehouseProcess.countUniqueAGVs());
	    }
	}
