package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import models.Alarm;


public class OpenAlarmList {

	public LinkedHashMap<Long,Alarm> list;
	
	public OpenAlarmList() {
		super();
		list = new LinkedHashMap<Long,Alarm>();
	}
	
	// returns a copy of the list of open alarms
	public List<Alarm> getAlarmList(){
    	
    	return new ArrayList<Alarm>(list.values());
	}
	
	
}
