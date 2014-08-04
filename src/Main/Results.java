package Main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Miscellaneous.TweetIDComparator;
import Miscellaneous.TweetRankComparator;
import Miscellaneous.TweetScoreComparator;
import API.RankedTweetList;
import API.Tweet;
import API.LuceneQuery;

/**
 * Outputs the Tweet results in the TREC format
 * 
 * @author Matthew Kemmer and Carl Tompkins v1.0
 * @edited Karl Appel (with added comments) v2.0
 * @author Lauren Mathews v3.0
 * 
 * @version 7/8/2011 v1.0
 * @version 6/20/12 v2.0
 * @version 7/11/14 v3.0
 */
public class Results {

	/**
	 * Outputs the results in the TREC format based on a list of Tweet objects
	 * 
	 * @param tweets
	 *            An ArrayList of Tweets
	 * @param tag
	 *            the run (tag) name
	 * @param scoreComparator
	 *            whether to print by a sorting algorithm (TweetIDComparator,
	 *            TweetScoreComparator, etc)
	 * @param queries
	 *            arraylist of topics
	 */
	public static void output(ArrayList<ArrayList<RankedTweetList>> tweets,
			String tag, boolean scoreComparator,
			ArrayList<LuceneQuery> queries, boolean raw, boolean api, int index, String task) {

		if (tweets == null) {
			System.err.println("In Results.java (in output). tweets is null!");
			System.exit(0);
		}

		if (tag.equalsIgnoreCase("default")) {
			System.err
					.println("In Results.java (in output). tag was set to 'default'.");
		}
		
		try {
			// object we use to write out to the file we specified
			BufferedWriter bfwriter = null;
			if (!raw) {
				if(api){
					if(index == 0){
						System.out.println("Using the output-split.csv!");
						bfwriter = new BufferedWriter(new FileWriter("output-split.csv"));
					}else{
						System.out.println("Using the output.csv (1)!");
						bfwriter = new BufferedWriter(new FileWriter("output.csv"));
					}
				}else{
					if(index == 1){
						System.out.println("Printing for TTG...");
						bfwriter = new BufferedWriter(new FileWriter(tag + ".csv"));
					}else{
						System.out.println("Using the output.csv (2)!");
						bfwriter = new BufferedWriter(new FileWriter("output.csv"));
					}
				}
			} else {
				bfwriter = new BufferedWriter(new FileWriter(tag + ".csv"));
			}

			if (!raw && index != 1) {
				bfwriter.append("Run: "
						+ tag
						+ "\nJudge\nTopic Num,TweetID,Tweet,Username,followers_count, statuses_count, status_id, user_id, re_status_id, re_used_id, re_count,score,rank\n");
			}
			
			for (int i = 0; i < tweets.size(); i++) {
				// gets the ranked list of tweets for each topic we want to
				// print out
				
				int lastRank = 1;
				
				for (int k = 0; k < tweets.get(i).size(); k++) {
					// gets each topic's tweets
					RankedTweetList rtl = tweets.get(i).get(k);

					// gets the list of those tweets and then sorts them
					List<Tweet> sublist = rtl.getRankedList().subList(0,
							rtl.size());

					if (scoreComparator && index != 1) {
						if (!raw) {
							Collections.sort(sublist, Collections
									.reverseOrder(new TweetScoreComparator()));
						} else if (raw) {
							Collections
									.sort(sublist, new TweetRankComparator());
						}
					} else {
						Collections.sort(sublist, Collections
								.reverseOrder(new TweetIDComparator()));
					}
					
					int amount;
					if(!raw){
						amount = rtl.size();
					}else{
						amount = Math.min(1000, rtl.size());
					}
					
					for (int j = 0; j < amount; j++) {
						Tweet tweet = rtl.getRankedList().get(j);

						tweet.setTopic(Integer.parseInt(queries.get(i)
								.getQueryNum()));

						tweet.setTag(tag);
						
						if(lastRank != tweet.getRankInResult() && raw){
							System.err.println("j: " + j);
							System.err.println("In Results.java. Ranks aren't being printed in order!");
							System.err.println("lastRank: " + lastRank);
							System.err.println("tweet.getRankInResult(): " + tweet.getRankInResult());
							System.err.println("tweet.getTweetID(): " + tweet.getTweetID());
							System.err.println("tweet.getTopicNum(): " + tweet.getTopicNum());
						}

						// writes to output.csv
						bfwriter.append(tweet.format(task, raw));
						bfwriter.newLine();
						bfwriter.flush();
						
						lastRank = tweet.getRankInResult();
						lastRank++;
					}
				}
			}

			bfwriter.close();
		} catch (Exception e) {
			System.err
					.println("In Results.java. Didn't make output.csv. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}
	}
}