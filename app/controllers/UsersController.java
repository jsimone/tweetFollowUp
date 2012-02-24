package controllers;

import models.Tweet;
import models.User;
import play.mvc.Controller;

public class UsersController extends Controller {

	public static void index(Long id) {
		User user = User.findById(id);
		//Tweet.find(", params)
		
		render(user);
	}
}
