package LinkCrawling;

import java.util.ArrayList;
import java.util.HashMap;

import API.RankedTweetList;
import API.Tweet;
import API.LuceneQuery;
import Main.Module;

/**
 * The RankedJoin class combines two ranked lists of tweets into a single ranked
 * list.
 * 
 * Version 3.0: API already does a baseline run. This updates those scores with
 * new scores based on link relevance (which is done by running Lucene with the
 * link data based on that link's query).
 * 
 * @author David Purcell v1.0
 * @edited Karl Appel v2.0
 * @edited Lauren Mathews v3.0
 * @version 6/2011 v1.0
 * @version 6/2012 v2.0
 * @version 7/10/2014 v3.0
 */
public final class RankedJoin implements Module {
	// The penalty applied to tweet IDs that do not have a high ranking URL.
	private static final float DEFAULT_NON_URL_PENALTY = -0.2f,
			DEFAULT_RELIABLE_WEBSITE_BONUS = 0.2f,
			DEFAULT_URLBOOST_BONUS = 0.1f;

	private int hitNum;
	private String harddrive;

	/**
	 * Create a RankedJoin object by specifying the path to the URL content
	 * index.
	 * 
	 * @param urlIndex
	 *            The path to the URL index.
	 * @see #getResults(java.util.List, java.util.List)
	 */
	public RankedJoin(String urlIndex, boolean querySplit) {
		this.harddrive = urlIndex;
		this.hitNum = 30;

		System.err
				.println("(In/For RankedJoin.java, for Link Crawling External Module) Do you need to update the link content?");
	}

	/**
	 * Goes through the url lists, compares to the regular tweets and adjusts
	 * the scores of those whom have urls.
	 */
	public ArrayList<ArrayList<RankedTweetList>> getResults(
			ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists) {

		System.err
				.println("UrlContentRetrieval.java is a stand-alone and SHOULD be completed before this is run.");

		// will contain, for each query, the top 30 urls with scores
		ArrayList<RankedTweetList> rankedUrlLists = TwitterIndexerUrls.createRankedUrls(queries, rankedTweetLists, hitNum, harddrive);

		/*for(int i = 0; i < rankedUrlLists.size(); i++){
			System.out.println("Topic: " + rankedUrlLists.get(i).getRankedList().get(0).getTopicNum());
			for(int m = 0; m < rankedUrlLists.get(i).size();m++){
				System.out.println("TweetID: " + rankedUrlLists.get(i).getRankedList().get(m).getTweetID());
			}
		}*/
		
		// will contain the new tweet scores
		ArrayList<ArrayList<RankedTweetList>> results = new ArrayList<ArrayList<RankedTweetList>>();

		System.out.println("Main RankedJoin... (editing scores)");

		// goes through each query and updates the tweets within them
		for (int i = 0; i < queries.size(); i++) {
			// will be 50 of these (one for each topic)
			ArrayList<RankedTweetList> rtl = new ArrayList<RankedTweetList>();

			if (rankedUrlLists.get(i).size() <= 0
					|| rankedTweetLists.get(i).get(0).size() <= 0) {
				String message = "";

				if (rankedTweetLists.get(i).get(0).size() <= 0) {
					message = "no tweets";
				} else if (rankedUrlLists.get(i).size() <= 0) {
					message = "no urls";
				}

				System.err
						.println("In RankedJoin.java (in getResults). During a real run, there shouldn't be "
								+ message + " for query " + (i + 1) + ".");
				System.exit(0);
			}

			// ensure original tweet status
			RankedTweetList joinedList = rankedJoin(rankedTweetLists.get(i)
					.get(0), rankedUrlLists.get(i));

			rtl.add(joinedList);
			results.add(rtl);
		}

		System.out.println("Finished main RankedJoin.");

		return results;
	}

	/**
	 * Adjusts score for those with urls
	 * 
	 * @param tweetList
	 *            A list of tweets from a tweet content query.
	 * @param urlList
	 *            A list of tweets from a URL content query.
	 * @return A ranked list that is the combination of the input lists.
	 */
	private RankedTweetList rankedJoin(RankedTweetList tweetList,
			RankedTweetList urlList) {
		// helps with removing duplicates from urlLists
		// possibility of same tweetID, multiple URLs, getting into urlList
		// uses urlTweetIDs to find and upgrade score
		HashMap<Long, Integer> urlTweetIDs = new HashMap<Long, Integer>();

		for (int k = 0; k < urlList.size(); k++) {
			Long tweetID = urlList.getRankedList().get(k).getTweetID();

			if (urlTweetIDs.containsKey(tweetID)) {
				int amount = urlTweetIDs.get(tweetID);
				amount++;
				urlTweetIDs.remove(tweetID);
				urlTweetIDs.put(tweetID, amount);
			} else {
				urlTweetIDs.put(tweetID, 1);
			}
		}

		// calculate the score of every tweet ID
		for (int m = 0; m < tweetList.size(); m++) {
			Tweet tweet = tweetList.getRankedList().get(m);

			// if the tweet is in the url list it is likely very good
			if (urlTweetIDs.containsKey(tweet.getTweetID())) {
				// if the tweet ID is in the reliable list
				tweet.setScore(tweet.getScore()
						+ DEFAULT_RELIABLE_WEBSITE_BONUS);

				// increase score if there are multiple ranked URLs in
				// the same tweet
				if (urlTweetIDs.get(tweet.getTweetID()) >= 2) {
					tweet.setScore(tweet.getScore()
							+ (urlTweetIDs.get(tweet.getTweetID()) * DEFAULT_RELIABLE_WEBSITE_BONUS));
				}
			}

			// if it is only in the tweet content list it is likely less
			// informative
			else if (!URLListGen.containsUrl(tweet.getStatus())) {
				float temp = tweet.getScore() + DEFAULT_NON_URL_PENALTY;

				if (temp > 0) {
					tweet.setScore(tweet.getScore() + DEFAULT_NON_URL_PENALTY);
				}
				// if it is only in the tweet content list it is likely less
				// informative
			} else {
				tweet.setScore(tweet.getScore() + DEFAULT_URLBOOST_BONUS);
			}
		}

		return tweetList;
	}
}