package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import models.Alarm;


public class OpenAlarmList {

	public HashMap<Long,Alarm> list;
	
	public OpenAlarmList() {
		super();
		list = new HashMap<Long,Alarm>();
	}
	
	// returns a copy of the list of open alarms
	public List<Alarm> getAlarmList(){
    	
    	return new ArrayList<Alarm>(list.values());
	}
	
	
}
