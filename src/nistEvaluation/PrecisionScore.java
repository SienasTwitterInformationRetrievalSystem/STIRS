package nistEvaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Main class of determining Precision for STIRS Goes through the run and
 * figures out how well we did compared to NIST/TREC.
 * 
 * Basically, this is how Precision @ 30 Works:
 * 1) You gather up the 1s and 2s from NISTJudgments
 * 2) You go through the tweetIDs from each topic from your run
 * 3) If the tweetID is in the NISTJudgments, precision goes up
 * 
 * @ 30 means it's the top 30 tweets of each topic that counts
 * 
 * @author Karl Appel v1.0 & v1.5
 * @author Lauren Mathews v2.0
 * @version 6/2011 v1.0
 * @version 6/2012 v1.5
 * @version 7/10/2014 v2.0
 */
public class PrecisionScore {

	private AllNISTTopics allNISTTopics = new AllNISTTopics();
	private static float PRECISIONBASE = 30;

	// default number of topics
	private int numOfTopics = 50;
	
	private String firstTopicNum = "1";
	private String tag = "default";

	/**
	 * Goes through and creates the precision score for the run
	 * 
	 * @param harddrive
	 *            Where these files have been located
	 * @param tag
	 *            The name of the run, which the csv file is called
	 */
	public PrecisionScore(String harddrive, String TAG, int numTopics, String judges) {
		this.numOfTopics = numTopics;
		String trec = judges;
		this.tag = harddrive + "/" + TAG + ".txt";
		String stirs = harddrive + "/" + TAG + ".csv";

		try {
			readRelJudge(trec);
		} catch (IOException e) {
			System.err
					.println("In PrecisionScore.java (in PrecisionScore). Something occured when making readRelJudge. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		if (allNISTTopics.howMany() < numTopics) {
			System.err
					.println("(In PrecisionScore) During a real run, the number of topics (in NISTJudgments) shouldn't be less than "
							+ numTopics
							+ " (size is "
							+ allNISTTopics.howMany() + ")!");
		}

		try {
			readSTIRSOutput(stirs);
		} catch (Exception e) {
			System.err
					.println("In PrecisionScore.java (in PrecisionScore). Something occured when making readSTIRSOutput. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Goes through and collects a list of each tweet's relevance.
	 * 
	 * @param trecPath
	 *            NISTJudgments.txt, official NIST judgments of each tweet
	 */
	public void readRelJudge(String trecPath) throws IOException {
		// all that are listed as irrelevant (0 or less)
		ArrayList<String> relZero = new ArrayList<String>();

		// all that are listed as as relevant (1 only)
		ArrayList<String> relOne = new ArrayList<String>();

		// all that are listed as highly relevant (2 or more)
		ArrayList<String> relTwo = new ArrayList<String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(trecPath));
		} catch (FileNotFoundException e) {
			System.err
					.println("In PrecisionScore.java (in readRelJudge). Something occured when making NISTJudgments.txt. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		// keeps track when it changes to a new topic
		String previousTopic = null;

		// will hold all the tweets, and their relevance, for each topic
		NISTTopic currentTopic = new NISTTopic();

		String line = br.readLine();
		firstTopicNum = line.substring(0, line.indexOf(" "));

		// READ TO TOPIC 30 , THEN TO THE OTHER TOPICS 30-50
		while (line != null) {
			// splits up each line
			StringTokenizer st = new StringTokenizer(line, " ");

			// GET TOPIC, TWEETID and SCORE
			String topic = st.nextToken();
			st.nextToken();
			String tweetId = st.nextToken();
			String relevance = st.nextToken();

			// READ ONE TOPIC AT A TIME; topic change
			if (previousTopic != null && !previousTopic.equalsIgnoreCase(topic)) {
				currentTopic.setRelevanceOne(relOne);
				currentTopic.setRelevanceTwo(relTwo);
				currentTopic.setNonRelevant(relZero);
				currentTopic.setTopicNum(previousTopic);

				allNISTTopics.addTopic(currentTopic);

				relOne = new ArrayList<String>();
				relTwo = new ArrayList<String>();
				relZero = new ArrayList<String>();

				currentTopic = new NISTTopic();
				currentTopic.setTopicNum(topic);

				previousTopic = topic;
			}

			if (previousTopic == null) {
				previousTopic = topic;
			}

			int score = Integer.parseInt(relevance);

			//counts out number of 0s, 1s and 2s
			if (score <= 0) {
				relZero.add(tweetId.trim());
			} else if (score == 1) {
				relOne.add(tweetId.trim());
			} else if (score >= 2) {
				relTwo.add(tweetId.trim());
			}

			line = br.readLine();
		}

		// ADD THE LAST ONE WHEN WE HAVE REACHED END OF FILE
		currentTopic.setRelevanceOne(relOne);
		currentTopic.setRelevanceTwo(relTwo);
		currentTopic.setNonRelevant(relZero);
		currentTopic.setTopicNum(previousTopic);

		allNISTTopics.addTopic(currentTopic);
	}

	/**
	 * Determines the precision of the run, compared to NISTJudgments.
	 * 
	 * @param stirsPath
	 *            The tag.csv file
	 */
	public void readSTIRSOutput(String stirsPath) throws IOException {
		// calculates the average precision of the run
		float totalAveragePrecision = 0;

		// calculates the overall precision of the run
		float totalHighRelevantPrecision = 0;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(stirsPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line = br.readLine();

		String previousTopic = null;

		int countOne = 0;
		int countTwo = 0;

		// grabs the NIST judgements of that topic
		NISTTopic nTopic = allNISTTopics.getNISTTopicByTopicNum(firstTopicNum);
		ArrayList<?> relOne = nTopic.getRelevanceOne();
		ArrayList<?> relTwo = nTopic.getRelevanceTwo();

		int amount = 1;

		// READ TO END
		String topic = null;
		while (line != null) {
			if (line.trim().length() == 0) {
				line = br.readLine();
			}

			if (line == null) {
				break;
			}

			StringTokenizer st = new StringTokenizer(line, ",");
			// System.out.println("line: " + line);

			// GET TOPIC
			topic = st.nextToken();
			topic = topic.substring(2);
			topic = topic.replaceFirst("0*", "");
			st.nextToken();
			String tweetId = st.nextToken();

			// topic change
			if (previousTopic != null && !previousTopic.equalsIgnoreCase(topic)) {
				amount = 1;

				float highRel = countTwo / PRECISIONBASE;
				
				float combRel = (countOne + countTwo) / PRECISIONBASE;
				totalAveragePrecision = totalAveragePrecision + combRel;
				totalHighRelevantPrecision = totalHighRelevantPrecision
						+ highRel;
				
				previousTopic = topic;
				countOne = 0;
				countTwo = 0;

				nTopic = allNISTTopics.getNISTTopicByTopicNum(topic);

				if (nTopic != null) {
					relOne = nTopic.getRelevanceOne();
					relTwo = nTopic.getRelevanceTwo();

					previousTopic = topic;
				}
			}

			if (previousTopic == null) {
				previousTopic = topic;
			}

			if (amount < 30) {
				// determines if the count is the same as NIST judges
				if (relOne.contains(tweetId.trim())) {
					countOne++;
				} else if (relTwo.contains(tweetId.trim())) {
					countTwo++;
				}
			}

			amount++;

			line = br.readLine();
		}

		// ADD THE LAST ONE WHEN WE HAVE REACHED END OF FILE
		float highRel = countOne / PRECISIONBASE;
		float combRel = (countOne + countTwo) / PRECISIONBASE;
		totalAveragePrecision = totalAveragePrecision + combRel;
		totalHighRelevantPrecision = totalHighRelevantPrecision
				+ highRel;
		
		//Calculates precision across topics
		double percentage = totalAveragePrecision / numOfTopics;
		percentage = percentage * 100;
		percentage = round(new BigDecimal(percentage, MathContext.DECIMAL64),
				2, true).doubleValue();

		BufferedWriter bfw = new BufferedWriter(new FileWriter(tag));

		System.out
				.println("Total Average Precision (1 & 2 scored tweets) at 30: "
						+ percentage + "%");

		bfw.write("Total Average Precision (1 & 2 scored tweets) at 30: "
				+ percentage + "%");
		bfw.newLine();
		bfw.flush();

		if ((totalAveragePrecision / numOfTopics) > 1) {
			System.err
					.println("In PrecisionScore.java (in readSTIRSOutput). Precision should be less than 1 (make sure tag.csv file only has top 30 for each topic)!");
		}

		percentage = totalHighRelevantPrecision / numOfTopics;
		percentage = percentage * 100;
		percentage = round(new BigDecimal(percentage, MathContext.DECIMAL64),
				2, true).doubleValue();

		System.out
				.println("Total High Relevant Precision (2 scored tweets only) at 30: "
						+ percentage + "%");

		bfw.write("Total High Relevant Precision (2 scored tweets only) at 30: "
				+ percentage + "%");
		bfw.newLine();
		bfw.flush();

		bfw.close();
	}

	/**
	 * Rounds up decimals to percentage:
	 * 
	 * percentage = totalHighRelevantPrecision / numOfTopics;
	 * percentage = percentage * 100;
	 * percentage = round(new BigDecimal(percentage, MathContext.DECIMAL64), 2, true).doubleValue();
	 * 
	 * @param d new BigDecimal(percentage, MathContext.DECIMAL64)
	 * @param scale How many digits to show (2, 3, etc)
	 * @param roundUp Whether to round up or down
	 * @return The percentage
	 */
	public static BigDecimal round(BigDecimal d, int scale, boolean roundUp) {
		int mode = (roundUp) ? BigDecimal.ROUND_UP : BigDecimal.ROUND_DOWN;
		return d.setScale(scale, mode);
	}
}