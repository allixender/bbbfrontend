package controllers;

import static play.data.Form.form;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import models.Meeting;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import play.Logger;
import play.data.Form;
import play.libs.F.Function;
import play.libs.WS;
import play.libs.XPath;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(Secured.class)
public class Meetings extends Controller {

	private final static String bbb_salt = play.Play.application().configuration().getString("bbb.salt");
	private final static String bbb_url =  play.Play.application().configuration().getString("bbb.url");
	private final static String attendeePW = play.Play.application().configuration().getString("bbb.defaultattendeepw");
	private final static String logout_url = play.Play.application().configuration().getString("bbb.logoutredirect");

	final static Form<Meeting> meetingForm = form(Meeting.class);

    public static Result createMeetingForm() {
    	return ok(views.html.createMeeting.render(meetingForm));
    }

    public static Result createMeeting() throws ParserConfigurationException, SAXException, IOException {
    	Form<Meeting> filledForm = meetingForm.bindFromRequest();

    	// Check accept conditions
        if(!"true".equals(filledForm.field("accept").value())) {
            filledForm.reject("accept", "You must accept the terms and conditions");
        }

        // Check repeated password
        if(!filledForm.field("password").valueOr("").isEmpty()) {
            if(!filledForm.field("moderatorPW").valueOr("").equals(attendeePW)) {
                filledForm.reject("moderatorPW", "not allowed");
            }
        }

        if(filledForm.hasErrors()) {
            return badRequest(views.html.createMeeting.render(filledForm));
        } else {

            Meeting created = filledForm.get();
            final String operation = "create";

            final String urlModeratorPW = urlEncode(created.moderatorPW);
            final String urlLogout_url = urlEncode(logout_url);
            final String urlMeetingName = urlEncode(created.meetingID);
            final String urlMeetingID = urlEncode(created.meetingID);
            String def_attendeePW = null;
            if (created.attendeePW != null && created.attendeePW.length() >0 ) {
            	def_attendeePW = urlEncode(attendeePW);
            } else {
            	def_attendeePW = urlEncode(attendeePW);
            }
            final String urlAttendeePW = def_attendeePW;
            String def_record = null;
            if((created.record == null)||(!created.record)) {
            	def_record = "false";
            } else {
            	def_record = "true";
            }
            final String urlRecordMeeting = urlEncode(def_record);

            final String moderatorEmail = created.moderatorEmail;

            StringBuilder urlRequestParams = new StringBuilder();
            urlRequestParams.append("moderatorPW=" + urlModeratorPW);
            urlRequestParams.append("&logoutURL=" + urlLogout_url);
            urlRequestParams.append("&name=" + urlMeetingName);
            urlRequestParams.append("&attendeePW=" + urlAttendeePW);
            urlRequestParams.append("&meetingID=" + urlMeetingID);
            urlRequestParams.append("&record=" + urlRecordMeeting);

            String urlChksum = DigestUtils.shaHex(operation + urlRequestParams.toString() + bbb_salt);

        	String requestUri = bbb_url + operation;

			HttpClient httpclient = new DefaultHttpClient();

            HttpGet httpget = new HttpGet(requestUri + "?"
            		+ urlRequestParams.toString() + "&checksum=" + urlChksum);

            String responseBody = null;

            Logger.debug("executing request " + httpget.getURI());

            try {
                // Create a response handler
                ResponseHandler<String> responseHandler = new BasicResponseHandler();

                responseBody = httpclient.execute(httpget, responseHandler);

            } catch (IOException e) {
				// TODO Auto-generated catch block
            	e.printStackTrace();
            	return ok(views.html.index.render("Failed ", e.getMessage()));

			} finally {
                // When HttpClient instance is no longer needed,
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                httpclient.getConnectionManager().shutdown();
            }

            Logger.debug("----------------------------------------");
            Logger.debug(responseBody);
            Logger.debug("----------------------------------------");

            String bodyText = responseBody;
			Document dom = parseXml(bodyText);



			if(dom == null) {
				String okMessage = "API Fail - Expecting Xml data";
			    return ok(views.html.index.render("Failed", okMessage));
			} else {
				dom.getDocumentElement().normalize();
			    String returncode = XPath.selectText("//returncode", dom);
			    Logger.info("Root element :" + dom.getDocumentElement().getNodeName() + " " + returncode);

			    if(returncode.equalsIgnoreCase("FAILED")) {
			    	String okMessage = XPath.selectText("//messageKey", dom) + " "
    						+ XPath.selectText("//message", dom);
			    	return ok(views.html.index.render("Failed ", okMessage));

			    } else {

			    	NodeList meetingNodes = dom.getElementsByTagName("response");
			    	Meeting tm = new Meeting();

			    	for (int temp = 0; temp < meetingNodes.getLength(); temp++) {
			    		Element meeting = (Element) meetingNodes.item(temp);
			    		tm.meetingID  = meeting.getElementsByTagName("meetingID").item(0).getTextContent();
			    		tm.createTime = meeting.getElementsByTagName("createTime").item(0).getTextContent();
			    		tm.attendeePW = meeting.getElementsByTagName("attendeePW").item(0).getTextContent();
			    		tm.moderatorPW = meeting.getElementsByTagName("moderatorPW").item(0).getTextContent();
			    		tm.hasBeenForciblyEnded = meeting.getElementsByTagName("hasBeenForciblyEnded").item(0).getTextContent();
			    	}

			    	// TODO send email to moderator
			    	try {
						sendLinksEmail(tm, moderatorEmail);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

    				return redirect(routes.Joining.join(tm.meetingID, tm.moderatorPW));
			    }
			}

        }
    }

	public static Result endMeeting(String meetingID) {
        return ok(views.html.index.render("end ", meetingID));
    }

    public static Result getMeetings() {

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
    						Document dom = parseXml(bodyText);

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
    						    		meetings.add(tm);

    						    	}
    						    }
    						}

    		    	        return ok(views.html.meetings.render(meetings));
    					}
    				}
    		)
   		);
    }

    //
	// parseXml() -- return a DOM of the XML
	//
	public static Document parseXml(String xml)
			throws ParserConfigurationException, IOException, SAXException {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory
		.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
		return doc;
	 }

	//
	// urlEncode() -- URL encode the string
	//
	public static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	//
	// urlDecode() -- URL decode the string
	//
	public static String urlDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// send email with links
	private static void sendLinksEmail(Meeting tm, String moderatorEmail) throws Exception {

		final String mailhost = 	play.Play.application().configuration().getString("email.1.server");
		final String mailuser = 	play.Play.application().configuration().getString("email.1.username");
		final String mailpass = 	play.Play.application().configuration().getString("email.1.password");
		final String mailfrom = 	play.Play.application().configuration().getString("email.1.from");
		final String mailport = 	play.Play.application().configuration().getString("email.1.port");
		final String mailssl =  	play.Play.application().configuration().getString("email.1.ssl");
		final String maildebug = 	play.Play.application().configuration().getString("email.1.debug");

	    Email email = new SimpleEmail();
	    email.setHostName(mailhost);

	    if (mailssl.equalsIgnoreCase("SSL")) {
	    	email.setSSL(true);
	    	System.setProperty( "javax.net.debug", "all" );
	    	System.setProperty( "java.security.debug", "provider" );
	    } else if (mailssl.equalsIgnoreCase("TLS")) {
	    	email.setTLS(true);
	    	System.setProperty( "javax.net.debug", "all" );
	    	System.setProperty( "java.security.debug", "provider" );
	    }

	    if (maildebug.equalsIgnoreCase("true")) {
	    	email.setDebug(true);
	    } else {
	    	email.setDebug(false);
	    }
	    email.setSmtpPort(Integer.parseInt(mailport));

	    if (!mailuser.isEmpty() && !mailpass.isEmpty()) {
	    	email.setAuthenticator(new DefaultAuthenticator(mailuser,
		            mailpass));
	    }

	    email.setFrom(mailfrom);
	    email.setSubject("Video Conference " + tm.meetingID);
	    StringBuilder mailBody = new StringBuilder();

	    String moderatorMeetingUrl =
				play.Play.application().configuration().getString("application.base_url")
				+ "join/" + Meetings.urlEncode(tm.meetingID)
				+ "/" + Meetings.urlEncode(tm.moderatorPW);

	    String attendeeMeetingUrl =
				play.Play.application().configuration().getString("application.base_url")
				+ "join/" + Meetings.urlEncode(tm.meetingID)
				+ "/" + Meetings.urlEncode(tm.attendeePW);

	    mailBody.append("Hi from the Video Conference System,\n" +
	    		"the meeting has been succesfully created.\n" +
	    		"\n" +
	    		"Invitation-Link: " + attendeeMeetingUrl + " \n" +
	    		"\n" +
	    		"Moderator-Link: " + moderatorMeetingUrl + " \n" +
	    		"\n" +
	    		"");

	    email.setMsg(mailBody.toString());
	    email.addTo(moderatorEmail);

	    email.send();
	}


}
