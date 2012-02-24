package controllers;

import java.util.List;

import models.Tweet;
import models.User;
import play.mvc.Controller;

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
	
}
