package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import models.Tweet;
import models.User;
import play.db.DB;
import play.libs.OpenID;
import play.libs.OpenID.UserInfo;
import play.mvc.Before;
import play.mvc.Controller;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TweetController extends Controller{

	@Before(unless={"unauthorized", "authenticate", "updateTweets"})
	static void checkAuthenticated() {
	    if(!session.contains("user")) {
	        authenticate();
	    }
	}
	     
	public static void unauthorized() {
	    render();
	}

	private static long addUser(String name, String email) {
		List<User> users = User.find("byEmail", email).fetch();
		if(users.size() == 0) {
			User user = new User();
			user.setName(name);
			user.setEmail(email);
			user.save();
			return user.getId();
		} else {
			return users.get(0).getId();
		}
	}
	    
	public static void authenticate() {
	    if(OpenID.isAuthenticationResponse()) {
	        UserInfo verifiedUser = OpenID.getVerifiedID();
	        if(verifiedUser == null) {
	            flash.error("Oops. Authentication has failed. Could be an issue with Google");
	            unauthorized();
	        } 
	        
	        String email = verifiedUser.extensions.get("email");
	        String firstName = verifiedUser.extensions.get("firstName");
	        String lastName = verifiedUser.extensions.get("lastName");
	        if(email != null) {
	        	session.put("user", verifiedUser.id);
	        	session.put("email", email);
	        	session.put("name", firstName);
	        	session.put("userId", addUser(firstName + " " + lastName, email));
	        	index();	        	
	        }
	    } else {
	    	OpenID.id("https://www.google.com/accounts/o8/id")
	    		.required("email", "http://axschema.org/contact/email")
	    		.required("firstName", "http://axschema.org/namePerson/first")
	    		.required("lastName", "http://axschema.org/namePerson/last").verify();
	    }
	}
	
	public static void index() {
		List<Tweet> tweets = Tweet.findAll();
		String userName = session.get("name");
		String userId = session.get("userId");
		render(tweets, userName, userId);
	}
	
	public static void followUpForm(Long id) {
		Tweet tweet = Tweet.findById(id);
		List<User> users = User.findAll();
		render(tweet, users);
	}
	
	public static void addFollowUp(Long tweetId, Long userId) {
		Tweet tweet = Tweet.findById(tweetId);
		User user = User.findById(userId);
		tweet.setUser(user);
		tweet.save();
		index();
	}
	
	private static long getLatestStatusId() {
		Connection conn = DB.getConnection();
		
		try {
			// Get a statement from the connection
			Statement stmt = conn.createStatement() ;
			// Execute the query
			ResultSet rs = stmt.executeQuery("SELECT max(statusId) from tweet");
			
			if(rs.next()) {
				return rs.getLong(1);
			} else {
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		} finally {
			DB.close();
		}
	}
	
	public static void updateTweets() {
		Twitter twitter = new TwitterFactory().getInstance();
		Query query = new Query();
		query.setQuery("Heroku");
		long sinceId = getLatestStatusId();
		query.setSinceId(sinceId);
		System.out.println("Updating tweets since: " + sinceId);
		try {
			QueryResult result = twitter.search(query);
			List<twitter4j.Tweet> tweets = result.getTweets();
			System.out.println(tweets.size() + " tweets found");
			for(twitter4j.Tweet queriedTweet : tweets) {
				Tweet tweet = new Tweet();
				tweet.setAuthor(queriedTweet.getFromUser());
				tweet.setBody(queriedTweet.getText());
				tweet.setCreatedAt(queriedTweet.getCreatedAt());
				tweet.setStatusId(queriedTweet.getId());
				tweet.save();
			}
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}
	
}
