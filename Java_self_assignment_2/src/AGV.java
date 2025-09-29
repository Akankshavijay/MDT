package Assignment;

public class AGV {

    private String id;
    private double batteryLoad;
    private double consumption;        
    private int chargingTime;
    private String position;
    private float maxspeed;
    private float actspeed;

    public void setdata(String agvId, double battery, double cons,
                        int charging, String pos, float maxSpd, float actSpd) {
        id = agvId;
        batteryLoad = battery;
        consumption = cons;
        chargingTime = charging;
        position = pos;
        maxspeed = maxSpd;
        actspeed = actSpd;
    }

    public String getdata() {
        return "AGV{id=" + id +
               ", batteryLoad=" + batteryLoad +
               ", consumption=" + consumption +
               ", chargingTime=" + chargingTime +
               ", position=" + position +
               ", maxspeed=" + maxspeed +
               ", actspeed=" + actspeed + "}";
    }


    public String getId() { 
    	return id; 
    }
    
    public double getConsumption() {
    	return consumption; 
    }
}
