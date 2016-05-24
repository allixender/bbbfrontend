package controllers;

import static play.data.Form.form;
import models.User;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.login;

public class Application extends Controller {
	
// -- Authentication
    
    public static class Login {
        
        public String username;
        public String password;
        
        public String validate() {
            if(User.authenticate(username, password) == null) {
                return "Invalid user or password";
            }
            return null;
        }
        
    }

    public static Result index() {
    	// return redirect(routes.Meetings.getMeetings());
    	
    	String welcomeMessage = play.Play.application().configuration().getString("application.welcome");
    	return ok(views.html.index.render("BigBlueButton", welcomeMessage));
    	
    }
    
    public static Result help() {
        return ok(views.html.help.render("Help", "Documentation"));
    }
    
    /**
     * Login page.
     */
    public static Result login() {
        return ok(
            login.render(form(Login.class))
        );
    }
    
    /**
     * Handle login form submission.
     */
    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if(loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            session("username", loginForm.get().username);
            return redirect(
                routes.Meetings.getMeetings()
            );
        }
    }

    /**
     * Logout and clean the session.
     */
    public static Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        return redirect(
            routes.Application.login()
        );
    }
    

}
