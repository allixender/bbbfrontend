import play.*;
import play.mvc.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {
    
	protected final static String TEST1 = "TEST1";
	
    public void onStart(Application app) {
        Logger.info("app start");
    }
    
    public Result onHandlerNotFound(String uri) {
      return notFound(
    		  views.html.index.render("Handler not found", "Oops, this should not happen...")
      );
    }
    
    public Result onBadRequest(String uri, String error) {
      return badRequest("Don't try to hack the URI!");
    }
}