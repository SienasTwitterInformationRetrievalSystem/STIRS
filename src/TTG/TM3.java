package TTG;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import API.LuceneQuery;
import API.RankedTweetList;
import API.Tweet;
import LinkCrawling.URLListGen;
import Main.Module;

/**
 * Twitter Module 3 is the module for the TREC 2014 Tweet Timeline Generation
 * Task.
 * 
 * @author Timothy LaRock v1.0
 * @author Lauren Mathews v1.5
 * @version 7/25/2014 v1.0
 * @version 7/26/2014 v1.5
 */
public class TM3 implements Module {
	private double duplicateThreshold;

	/**
	 * @param duplicateThreshold
	 *            if the duplicateThreshold is low, more possible duplicates
	 *            will be accepted.
	 */
	public TM3(int duplicateThreshold) {
		this.duplicateThreshold = duplicateThreshold;
	}

	/**
	 * Goes through the first 100 tweets of each topic.
	 * For each topic:
	 * 		1) The first tweet starts off the first cluster group
	 * 		2) Every consecutive tweet gets a percentage of the first tweet
	 * 		   of every cluster for the topic
	 * 		3) If it matches any cluster group, gets added to that cluster
	 * 		4) Otherwise, creates it's own cluster group
	 * Afterwards, it adds the first tweet of each cluster to a list that gets printed.
	 * Cluster groups thata contain only 1 tweet are not included (relevance purposes).
	 * 
	 * You only need to check the first tweet of each cluster because that tweet makes
	 * up the "rules" of what it takes to get into that cluster group.
	 */
	public ArrayList<ArrayList<RankedTweetList>> getResults(
			ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists) {
		// The list to be returned
		ArrayList<ArrayList<RankedTweetList>> returnList = new ArrayList<ArrayList<RankedTweetList>>();

		// goes through all topics
		for (int i = 0; i < rankedTweetLists.size(); i++) {
			ArrayList<RankedTweetList> outerTweetList = new ArrayList<RankedTweetList>();

			ArrayList<ArrayList<Tweet>> clusters = new ArrayList<ArrayList<Tweet>>();
			ArrayList<Tweet> tweets = new ArrayList<Tweet>();

			for (int j = 0; j < rankedTweetLists.get(i).size(); j++) {
				RankedTweetList rtl = rankedTweetLists.get(i).get(j);

				// goes through first 100 tweets of each topic
				for (int k = 0; k < Math.min(rtl.size(), 100); k++) {
					Tweet currTweet = rtl.getTweet(k);
					long tweetID = currTweet.getTweetID();

					// If the list is empty, add the first tweet to the first
					// cluster
					if (clusters.isEmpty()) {
						clusters.add(new ArrayList<Tweet>());
						clusters.get(0).add(currTweet);
					} else {
						for (int m = 0; m < clusters.size(); m++) {
							double percent = percentDuplicate(clusters.get(m)
									.get(0), currTweet);

							long clusterstweetID = clusters.get(m).get(0)
									.getTweetID();

							// we only want to have unique tweets in each topic
							if (tweetID == clusterstweetID) {
								continue;
							}

							// if the percentDuplicate value between the
							// first tweet of a cluster is below the
							// threshold, add that tweet to that cluster
							if (percent >= duplicateThreshold) {
								clusters.get(m).add(currTweet);
								break;
							} else if ((m + 1) == clusters.size()) {
								// if the end of the cluster list is reached,
								// create a new cluster for the tweet
								clusters.add(new ArrayList<Tweet>());

								// add the tweet to the new cluster
								clusters.get(m + 1).add(currTweet);
							}
						}
					}
				}

				// Make the ArrayList<Tweet> to be sent back
				tweets = clustersToTweets(clusters, i);

				outerTweetList.add(new RankedTweetList(tweets));
			}

			returnList.add(outerTweetList);
		}

		return returnList;
	}

	/**
	 * Method that takes an arrayList of arrayLists of Tweets that represent
	 * clusters and creates one tweetlist
	 */
	public ArrayList<Tweet> clustersToTweets(
			ArrayList<ArrayList<Tweet>> clusters, int queryIndex) {

		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

		HashSet<Long> tweetIDs = new HashSet<Long>();

		// goes through each cluster group for each topic
		for (int i = 0; i < clusters.size(); i++) {
			Long tweetID = clusters.get(i).get(0).getTweetID();

			// only want unique tweets to be printed
			if (tweetIDs.contains(tweetID)) {
				continue;
			}

			// gets rid of 1 tweet clusters
			if (clusters.get(i).size() == 1) {
				continue;
			}

			// add first tweet from each cluster group
			// this could be modified better to get the
			// best tweet of each cluster
			tweets.add(clusters.get(i).get(0));

			tweetIDs.add(tweetID);
		}

		return tweets;
	}

	/**
	 * Takes two tweet objects and compares their statuses to find the
	 * percentage of words that match between them
	 * 
	 * @return the percentage of words in the tweets that match
	 */
	public double percentDuplicate(Tweet firstTweet, Tweet secondTweet) {
		double percentage = 0, matchCount = 0;

		String firstStatus = firstTweet.getStatus();
		String secondStatus = secondTweet.getStatus();

		firstStatus = URLListGen.removeURLs(firstStatus);
		secondStatus = URLListGen.removeURLs(secondStatus);

		if (firstStatus.compareToIgnoreCase(secondStatus) == 0) {
			return 100;
		}

		int i = 0;
		StringTokenizer st = new StringTokenizer(firstStatus);
		double numberOfTokens = st.countTokens();
		String[] firstTokenized = new String[st.countTokens()];
		while (st.hasMoreTokens()) {
			firstTokenized[i] = st.nextToken();
			i++;
		}

		i = 0;
		st = new StringTokenizer(secondStatus);
		double numberOfTokens2 = st.countTokens();
		String[] secondTokenized = new String[st.countTokens()];
		while (st.hasMoreTokens()) {
			secondTokenized[i] = st.nextToken();
			i++;
		}

		// counts up the amount of words that are the same
		for (i = 0; i < numberOfTokens; i++) {
			for (int j = 0; j < numberOfTokens2; j++) {
				if (firstTokenized[i].compareToIgnoreCase(secondTokenized[j]) == 0) {
					matchCount++;
				}
			}
		}

		// creates a percentage
		if (matchCount > 0) {
			percentage = (matchCount / numberOfTokens2) * 100;
		} else {
			percentage = 0;
		}

		return percentage;
	}
}