package core;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import models.Alarm;

public class LocalMonitor {

	Timer timer;
	
	HashMap<Long,TimerTask> timerTasks;

	public static long assignmentTimeTreshold = 10 * 1000; // 10 seconds
	public static long resolutionTimeTreshold = 60 * 60 * 1000; // 60 minutes
	
	public LocalMonitor() {
		super();
		timer = new Timer("LocalMonitor");
		timerTasks = new HashMap<Long,TimerTask>();
	} 
	
	public void registerNewAlert(long alertId){
		AssignmentReminderTask at = new AssignmentReminderTask(alertId);
		timerTasks.put(alertId, at);
		timer.schedule(at, assignmentTimeTreshold);
	}
	
	
	public void registerAssignment(long alertId){
		AssignmentReminderTask t = (AssignmentReminderTask) timerTasks.get(alertId);
		if(null != t){
			t.cancel();
			timerTasks.remove(alertId);
		}
		
	}
	

	// TODO: add functions for dispatching and closing
	
	public class AssignmentReminderTask extends TimerTask{
		
		long id;
		
		public AssignmentReminderTask(long id){
			this.id= id;
		}
		
		@Override
		public void run() {
			// TODO call Websocket
			timerTasks.remove(this.id);
			Alarm expiredAlarm =Global.alarmList.list.get(this.id);
			expiredAlarm.expired = true;
			MyWebSocketManager.addTimeIconToAlarm(id);
			
			
		}
	}


	
}
