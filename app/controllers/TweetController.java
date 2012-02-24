package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import models.Tweet;
import models.User;
import play.db.DB;
import play.mvc.Controller;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TweetController extends Controller{

	public static void index() {
		List<Tweet> tweets = Tweet.findAll();
		render(tweets);
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
	
	private static Date getLatestTweet() {
		Connection conn = DB.getConnection();
		
		try {
			// Get a statement from the connection
			Statement stmt = conn.createStatement() ;
			// Execute the query
			ResultSet rs = stmt.executeQuery("SELECT max(createdAt) from tweet") ;
			
			rs.next();
			return new Date(rs.getDate(1).getTime());
		} catch (SQLException e) {
			e.printStackTrace();
			return new Date(0);
		} finally {
			DB.close();
		}
	}
	
	private static long getLatestStatusId() {
		Connection conn = DB.getConnection();
		
		try {
			// Get a statement from the connection
			Statement stmt = conn.createStatement() ;
			// Execute the query
			ResultSet rs = stmt.executeQuery("SELECT max(statusId) from tweet") ;
			
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
