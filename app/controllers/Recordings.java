package controllers;

import static play.data.Form.form;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Recording;
import views.html.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import play.Logger;
import play.data.Form;
import play.libs.WS;
import play.libs.XPath;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(Secured.class)
public class Recordings extends Controller {
	
	private final static String bbb_salt = play.Play.application().configuration().getString("bbb.salt");
	private final static String bbb_url =  play.Play.application().configuration().getString("bbb.url");
	private final static String logout_url = play.Play.application().configuration().getString("bbb.logoutredirect");
	
	public static Result publish(String recordID) {
		
		final String operation = "publishRecordings";
        
        final String urlRecordID = Meetings.urlEncode(recordID);
        final String urlPublish = Meetings.urlEncode("true");
                
        StringBuilder urlRequestParams = new StringBuilder();
        urlRequestParams.append("recordID=" + urlRecordID);
        urlRequestParams.append("&publish=" + urlPublish);
        
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
		
        return redirect(routes.Recordings.getRecordings());
    }
	
	public static Result unpublish(String recordID) {
		
        final String operation = "publishRecordings";
        
        final String urlRecordID = Meetings.urlEncode(recordID);
        final String urlPublish = Meetings.urlEncode("false");
                
        StringBuilder urlRequestParams = new StringBuilder();
        urlRequestParams.append("recordID=" + urlRecordID);
        urlRequestParams.append("&publish=" + urlPublish);
        
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
		
        return redirect(routes.Recordings.getRecordings());
    }

	public static Result delete(String recordID) {
		
		final String operation = "deleteRecordings";
        
        final String urlRecordID = Meetings.urlEncode(recordID);
        final String urlPublish = Meetings.urlEncode("false");
                
        StringBuilder urlRequestParams = new StringBuilder();
        urlRequestParams.append("recordID=" + urlRecordID);
        
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
		
        return redirect(routes.Recordings.getRecordings());
    }
	
    public static Result getRecordings() {
    	
    	String operation = "getRecordings";
    	String chksum = DigestUtils.shaHex(operation + bbb_salt);
    	String requestUri = bbb_url + operation;
    	
    	return async(
    		WS.url(requestUri).setQueryParameter("checksum", chksum)
    			.get().map(
    				new Function<WS.Response, Result>() {
    					public Result apply(WS.Response response) throws Exception {
    						List<Recording> recordings = new ArrayList<Recording>();
    						
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
    						    	
    						    	NodeList recordingsNodes = dom.getElementsByTagName("recording");
    						    	
    						    	for (int temp = 0; temp < recordingsNodes.getLength(); temp++) {
    						    		Element recording = (Element) recordingsNodes.item(temp);
    						    		Recording rec = new Recording();
    						    		rec.meetingID  = recording.getElementsByTagName("meetingID").item(0).getTextContent();
    						    		rec.recordID = recording.getElementsByTagName("recordID").item(0).getTextContent();
    						    		
    						    		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    									Date startDate = new Date(Long.parseLong(recording.getElementsByTagName("startTime").item(0).getTextContent()));
    									rec.startTime = sdf.format(startDate);
    									Date endDate = new Date(Long.parseLong(recording.getElementsByTagName("endTime").item(0).getTextContent()));
    									rec.endTime = sdf.format(endDate);
    									
    						    		if (!recording.getElementsByTagName("published").item(0).getTextContent().isEmpty() && 
    						    				recording.getElementsByTagName("published").item(0).getTextContent().equalsIgnoreCase("true")) {
    						    			rec.published = true;
    						    		} else {
    						    			rec.published = false;
    						    		}
    						    		recordings.add(rec);
						    			
    						    	}
    						    }
    						}
    						
    		    	        return ok(views.html.recordings.render(recordings));
    					}
    				}
    		)
   		);
    }

}
