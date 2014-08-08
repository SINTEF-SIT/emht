package core;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import models.Alarm;

public class LocalMonitor {

	Timer timer;
	
	HashMap<Long,TimerTask> timerTasks;

	public static long assignmentTimeTreshold = 30 * 1000; // 30 seconds
	public static long resolutionTimeTreshold = 120 * 60 * 1000; // 120 minutes
	
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
	
	public void registerFollowUp(long alertId){
		// since one can save a case to followup multiple times, we have to ensure that we just create the
		// monitoring task in the first time it is set to followup
		if( timerTasks.get(alertId) == null){
			AssignmentReminderTask t = new AssignmentReminderTask(alertId);
			timerTasks.put(alertId, t);
			timer.schedule(t, resolutionTimeTreshold);
		}
	}
	
	
	public void registerClosing(long alertId){
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
