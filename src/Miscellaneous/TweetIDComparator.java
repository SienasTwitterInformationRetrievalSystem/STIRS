package Miscellaneous;

import java.math.BigInteger;
import java.util.Comparator;

import API.Tweet;

/**
 * A TweetIDComparator compares Tweets based on tweetID. Tweets with a lower
 * tweetID come before Tweets with a higher tweetID.
 * 
 * To sort a collection of Tweets on tweetID:
 * <code>Collections.sort(tweets, new TweetIDComparator());</code>
 * 
 * @author David Purcell
 */
public class TweetIDComparator implements Comparator<Tweet> {

	public int compare(Tweet tweetA, Tweet tweetB) {
		long tweetAID = tweetA.getTweetID();
		long tweetBID = tweetB.getTweetID();
		return new BigInteger(String.valueOf(tweetAID)).compareTo(new BigInteger(String.valueOf(tweetBID)));
	}
}