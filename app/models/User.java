package models;

import java.util.*;

import play.db.ebean.Model;


public class User extends Model {

	// generated
	private static final long serialVersionUID = -2368628004394533060L;

	public String username;
    public String password;

    // -- Queries


    /**
     * Retrieve all users.
     */
    public static List<User> findAll() {
    	User bbb = new User();
		bbb.username = "bbb";
		bbb.password = "bbbadmin";
		List<User> all = new ArrayList<User>();
		all.add(bbb);
        return all;
    }

    /**
     * Retrieve a User from email.
     */
    public static User findByEmail(String email) {
    	User bbb = new User();
		bbb.username = "bbb";
		bbb.password = "bbbadmin";
		return bbb;
    }

    /**
     * Authenticate a User.
     */
    public static User authenticate(String username, String password) {
    	if (username.equals("bbb") && password.equals("bbbadmin")) {
    		User bbb = new User();
    		bbb.username = "bbb";
    		bbb.password = "bbbadmin";
    		return bbb;
    	}
        return null;
    }

    // --

    public String toString() {
        return "User(" + username + ")";
    }

}
