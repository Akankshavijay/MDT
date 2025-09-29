import java.util.ArrayList;
import java.util.List;

public class IndustrialProcess {
    private String id;
    private List<IOperation> operations;

    public IndustrialProcess() {
        operations = new ArrayList<>();
    }


    public void setId(String processId) { 
    	id = processId;
    }
    
    public void addOperation(IOperation op) { 
    	operations.add(op); 
    }

    public int calculateTotalDuration() {
        int total = 0;
        for (IOperation op : operations) {
            total = total + op.getduration();
        }
        return total;
    }


    public int countUniqueAGVs() {
        java.util.List<String> uniqueIds = new java.util.ArrayList<>();
        for (IOperation op : operations) {
            for (AGV a : op.getResources()) {
                boolean included = false;
                for (String id : uniqueIds) {
                    if (id.equals(a.getId())) {
                    	included = true;
                        break;
                    }
                }
                if (!included) {
                    uniqueIds.add(a.getId());
                }
            }
        }
        return uniqueIds.size();
    }


    public String print_indus() {
        return "IndustrialProcess{id=" + id +
               ", operations=" + operations.size() +
               ", durationMinutes=" + calculateTotalDuration() +
               ", uniqueAGVs=" + countUniqueAGVs() + "}";
    }
}

