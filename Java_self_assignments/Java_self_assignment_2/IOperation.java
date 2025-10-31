import java.util.ArrayList;
import java.util.List;

public class IOperation {
    private String id;
    private String description;
    private int nominalTime;
    private List<AGV> resources;

    public IOperation() {
        resources = new ArrayList<>();
    }

    public void setData(String id, String description, int nominalTime) {
        this.id = id;
        this.description = description;
        this.nominalTime = nominalTime;
    }

    public void addResource(AGV agv) {
        if (agv != null) {
            resources.add(agv);
        }
    }

    public List<AGV> getResources() {
        return resources;
    }

    public int getduration() {
        return nominalTime;
    }

    public String getData() {
        StringBuilder sb = new StringBuilder();
        sb.append("IOperation{id=").append(id)
          .append(", description=").append(description)
          .append(", nominalTime=").append(nominalTime)
          .append(", resources=[");
        for (int i = 0; i < resources.size(); i++) {
            sb.append(resources.get(i).getId());
            if (i < resources.size() - 1) sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }
}
