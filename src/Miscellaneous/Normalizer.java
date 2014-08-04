package Miscellaneous;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import API.RankedTweetList;
import API.Tweet;

/**
 * This class normalizes a set of ranked tweet list so there scores are between
 * 0 and 1(inclusive). This is done by dividing the highest score for each tweet
 * with each current tweet.
 * 
 * @author Karl Appel v1.0
 * @author Lauren Mathews v1.5
 * @version 6/2012
 * @versio 7/10/2014 v1.5
 */
public class Normalizer {

	/**
	 * Method that initiates the normalization of the tweets
	 * 
	 * @param rankedTweetLists
	 *            lists before normalization
	 * @return rankedTweetLists lists after normalization
	 */
	public static ArrayList<RankedTweetList> normalize(
			ArrayList<RankedTweetList> rankedTweetLists) {

		int rank = 1;

		// each list will be normalized individually
		for (int i = 0; i < rankedTweetLists.size(); i++) {
			if (rankedTweetLists.get(i) == null) {
				continue;
			}

			RankedTweetList currentList = rankedTweetLists.get(i);

			// gets the list of those tweets and then sorts them
			List<Tweet> sublist = currentList.getRankedList().subList(0,
					currentList.size());

			Collections.sort(sublist,
					Collections.reverseOrder(new TweetScoreComparator()));

			// gets the first tweet score and uses that as
			// a constant to divide with the other scores
			Tweet firstTweet = currentList.getTweet(0);
			float normalizedConstant = firstTweet.getScore();

			float orgScoreConstant = firstTweet.getOrgScore();

			float newOrgScore = firstTweet.getOrgScore() / orgScoreConstant;

			float temp = firstTweet.getOrgScore();
			while (newOrgScore > 1) {
				temp--;
				newOrgScore = temp / orgScoreConstant;
			}

			if (newOrgScore < 0) {
				newOrgScore = (float) 0.0;
			}

			firstTweet.setOrgScore(newOrgScore);

			// sets the score to 1.0
			firstTweet.setScore((float) 1.0);
			firstTweet.setRank(rank);
			rank++;

			// for each tweet the tweet scores are normalized as described in
			// class description
			for (int j = 1; j < currentList.size(); j++) {
				Tweet currentTweet = currentList.getTweet(j);
				float newScore = currentTweet.getScore() / normalizedConstant;
				newOrgScore = currentTweet.getOrgScore() / orgScoreConstant;

				temp = firstTweet.getScore();
				while (newScore > 1) {
					temp--;
					newScore = (currentTweet.getScore() - 1)
							/ normalizedConstant;
				}

				temp = currentTweet.getOrgScore();
				while (newOrgScore > 1) {
					temp--;
					newOrgScore = temp / orgScoreConstant;
				}

				if (newScore < 0) {
					newScore = (float) 0.0;
				}

				if (newOrgScore < 0) {
					newOrgScore = (float) 0.0;
				}

				currentTweet.setOrgScore(newOrgScore);
				currentTweet.setScore(newScore);
				currentTweet.setRank(rank);
				rank++;
			}
		}

		// returns rankedtweetlist with normalized scores
		return rankedTweetLists;
	}

	/**
	 * Re-ranks tweets after they've been rescored.
	 * 
	 * @param tweets
	 *            The rtl of the topic
	 * @return New rtl for topic
	 */
	public static ArrayList<RankedTweetList> rerank(
			ArrayList<RankedTweetList> tweets) {

		int rank = 1;

		// each list will be normalized individually
		for (int i = 0; i < tweets.size(); i++) {
			if (tweets.get(i) == null) {
				continue;
			}

			RankedTweetList currentList = tweets.get(i);

			// gets the list of those tweets and then sorts them
			List<Tweet> sublist = currentList.getRankedList().subList(0,
					currentList.size());

			Collections.sort(sublist,
					Collections.reverseOrder(new TweetScoreComparator()));

			// for each tweet the tweet scores are normalized as described in
			// class description
			for (int j = 1; j < currentList.size(); j++) {
				Tweet currentTweet = currentList.getTweet(j);

				currentTweet.setRank(rank);
				rank++;
			}
		}

		// returns rankedtweetlist with normalized scores
		return tweets;
	}
}