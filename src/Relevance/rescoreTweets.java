package Relevance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import nistEvaluation.PrecisionScore;

import API.LuceneQuery;
import API.RankedTweetList;
import API.Tweet;
import LinkCrawling.URLListGen;
import Main.Module;
import Query_ExpansionInternal.CommonWords;

/**
 * Working with classes Tweet and RankedTweetList, it "ups" the score of the
 * tweet if it has more information then the query, based on percentage.
 * 
 * @author Lauren Mathews v1.0
 * @edited Lauren Mathews v1.5
 * @version 7/6/12 v1.0
 * @version 7/2/14 v1.5
 */
public class rescoreTweets implements Module {

	private static final float DEFAULT_PERCENTAGE_BONUS = 0.2f, DEFAULT_RETWEET_BONUS = 0.1f;

	/**
	 * Used to run "rescoreTweets" of Internal Query Expansion collection.
	 * rescoreTweets.java used to go through each tweet and up the score
	 * directly
	 */
	public ArrayList<ArrayList<RankedTweetList>> getResults(
			ArrayList<LuceneQuery> queList,
			ArrayList<ArrayList<RankedTweetList>> rankList) {

		if (queList.isEmpty() || queList == null || queList.size() < 49) {
			System.err
					.println("In QueryExpansionInternal.java (rescoreTweets). queList (queries - topics) is empty, null or wrong amount.");
			System.exit(0);
		}

		if (rankList.isEmpty() || rankList == null) {
			System.err
					.println("In QueryExpansionInternal.java (rescoreTweets). rankList is empty or null.");
			System.exit(0);
		}

		HashMap<String, Integer> retweets = new HashMap<String, Integer>();
		int count = 0;
		int mean = 0;

		HashMap<String, Double> queryThreshold = calculateQueryThreshold(
				rankList, queList);

		// goes through the rankList (all tweets for all topics
		for (int i = 0; i < rankList.size(); i++) {

			// gets ranked list of tweets for each topic we want to print
			for (int k = 0; k < rankList.get(i).size(); k++) {
				// the list of tweets for that topic
				RankedTweetList rtl = rankList.get(i).get(k);

				count = 0;
				mean = 0;

				// updates score (directly) with list and query (topic)
				rescore(rtl,
						queList.get(i).getQuery(),
						queryThreshold.get(rtl.getRankedList().get(k)
								.getTopicNum()));
				
				for (int j = 0; j < rtl.size(); j++) {
					Tweet curTweet = rtl.getRankedList().get(j);
					int retweet = curTweet.getRe_count();

					if (retweet > 1) {
						mean += retweet;
						count++;
					}
				}

				if (mean != 0 && count != 0) {
					mean = mean / count;
				} else {
					mean = 0;
				}

				retweets.put(rtl.getRankedList().get(0).getTopicNum(), mean);
			}
		}

		calculateReTweets(retweets, rankList);

		return rankList;
	}

	/**
	 * Goes through and ups those tweets that are retweeted more then their
	 * topic's threshold.
	 * 
	 * @param retweets
	 *            Each of the topic numbers and their mean retweet number
	 * @param rankList
	 *            The list of tweets for each topic
	 */
	public void calculateReTweets(HashMap<String, Integer> retweets,
			ArrayList<ArrayList<RankedTweetList>> rankList) {
		int THRESHOLD = 0;

		for (int i = 0; i < rankList.size(); i++) {

			// gets ranked list of tweets for each topic we want to print
			for (int k = 0; k < rankList.get(i).size(); k++) {
				// the list of tweets for that topic
				RankedTweetList rtl = rankList.get(i).get(k);

				THRESHOLD = retweets.get(rtl.getRankedList().get(0)
						.getTopicNum());

				for (int j = 0; j < rtl.size(); j++) {
					Tweet curTweet = rtl.getRankedList().get(j);
					int retweet = curTweet.getRe_count();
					float score = curTweet.getScore();

					if (retweet > THRESHOLD) {
						curTweet.setScore(score + DEFAULT_RETWEET_BONUS);
					}else{
						curTweet.setScore(score - DEFAULT_RETWEET_BONUS);
					}
				}
			}
		}
	}

	/**
	 * Calculates how much of the query is in the tweet
	 */
	private HashMap<String, Double> calculateQueryThreshold(
			ArrayList<ArrayList<RankedTweetList>> rankList,
			ArrayList<LuceneQuery> queList) {
		
		HashMap<String, Double> queryThreshold = new HashMap<String, Double>();
		
		HashMap<String, HashSet<String>> queryWords = new HashMap<String, HashSet<String>>();
		HashSet<String> queryWord = new HashSet<String>();

		for (int j = 0; j < queList.size(); j++) {
			String query = queList.get(j).getQuery();

			String newQuery = query.toLowerCase().trim();
			newQuery = CommonWords.removesAllPunct(newQuery);
			
			StringTokenizer sTQ = new StringTokenizer(newQuery, " ");
			queryWord = new HashSet<String>();

			while (sTQ.hasMoreTokens()) {

				String lineInput = sTQ.nextToken();

				if (!queryWord.contains(lineInput)) {
					queryWord.add(lineInput);
				}
			}
			queryWords.put("MB" + queList.get(j).getQueryNum(), queryWord);
		}
		
		double mean = 0.0;
		double number = 0.0;

		for (int i = 0; i < rankList.size(); i++) {
			// gets ranked list of tweets for each topic we want to print
			
			for (int k = 0; k < rankList.get(i).size(); k++) {
				// the list of tweets for that topic
				RankedTweetList rtl = rankList.get(i).get(k);
				
				mean = 0;
				number = 0;

				for (int j = 0; j < rtl.size(); j++) {
					Tweet curTweet = rtl.getRankedList().get(j);
					String tweet = curTweet.getStatus();

					// Fixes the tweet, for later use (to find it the whole
					// tweet is just a url or not)
					tweet = tweet.trim();

					// Creates a temp tweet, tweetCheck, that will be the tweet
					// we use to compare with
					String tweetCheck = tweet;

					tweetCheck = URLListGen.removeURLs(tweetCheck);

					// Sets both tweet(Check) and query to lowercase and trims
					// for comparison purposes below
					tweetCheck = tweetCheck.toLowerCase().trim();
					
					if(tweetCheck.isEmpty()){
						continue;
					}

					// Goes through the tweet and removes all non
					// alphabet/number/space values
					tweetCheck = CommonWords.removesAllPunct(tweetCheck);

					// A counter the counts how many of the query's words are in
					// the tweet
					double count = 0.0;
					double amount = 0.0;
					
					queryWord = queryWords.get(curTweet.getTopicNum());
					
					// While loop: Goes through every tweet word and checks to
					// see if it
					// matches a query word
					StringTokenizer sTT = new StringTokenizer(tweetCheck, " ");
					while (sTT.hasMoreTokens()) {
						String inputLine = sTT.nextToken();
						
						if (queryWord.contains(inputLine)) {
							count++;
						}

						amount++;
					}
					
					if(amount <= 0){
						continue;
					}

					// Creates a percent of how much of the tweet is the query
					double percent = (count / amount) * 100;

					mean += PrecisionScore.round(new BigDecimal(percent, MathContext.DECIMAL64), 2, true).doubleValue();
					
					number++;
				}
				
				if (mean != 0 && number != 0) {
					mean = mean / number;
				} else {
					mean = 0;
				}

				queryThreshold.put(rtl.getRankedList().get(0).getTopicNum(), mean);
			}
		}
		
		return queryThreshold;
	}

	/**
	 * Goes through the topic's list of tweets. Updates the score of the tweet
	 * based off of newScore.
	 * 
	 * @param list
	 *            the current topic's list of tweets
	 * @param query
	 *            the current topic
	 */
	public void rescore(RankedTweetList list, String query, double threshold) {
		// Reads in tweet, tweetID, score and query throughout the list
		for (int i = 0; i < list.size(); i++) {
			Tweet currentTweet = list.getTweet(i);
			String tweetContent = currentTweet.getStatus();
			float score = currentTweet.getScore();

			// Calls below method to adjust score if needed
			float newScore = calQueryPerc(tweetContent, score, query, threshold);

			// directly changes the score
			currentTweet.setScore(newScore);
		}
	}

	/**
	 * Re-calculates the score based on the amount of topic words contained in
	 * the tweet.
	 * 
	 * @param tweet
	 *            the tweet being re-calculated
	 * @param curScore
	 *            the original score of the tweet
	 * @param query
	 *            the topic the tweet belongs to
	 * @return a new score to be added to that tweet
	 */
	public float calQueryPerc(String tweet, float curScore, String query,
			double threshold) {

		HashSet<String> queryWords = new HashSet<String>();

		// Fixes the tweet, for later use (to find it the whole tweet is just a
		// url or not)
		tweet = tweet.trim();

		// Creates a temp tweet, tweetCheck, that will be the tweet we use to
		// compare with
		String tweetCheck = tweet;

		tweetCheck = URLListGen.removeURLs(tweetCheck);

		if (tweetCheck.isEmpty()) {
			return curScore;
		}

		// Sets both tweet(Check) and query to lowercase and trims for
		// comparison purposes below
		tweetCheck = tweetCheck.toLowerCase().trim();
		String newQuery = query.toLowerCase().trim();

		// Goes through the tweet and removes all non alphabet/number/space
		// values
		tweetCheck = CommonWords.removesAllPunct(tweetCheck);
		newQuery = CommonWords.removesAllPunct(newQuery);

		StringTokenizer sTQ = new StringTokenizer(newQuery, " ");

		double query_amt = 0;
		
		while (sTQ.hasMoreTokens()) {

			String lineInput = sTQ.nextToken();

			if (!queryWords.contains(lineInput)) {
				queryWords.add(lineInput);
				query_amt++;
			}
		}

		// A counter the counts how many of the query's words are in the tweet
		double count = 0.0;
		double amount = 0.0;

		// While loop: Goes through every tweet word and checks to see if it
		// matches a query word
		StringTokenizer sTT = new StringTokenizer(tweetCheck, " ");
		while (sTT.hasMoreTokens()) {
			String inputLine = sTT.nextToken();

			if (queryWords.contains(inputLine)) {
				count++;
			}

			amount++;
		}

		// Creates a percent of how much of the tweet is the query
		double percent = (count / amount) * 100;

		// If 30% or more of the tweetCheck is made up of query, increases
		if (percent > threshold && (count >= (query_amt/1.5))) {
			return (float) (curScore + DEFAULT_PERCENTAGE_BONUS);
		} else {
			return (float) (curScore - DEFAULT_PERCENTAGE_BONUS);
		}
	}
}