package models;

import java.util.Date;
import java.util.List;

import javax.validation.*;

import play.data.validation.Constraints.*;

public class Meeting {
	
	@Required
	@MinLength(value = 4)
	public String meetingID;
	
	public String meetingName;
	
	public String welcome;
	
	public String createTime;
	
	public Date startTime;
	
	@Required
	@MinLength(value = 6)
	public String moderatorPW;
	
	@Required
	@Email
	public String moderatorEmail;
	
	public String attendeePW;
	
	public Long participantCount;
	
	public Long moderatorCount;
	
	public List<String> attendees;
	
	public String running;
	
	public String hasBeenForciblyEnded;
	
	public Boolean accept;
	
	public Boolean record;
	
	
}
