package API;

import java.util.ArrayList;

/**
 * RankedTweetList holds an ArrayList of Tweets in the order that Lucene outputs
 * 
 * @author Dr. Sharon Small & Carl Tompkins v1.0
 * @edited Lauren Mathews v2.0
 * 
 * @version 7/5/2011 v1.0
 * @version 6/9/14 v2.0
 */
public class RankedTweetList {
	private ArrayList<Tweet> tweets;

	public RankedTweetList(ArrayList<Tweet> tweets) {
		this.tweets = tweets;
	}

	public ArrayList<Tweet> getRankedList() {
		return tweets;
	}

	/**
	 * getTweet() returns a the tweet at the specific index
	 * 
	 * @param tweetNum
	 *            the index that the tweet resides at (0 -> size of list)
	 * @return Tweet located at tweetNum
	 */
	public Tweet getTweet(int tweetNum) {
		return tweets.get(tweetNum);
	}

	public int size() {
		return tweets.size();
	}

	public String toString() {
		return "RankedTweetList [tweets=" + tweets + ", numTweets=" + size()
				+ "]";
	}

	public void remove(int i) {
		tweets.remove(i);
	}
}