package monitor;

/**
 * Created by Aleksander Skraastad (myth) on 7/10/15.
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * Support class that maintains statistics and provides statistics calculations
 */
public class MonitorStatistics {

    private Integer totalIncidents;
    private Integer totalIncidentsAboveAssignmentThreshold;
    private Integer totalIncidentsAboveResolutionThreshold;
    private Long totalAssignmentWaitingTime;
    private Long totalResolutionWaitingTime;
    private Long maximumAssignmentTime;

    public MonitorStatistics() {
        this.totalIncidents = 0;
        this.totalIncidentsAboveAssignmentThreshold = 0;
        this.totalIncidentsAboveResolutionThreshold = 0;
        this.totalAssignmentWaitingTime = 0L;
        this.totalResolutionWaitingTime = 0L;
        this.maximumAssignmentTime = 0L;
    }


    public Integer getTotalIncidents() {
        return totalIncidents;
    }

    public Integer getTotalIncidentsAboveAssignmentThreshold() {
        return totalIncidentsAboveAssignmentThreshold;
    }

    public Integer getTotalIncidentsAboveResolutionThreshold() {
        return totalIncidentsAboveResolutionThreshold;
    }

    public Long getTotalAssignmentWaitingTime() {
        return totalAssignmentWaitingTime;
    }

    public Long getTotalResolutionWaitingTime() {
        return totalResolutionWaitingTime;
    }

    public Long getMaximumAssignmentTime() {
        return maximumAssignmentTime;
    }

    public Double getAverageResponseTime() {
        if (totalIncidents == 0) return 0d;
        return totalAssignmentWaitingTime.doubleValue() / totalIncidents.doubleValue();
    }

    public Double getAverageResolutionTime() {
        if (totalIncidents == 0) return 0d;
        return totalResolutionWaitingTime.doubleValue() / totalIncidents.doubleValue();
    }

    public void incrementTotalIncidents() {
        this.totalIncidents++;
    }

    public void incrementTotalIncidentsAboveAssignmentThreshold() {
        this.totalIncidentsAboveAssignmentThreshold++;
    }

    public void incrementTotalIncidentsAboveResolutionThreshold() {
        this.totalIncidentsAboveResolutionThreshold++;
    }

    public void incrementTotalAssignmentWaitingTimeBy(Long amount) {
        this.totalAssignmentWaitingTime += amount;
        if (amount > maximumAssignmentTime) maximumAssignmentTime = amount;
    }

    public void incrementTotalResolutionWaitingTimeBy(Long amount) {
        this.totalResolutionWaitingTime += amount;
    }

    /**
     * Transform this MonitorStatistics object into a JSON ObjectNode representation
     * @return A JSON ObjectNode containing the state of the object
     */
    public ObjectNode toJson() {
        ObjectNode stats = Json.newObject();
        stats.put("totalIncidents", getTotalIncidents());
        stats.put("totalIncidentsAboveAssignmentThreshold", getTotalIncidentsAboveAssignmentThreshold());
        stats.put("totalIncidentsAboveResolutionThreshold", getTotalIncidentsAboveResolutionThreshold());
        stats.put("totalAssignmentWaitingTime", getTotalAssignmentWaitingTime());
        stats.put("totalResolutionWaitingTime", getTotalResolutionWaitingTime());
        stats.put("maximumAssignmentTime", getMaximumAssignmentTime());
        stats.put("averageResponseTime", getAverageResponseTime());
        stats.put("averageResolutionTime", getAverageResolutionTime());
        return stats;
    }
}
