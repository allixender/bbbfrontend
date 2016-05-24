package controllers;

import static play.data.Form.form;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import models.Meeting;
import play.Logger;
import play.data.Form;
import play.libs.WS;
import play.libs.XPath;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Joining extends Controller {

	private final static String bbb_salt = play.Play.application().configuration().getString("bbb.salt");
	private final static String bbb_url =  play.Play.application().configuration().getString("bbb.url");
	private final static String attendeePW = play.Play.application().configuration().getString("bbb.defaultattendeepw");
	private final static String logout_url = play.Play.application().configuration().getString("bbb.logoutredirect");
	
	final static Form<Invitation> joinForm = form(Invitation.class);
  
    /* join / invitation stuff
     * 
     */
    public static class Invitation {
        
        public String meetingID;
        public String password;
        public String username;
        public String attendeeMeetingUrl;
        public String moderatorMeetingUrl;
        
    }
    
    public static Result join(final String meetingID, final String checksum) {
    	
    	Invitation icard = new Invitation();
    	icard.meetingID = Meetings.urlDecode(meetingID);
    	icard.password = Meetings.urlDecode(checksum);
    	
    	String operation = "getMeetings";
    	String chksum = DigestUtils.shaHex(operation + bbb_salt);
    	String requestUri = bbb_url + operation;
    	
    	return async(
    		WS.url(requestUri).setQueryParameter("checksum", chksum)
    			.get().map(
    				new Function<WS.Response, Result>() {
    					public Result apply(WS.Response response) throws Exception {
    						
    						String bodyText = response.getBody();
    						Document dom = Meetings.parseXml(bodyText);
    						    						
    						if(dom == null) {
    							String okMessage = "API Fail - Expecting Xml data";
    						    return ok(views.html.index.render("Failed", okMessage));
    						} else {
    							dom.getDocumentElement().normalize();
    						    String returncode = XPath.selectText("//returncode", dom);
    						    Logger.info("Root element: " + dom.getDocumentElement().getNodeName() + " " + returncode);
    						    
    						    if(returncode.equalsIgnoreCase("FAILED")) {
    						    	String okMessage = XPath.selectText("//messageKey", dom) + " "
				    						+ XPath.selectText("//message", dom);
    						    	return ok(views.html.index.render("Failed ", okMessage));
    						    } else {
    						    	
    						    	NodeList meetingNodes = dom.getElementsByTagName("meeting");
    						    	
    						    	for (int temp = 0; temp < meetingNodes.getLength(); temp++) {
    						    		
    						    		Element meeting = (Element) meetingNodes.item(temp);
    						    		Meeting tm = new Meeting();
    						    		tm.meetingID  = meeting.getElementsByTagName("meetingID").item(0).getTextContent();
    						    		tm.meetingName = meeting.getElementsByTagName("meetingName").item(0).getTextContent();
    						    		
    						    		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    									Date resultdate = new Date(Long.parseLong(meeting.getElementsByTagName("createTime").item(0).getTextContent()));
    									tm.createTime = sdf.format(resultdate);
    									
    						    		tm.attendeePW = meeting.getElementsByTagName("attendeePW").item(0).getTextContent();
    						    		tm.moderatorPW = meeting.getElementsByTagName("moderatorPW").item(0).getTextContent();
    						    		tm.hasBeenForciblyEnded = meeting.getElementsByTagName("hasBeenForciblyEnded").item(0).getTextContent();
    						    		tm.running = meeting.getElementsByTagName("running").item(0).getTextContent();
    						    		
    						    		if ( (meetingID.equals(tm.meetingID)) &&
    						    				(checksum.equals(tm.attendeePW) || checksum.equals(tm.moderatorPW))) {
    						    			Invitation iCardTested = new Invitation();
    						    			iCardTested.meetingID = meetingID;
    						    			iCardTested.password = checksum;
    						    			iCardTested.attendeeMeetingUrl = 
    						    					play.Play.application().configuration().getString("application.base_url")
    						    							+ "join/" + Meetings.urlEncode(tm.meetingID)
    						    							+ "/" + Meetings.urlEncode(tm.attendeePW);
    						    			if (checksum.equals(tm.moderatorPW) ) {
    						    				iCardTested.moderatorMeetingUrl = 
        						    					play.Play.application().configuration().getString("application.base_url")
        						    							+ "join/" + Meetings.urlEncode(tm.meetingID)
        						    							+ "/" + Meetings.urlEncode(tm.moderatorPW);
    						    			} 
						    				return ok(join.render(joinForm, iCardTested));
    						        	}						    			
    						    	}
    						    }
    						    String okMessage = "Sorry, no applicable meeting found";
    						    return ok(views.html.index.render("Failed", okMessage));
    						}
    					}
    				}
    		)
   		);
    }
    
    public static Result eval() {
    	Form<Invitation> filledForm = joinForm.bindFromRequest();

    	final Invitation icard = filledForm.get();
    	
    	String operation = "getMeetings";
    	String chksum = DigestUtils.shaHex(operation + bbb_salt);
    	String requestUri = bbb_url + operation;
    	
    	return async(
    		WS.url(requestUri).setQueryParameter("checksum", chksum)
    			.get().map(
    				new Function<WS.Response, Result>() {
    					public Result apply(WS.Response response) throws Exception {
    						List<Meeting> meetings = new ArrayList<Meeting>();
    						
    						String bodyText = response.getBody();
    						Document dom = Meetings.parseXml(bodyText);
    						    						
    						if(dom == null) {
    							String okMessage = "API Fail - Expecting Xml data";
    						    return ok(views.html.index.render("Failed", okMessage));
    						} else {
    							dom.getDocumentElement().normalize();
    						    String returncode = XPath.selectText("//returncode", dom);
    						    Logger.info("Root element: " + dom.getDocumentElement().getNodeName() + " " + returncode);
    						    
    						    if(returncode.equalsIgnoreCase("FAILED")) {
    						    	String okMessage = XPath.selectText("//messageKey", dom) + " "
				    						+ XPath.selectText("//message", dom);
    						    	return ok(views.html.index.render("Failed ", okMessage));
    						    } else {
    						    	
    						    	NodeList meetingNodes = dom.getElementsByTagName("meeting");
    						    	
    						    	for (int temp = 0; temp < meetingNodes.getLength(); temp++) {
    						    		Element meeting = (Element) meetingNodes.item(temp);
    						    		Meeting tm = new Meeting();
    						    		tm.meetingID  = meeting.getElementsByTagName("meetingID").item(0).getTextContent();
    						    		tm.meetingName = meeting.getElementsByTagName("meetingName").item(0).getTextContent();
    						    		tm.createTime = meeting.getElementsByTagName("createTime").item(0).getTextContent();
    						    		tm.attendeePW = meeting.getElementsByTagName("attendeePW").item(0).getTextContent();
    						    		tm.moderatorPW = meeting.getElementsByTagName("moderatorPW").item(0).getTextContent();
    						    		tm.hasBeenForciblyEnded = meeting.getElementsByTagName("hasBeenForciblyEnded").item(0).getTextContent();
    						    		tm.running = meeting.getElementsByTagName("running").item(0).getTextContent();
    						    		meetings.add(tm);
    						    		
    						    		if ( ( icard.meetingID.equals(tm.meetingID) ) &&
    						    				( (icard.password.equals(tm.attendeePW)) || icard.password.equals(tm.moderatorPW)) ) {

    						    			final String operation = "join";
    						    			final String meetingID = Meetings.urlEncode(icard.meetingID);
    						    			final String username = Meetings.urlEncode(icard.username);
    						    			final String password = Meetings.urlEncode(icard.password);
    						    			
						    	            StringBuilder requestParams = new StringBuilder();
						    	            requestParams.append("meetingID=" + meetingID);
						    	            requestParams.append("&fullName=" + username);
						    	            requestParams.append("&password=" + password);
						    	            
						    	        	String bbb_checksum = 
						    	        			DigestUtils.shaHex(operation + requestParams.toString() + bbb_salt);
						    	        	
						    	        	String requestUri = bbb_url + operation + "?" +
						    	        			requestParams.toString() + "&checksum=" + bbb_checksum;

						    	        	return redirect(requestUri);
        						    		    
						    			} else {
						    				String okMessage = "Sorry, no applicable meeting found";
				    						return ok(views.html.index.render("Failed", okMessage));
						    			}			    			
    						    	}
    						    }
    						    String okMessage = "Sorry, no applicable meeting found";
    						    return ok(views.html.index.render("Failed", okMessage));
    						}
    					}
    				}
    		)
   		);
    }
}
