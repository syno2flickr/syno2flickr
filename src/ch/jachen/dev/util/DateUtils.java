package ch.jachen.dev.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtils {
	
	public static String getFormatedTimeElapsed(Date start, Date end){
		long elapsed = (end.getTime() - start.getTime()) / 1000;
		StringBuilder res = new StringBuilder();
		long days =  TimeUnit.SECONDS.toDays(elapsed);
	    long hours = TimeUnit.SECONDS.toHours(elapsed) -
	                 TimeUnit.DAYS.toHours(days);
	    long minutes = TimeUnit.SECONDS.toMinutes(elapsed) -
	                  TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(elapsed));
	    long seconds = TimeUnit.SECONDS.toSeconds(elapsed) -
	                  TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(elapsed));
		if(days>0)
			res.append(days+" day"+(days>1?"s":"")+", ");
		if(hours>0 || res.length()>0)
			res.append(hours+" hour"+(hours>1?"s":"")+", ");
		if(minutes>0 || res.length()>0)
			res.append(minutes+" minute"+(minutes>1?"s":"")+", ");
		if(seconds>0 || res.length()>0)
			res.append(seconds+" second"+(seconds>1?"s":""));	
		return res.toString();
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		Date start = new Date(2012, 1, 1, 0,0,0);
		Date end = new Date(2013, 1, 1, 6,2,59);
		System.out.println(getFormatedTimeElapsed(start, end));
	}
}
