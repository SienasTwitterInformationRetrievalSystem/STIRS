package Machine_Learning;

import java.util.ArrayList;
import java.io.*;

import weka.classifiers.Classifier;
import weka.classifiers.rules.JRip; //40.54 40.54
/*import weka.classifiers.bayes.BayesNet; //39.07
 import weka.classifiers.functions.SimpleLogistic; //39.54
 import weka.classifiers.rules.DecisionTable; //40.07
 import weka.classifiers.rules.PART; //38.41
 import weka.classifiers.rules.ZeroR; //40.54
 import weka.classifiers.trees.DecisionStump; //40.47
 import weka.classifiers.trees.J48; //40.41
 import weka.classifiers.trees.REPTree; //36.47*/
import weka.core.Instances;

import API.RankedTweetList;
import API.Tweet;
import LinkCrawling.TwitterIndexerUrls;
import Main.Module;
import API.LuceneQuery;

/**
 * Twitter Module 2, which uses Weka and data retrieved from the Twitter API to
 * rank tweets
 * 
 * @author Denis Kalic & Matthew Kemmer v1.0
 * @author Timothy LaRock v2.0
 * @author Lauren Mathews v2.5
 * @version 7/18/2011 v1.0
 * @version 7/15/2014 v2.0
 * @version 7/17/2014 v2.5
 */
public class TM2 implements Module {

	private String harddrive = "", judges;
	private boolean createTrainingSet = false;

	public TM2(String hd, boolean create, String judgment) {
		this.harddrive = hd;
		this.createTrainingSet = create;
		this.judges = hd + judgment;
	}

	/**
	 * Takes in a list of tweets, and ranks them based on the Weka decision tree
	 * and other data about the tweet and from Twitter.
	 * 
	 * @param queries
	 *            A list of LuceneQuery objects which correspond to the TREC
	 *            queries
	 * @param tweetList
	 *            A list of RankedTweetList objects, which have the Lucene
	 *            results for each query
	 * @results The same RankedTweetList that was taken in, but modified to
	 *          reflect the TM2 results
	 */
	public ArrayList<ArrayList<RankedTweetList>> getResults(
			ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> tweetList) {

		// this is a list of ranked tweets that will be returned
		RankedTweetList rankedTweet = null;

		// Create ArrayList of RankedTweetLists of
		// tweets with relevent URLs
		ArrayList<RankedTweetList> linksList = TwitterIndexerUrls
				.createRankedUrls(queries, tweetList, 30, harddrive);

		// goes through each ranked tweet list and makes a new instance of
		// a weka tweet which has more information about a tweet than
		// a regular tweet for relevance purposes
		ArrayList<WekaTweet> wekaTweetList = new ArrayList<WekaTweet>();

		if (createTrainingSet) {
			System.out.println("Starting to create training set (WEKA)...");

			for (int h = 0; h < tweetList.size(); h++) {

				System.out.println("Topic: " + (h + 1));

				for (int i = 0; i < tweetList.get(h).size(); i++) {
					// Gets current ranked tweet list
					rankedTweet = tweetList.get(h).get(i);

					// each tweet a new weka tweet will be created
					for (int j = 0; j < Math.min(rankedTweet.size(), 200); j++) {
						Tweet singleTweet = rankedTweet.getTweet(j);

						// tweet information/content
						String currentStatus = singleTweet.getStatus();

						// tweet ID
						long tweetID = singleTweet.getTweetID();
						int retweets = singleTweet.getRe_count();
						double tweets = (double) singleTweet.getStatusesCount();
						double followers = (double) singleTweet
								.getFollowersCount();
						double tweetsToFollowers = 0;
						tweetsToFollowers = tweets / followers;
						int followers_count = singleTweet.getFollowersCount();

						// set relevantURL to false
						boolean relevantURL = false;

						// check if each tweet is contained in
						// the relevant url set
						for (int n = 0; n < linksList.get(i).size(); n++) {
							if (linksList.get(i).getTweet(n).getTweetID() == tweetID) {
								relevantURL = true;
							}
						}

						// New instance of the weka processor
						ProcessAttributes processer = new ProcessAttributes(
								currentStatus, tweetID, retweets,
								tweetsToFollowers, followers_count,
								relevantURL, queries, judges);

						// processes weka tweet
						WekaTweet wekaTweet = processer.processWekaTweet(i,
								true);
						wekaTweetList.add(wekaTweet);
					}
				}
			}

			try {
				// creates the training set for the tweets
				createTrainingSet(wekaTweetList, "tweetSet");

				// Creates an ARFF file out of the csv file created
				// by creatTrainingSet()
				new ARFFCreator(harddrive
						+ "/src/Machine_Learning/tweetSet.csv", harddrive
						+ "/src/Machine_Learning/tweetSet.arff");
			} catch (IOException io) {
				System.err.println("In TM2.java (in: " + io.getMessage());
			}

			System.out.println("Finished creating training set (WEKA).");

			System.exit(0);

			return null;
		} else {
			System.out.println("Creating model from training set...");

			Classifier cls = new JRip();
			Instances inst = null;
			try {
				inst = new Instances(new BufferedReader(new FileReader(
						harddrive + "/src/Machine_Learning/tweetSet.arff")));

				inst.setClassIndex(inst.numAttributes() - 1);
				cls.buildClassifier(inst);
				weka.core.SerializationHelper.write(harddrive
						+ "/src/Machine_Learning/tree.model", cls);
			} catch (Exception e) {
				System.err
						.println("In TM2 (in getResults). Something happened when making the training model. ERROR: "
								+ e.getMessage());
				System.exit(0);
			}

			System.out
					.println("Finished creating model. Going through all tweets...");

			for (int h = 0; h < tweetList.size(); h++) {
				wekaTweetList.clear();

				for (int i = 0; i < tweetList.get(h).size(); i++) {
					// Gets current ranked tweet list
					RankedTweetList rtl = tweetList.get(h).get(i);
					ArrayList<Tweet> originalList = rtl.getRankedList();
					// each tweet a new weka tweet will be created
					for (int j = 0; j < rtl.size(); j++) {
						Tweet singleTweet = originalList.get(j);

						// tweet information/content
						String currentStatus = singleTweet.getStatus();

						// tweet ID
						long tweetID = singleTweet.getTweetID();
						int retweets = singleTweet.getRe_count();
						double tweets = (double) singleTweet.getStatusesCount();
						double followers = (double) singleTweet
								.getFollowersCount();
						double tweetsToFollowers = 0;
						tweetsToFollowers = tweets / followers;
						int followers_count = singleTweet.getFollowersCount();

						// set relevantURL to false
						boolean relevantURL = false;

						// check if each tweet is contained in
						// the relevant url set
						for (int n = 0; n < linksList.get(i).size(); n++) {
							if (linksList.get(i).getTweet(n).getTweetID() == tweetID) {
								relevantURL = true;
							}
						}

						// New instance of the weka processor
						ProcessAttributes processer = new ProcessAttributes(
								currentStatus, tweetID, retweets,
								tweetsToFollowers, followers_count,
								relevantURL, queries, judges);

						// processes weka tweet
						WekaTweet wekaTweet = processer.processWekaTweet(i,
								false);
						wekaTweetList.add(wekaTweet);
					}

					try {
						// creates the training set for the tweets
						createTrainingSet(wekaTweetList, "trainingSet");

						// Creates an ARFF file out of the csv file created
						// by creatTrainingSet()
						new ARFFCreator(
								harddrive
										+ "/src/Machine_Learning/trainingSet.csv",
								harddrive
										+ "/src/Machine_Learning/trainingSet.arff");
					} catch (IOException io) {
						System.err
								.println("In TM2.java (in getResults). Trouble with creating training set. ERROR: "
										+ io.getMessage());
					}

					// Creates a new instance of the weka object
					Weka w = new Weka(harddrive, "tree", "trainingSet");

					// gives an error if weka is not loaded correctly
					// and returns the list as such
					if (!w.loadedCorrectly()) {
						System.err
								.println("In TM2 (in getResults). WEKA was not loaded correctly.");

						System.exit(0);
					}

					ArrayList<Tweet> list = w.run(originalList);

					tweetList.get(h).set(i, new RankedTweetList(list));
				}
			}

			System.out.println("Finished tweet analysis.");

			return tweetList;
		}
	}

	/**
	 * Creates the trainingSet.csv file of the current attributes for WEKA use.
	 * 
	 * @param testSet
	 *            Contains the attributes in ArrayList form, to be converted.
	 */
	private void createTrainingSet(ArrayList<WekaTweet> testSet, String csv) {
		try {
			BufferedWriter bfw = new BufferedWriter(new FileWriter(harddrive
					+ "/src/Machine_Learning/" + csv + ".csv"));

			// gets the attributes to read
			bfw.write(testSet.get(0).newAttributes());

			// writes each line to a file using csv format
			for (int i = 0; i < testSet.size(); i++) {
				bfw.newLine();
				WekaTweet currentTweet = testSet.get(i);
				bfw.write(currentTweet.toString());
				bfw.flush();
			}
			bfw.close();
		} catch (IOException io) {
			System.err
					.println("In TM2 (in createTrainingSet). Caught IOException in createTrainingSet() "
							+ io.getMessage());
		}
	}
}