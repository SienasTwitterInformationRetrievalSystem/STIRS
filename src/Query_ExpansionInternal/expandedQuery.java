package Query_ExpansionInternal;

import java.util.ArrayList;
import java.util.StringTokenizer;

import API.LuceneQuery;
import Miscellaneous.POSTagger;

/**
 * Originally: Expands the current query topics.
 * 
 * Version 1.5: Updated it so it return an arraylist of the new queries (to add
 * on, not including the original query topic)
 * 
 * @author Lauren Mathews
 * @editor Lauren Mathews v1.5
 * @version 7/3/12 v1.0
 * @version 6/17/14 v1.5
 */
public class expandedQuery {

	/**
	 * Creates an arraylist of all the new queries Is important because could
	 * use POSTagger to narrow them down.
	 * 
	 * @param queries
	 *            an arraylist of queries, created by QueryProcess.java
	 * @param top10Words
	 *            an arraylist of top10Words for each topic, created by
	 *            top10Words.java
	 * @param modelFile
	 *            file path to the model (for POSTagger) en-pos-maxent.bin, in
	 *            libraries
	 * @return an arraylist of all the new queries to be added (not including
	 *         original)
	 */
	public static ArrayList<String> expandTheQuery(
			ArrayList<LuceneQuery> queries, ArrayList<String> top10,
			String modelFile) {
		// checks arguments to make sure they aren't null/empty
		if (queries.isEmpty() || queries == null) {
			System.err
					.println("In expandTheQuery.java. queries (topics) is empty or null.");
			System.exit(0);
		}

		if (top10.isEmpty() || top10 == null) {
			System.err
					.println("In expandTheQuery.java. top10Words (topics) is empty or null.");
			System.exit(0);
		}

		String speechCodes = "NN NNS NNP NNPS";

		// will contain the list of new topics
		ArrayList<String> newTopics = new ArrayList<String>();

		// used to keep track of top words
		ArrayList<String> wordsInTop = new ArrayList<String>();

		POSTagger tagger = null;
		// only if we're not using external data
		if (!(modelFile.isEmpty() && modelFile.equals(""))) {
			// Creates an instance of the POSTagger class, for finding nouns
			try {
				tagger = new POSTagger();
				tagger.initializeTagger(modelFile);
			} catch (Exception e) {
				System.err
						.println("In expandedQuery.java. Something happened when creating the POSTagger. ERROR: "
								+ e.getMessage());
				System.err.println("modelFile: " + modelFile);
				System.exit(0);
			}
		}

		// While loop: goes through each line to expand each topic one at a time
		for (int i = 0; i < queries.size(); i++) {
			// creates the topic number
			String curTopic = "mb" + queries.get(i).getQueryNum();
			
			// used to create all the words on one line
			String newQuery = "";

			// grabs the indexes for the for loop
			int orgIndex = top10.indexOf(curTopic);

			// makes sure it has a list of words to be expanded
			if (top10.get(orgIndex + 1).contains("mb")) {
				newTopics.add("");
				continue;
			}

			int startIndex = orgIndex + 1;
			int endIndex = startIndex + 10;

			// goes through top10Words, for each topic
			// one topic per each k for loop
			for (int k = startIndex; k < endIndex; k++) {
				int indexOfSpace = top10.get(k).indexOf(" ");

				// some topics have less then 10 words
				if (indexOfSpace < 0) {
					break;
				}

				String word = top10.get(k).substring(0, indexOfSpace);

				String s = "";

				if (word.endsWith("'s")) {
					s = word.substring(0, word.lastIndexOf("'s"));
				} else if (word.endsWith("s")) {
					s = word.substring(0, word.lastIndexOf("s"));
				} else {
					s = word + "s";
				}

				// checks for hashtags duplication
				if (word.startsWith("#")) {
					word = word.substring(1);
				}

				if (!(wordsInTop.contains(word) || word.equalsIgnoreCase("rn") || wordsInTop
						.contains(s))) {
					wordsInTop.add(word);
				}
			}

			// if external data is turned on
			if (!(modelFile.isEmpty() && modelFile.equals(""))) {
				// Only writes those that are nouns
				for (int l = 0; l < wordsInTop.size(); l++) {

					String word = wordsInTop.get(l);

					// makes sure to have hashtags and retweets work with POS
					if (word.contains("#") || word.contains("@")) {
						word = word.substring(1);
					}

					String[] tags = tagger.findTags(word);

					if (tags.length >= 1) {
						StringTokenizer sTnoun = new StringTokenizer(
								speechCodes, " ");
						String nounInput = sTnoun.nextToken();

						// goes through each of the speech codes
						while (sTnoun.hasMoreTokens()) {
							// if the word is a noun
							if (tags[0].equals(nounInput)) {
								// Updates the newQuery line for each loop
								newQuery = newQuery + " " + wordsInTop.get(l);

								break;
							}

							nounInput = sTnoun.nextToken();
						}
					}
				}
			} else {
				// if external data not turned on, just adds all words
				for (int f = 0; f < wordsInTop.size(); f++) {
					newQuery = newQuery + " " + wordsInTop.get(f);
				}
			}
			
			// adds the topic and words to newTopics
			newTopics.add(newQuery.toLowerCase().trim());

			// keeps it clean for each topic
			wordsInTop.clear();
		}

		return newTopics;
	}
}