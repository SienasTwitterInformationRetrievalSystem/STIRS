package Query_ExpansionInternal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import API.LuceneQuery;
import Main.QueryExpansion;

/**
 * This program reads in a file and only processes the hashtags from that file.
 * When it processes them, it prints out each hashtag, separated out by
 * camel-case, into a file of the users choice.
 * 
 * @author Matt Roberts v1.0
 * @author Lauren Mathews v1.5
 * @version 7/15/2014 v1.0
 * @version 7/17/2014 v1.5
 */
public class HashTags implements QueryExpansion {

	private String harddrive;
	private String inputName;
	private HashSet<String> stopWords = new HashSet<String>();

	public HashTags(String hD, String inputFile) {
		this.harddrive = hD;
		this.inputName = harddrive + "/" + inputFile;
	}

	/**
	 * Grabs the top unique hashtags for each topic
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queries) {
		String hashtagList = harddrive
				+ "/src/Query_ExpansionInternal/hashtags.txt";

		HashMap<String, HashMap<String, Integer>> hashtags = new HashMap<String, HashMap<String, Integer>>();
		ArrayList<String> newQueries = new ArrayList<String>();

		ArrayList<String> orgQueries = new ArrayList<String>();

		try {
			// creates a file with all hashtags for each topic
			separate(inputName, hashtagList);
		} catch (Exception e) {
			System.err
					.println("In HashTags.java (in getNewQueries). Something went wrong separating the hashtags. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		try {
			// finds unique hashtags for each topic
			hashtags = findHashtags(hashtagList);
		} catch (IOException e) {
			System.err
					.println("In HashTags.java (in getNewQueries). Something went wrong finding the hashtags. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		if (hashtags.size() != queries.size()) {
			System.err.println("queries.size(): " + queries.size());
			System.err.println("hashtags.size(): " + hashtags.size());
			System.exit(0);
		}

		String newQuery = "";

		Iterator it = hashtags.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			@SuppressWarnings("unchecked")
			Iterator itr = ((HashMap<String, Integer>) pairs.getValue())
					.entrySet().iterator();

			ArrayList<String> queList = new ArrayList<String>();

			// goes through each word for each topic
			while (itr.hasNext()) {
				Map.Entry pairs2 = (Map.Entry) itr.next();

				// if it occured more then once in that topic
				// only keeps unique hashtags for that topic
				if ((int) pairs2.getValue() > 2
						&& !stopWords.contains(pairs2.getKey())) {
					queList.add((String) pairs2.getKey());
					queList.add("(v: " + pairs2.getValue() + ")");
				}
			}

			newQuery = findTopHashtags(queList).trim();
			orgQueries.add(pairs.getKey() + ": " + newQuery);
			newQuery = "";
		}

		// goes through and adds them in correct format
		// (hashmaps added the topics in random places)
		for (int i = 0; i < queries.size(); i++) {
			for (int j = 0; j < orgQueries.size(); j++) {
				String query = orgQueries.get(j);
				String topicNum = query.substring(2, query.indexOf(":"));

				if (queries.get(i).getQueryNum().equalsIgnoreCase(topicNum)) {
					newQueries.add(query.substring(query.indexOf(":") + 2)
							.trim());
				}
			}
		}

		return newQueries;
	}

	/**
	 * Orders each topic's list from max to min.
	 * 
	 * @return Returns top 10 hashtags.
	 */
	private String findTopHashtags(ArrayList<String> queList) {
		String query = "";

		ArrayList<Integer> topNumbers = new ArrayList<Integer>();

		for (int i = 1; i < queList.size(); i += 2) {
			String curTok = queList.get(i);

			int amount = Integer.parseInt(curTok.substring(
					curTok.indexOf("v: ") + 3, curTok.indexOf(")")));

			topNumbers.add(amount);
		}

		// goes through the list
		for (int i = 0; i < Math.min(queList.size(), 10); i++) {
			// finds the top number each time
			int maxNum = Collections.max(topNumbers);

			// once it gets down to ones, we don't want it anymore
			if (maxNum == 2) {
				break;
			}

			// all values had been inputted with "&", to distinguish
			// them from words which were numbers
			// this re-adds them so it can be matched up with arraylist
			String findMaxNum = "(v: " + String.valueOf(maxNum) + ")";

			// gets the index of the max number
			int maxNumIndex = queList.indexOf(findMaxNum);

			// gets the word that matches the top number
			String wordMax = queList.get(maxNumIndex - 1);

			// adds the word and number, in order, to arraylist
			query += wordMax + " ";

			// removes it from these two arraylists
			// keeps the word and number from being found again
			queList.remove(maxNumIndex);
			topNumbers.remove(topNumbers.indexOf(maxNum));
		}

		return query;
	}

	/**
	 * Goes through hashtag file. For each topic, finds amount of each hashtag
	 * occuring.
	 * 
	 * @param hashtagList
	 *            The file name
	 * @return For each topic, a list of word and the amount it occured in that
	 *         topic.
	 */
	@SuppressWarnings("rawtypes")
	private HashMap<String, HashMap<String, Integer>> findHashtags(
			String hashtagList) throws IOException {
		HashMap<String, HashMap<String, Integer>> hashtags = new HashMap<String, HashMap<String, Integer>>();

		HashMap<String, Integer> topHashTags = new HashMap<String, Integer>();

		BufferedReader bfr = null;
		try {
			bfr = new BufferedReader(new FileReader(hashtagList));
		} catch (IOException e) {
			System.err
					.println("In HashTags.java (in separate). Reading/Writing failed. ERROR: "
							+ e.getMessage());
			System.err.println("hashtagList: " + hashtagList);
			System.exit(0);
		}

		String inputLine = bfr.readLine();

		HashMap<String, Integer> curHashtags = new HashMap<String, Integer>();
		String curTopic = inputLine;

		inputLine = bfr.readLine();

		while (inputLine != null) {
			// new topic
			if (inputLine.startsWith("MB")) {
				hashtags.put(curTopic, curHashtags);
				curTopic = inputLine;
				curHashtags = new HashMap<String, Integer>();
			} else {
				inputLine = inputLine.toLowerCase().trim();

				String plus = inputLine.replace("+", "");
				String s = "";

				// basic stemming
				if (inputLine.endsWith("'s")) {
					s = inputLine.substring(0, inputLine.lastIndexOf("'s"));
				} else if (inputLine.endsWith("s")) {
					s = inputLine.substring(0, inputLine.lastIndexOf("s"));
				} else {
					s = inputLine + "s";
				}

				if (curHashtags.containsKey(s)) {
					int amount = curHashtags.get(s);
					amount++;
					curHashtags.remove(s);
					curHashtags.put(s, amount);
				} else if (curHashtags.containsKey(plus)) {
					int amount = curHashtags.get(plus);
					amount++;
					curHashtags.remove(plus);
					curHashtags.put(plus, amount);
				} else if (curHashtags.containsKey(inputLine)) {
					int amount = curHashtags.get(inputLine);
					amount++;
					curHashtags.remove(inputLine);
					curHashtags.put(inputLine, amount);
				} else {
					curHashtags.put(inputLine, 1);

					// top hashtags containus all hashtags among different
					// topics
					if (topHashTags.containsKey(inputLine)) {
						int amount = topHashTags.get(inputLine);
						amount++;
						topHashTags.remove(inputLine);
						topHashTags.put(inputLine, amount);
					} else {
						topHashTags.put(inputLine, 1);
					}
				}
			}

			inputLine = bfr.readLine();

			if (inputLine == null) {
				hashtags.put(curTopic, curHashtags);
			}
		}

		bfr.close();

		// adds those that are occurring more then once across topics
		Iterator it = topHashTags.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			if ((int) pairs.getValue() > 1) {
				stopWords.add((String) pairs.getKey());
			}
		}

		return hashtags;
	}

	/**
	 * This method goes through an output.csv file and separates out the tweet's
	 * hashtag by camel case, except in cases where the hashtag is all
	 * capitalized
	 * 
	 * e.g. #ThisIsAnExample becomes #This Is An Example e.g. #NSA will stay
	 * #NSA
	 * 
	 * @param readInFile
	 *            is the file to read in
	 * @param outputFile
	 *            is the file that is written to
	 */
	private void separate(String readInFile, String outputFile)
			throws IOException {
		BufferedWriter writer = null;
		BufferedReader reader = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			reader = new BufferedReader(new FileReader(readInFile));
		} catch (IOException e) {
			System.err
					.println("In HashTags.java (in separate). Reading/Writing failed. ERROR: "
							+ e.getMessage());
			System.err.println("readInFile: " + readInFile);
			System.err.println("outputFile: " + outputFile);
			System.exit(0);
		}

		String next = reader.readLine();
		next = reader.readLine();
		next = reader.readLine();
		next = reader.readLine();

		String curTopic = next.substring(0, next.indexOf(","));
		writer.write(curTopic);
		writer.newLine();
		writer.flush();
		String pastTopic = curTopic;

		while (next != null) {
			next = reader.readLine();

			if (next == null) {
				break;
			}

			curTopic = next.substring(0, next.indexOf(","));

			if (!pastTopic.equalsIgnoreCase(curTopic)) {
				pastTopic = curTopic;
				writer.write(curTopic);
				writer.newLine();
				writer.flush();
			}

			boolean capital = true;// checks to see if the letter before is a
									// capital letter

			String[] line = next.split(",");// splits up the tweets
			String tweet = "";

			if (line.length > 2) {
				tweet = line[2]; // sets the variable to be the tweet
			}

			if (tweet.contains("#")) {
				StringTokenizer st = new StringTokenizer(tweet);
				while (st.hasMoreTokens()) {// makes sure that the next token is
											// not null
					String nextToken = st.nextToken();
					char[] seq = new char[nextToken.length() + 1];
					// a char array makes it possible to check is character
					// easily

					if (nextToken.contains("#") && !nextToken.contains("http")) {
						// makes sure that the token to procss is a hastag and
						// not a link
						if (nextToken.charAt(0) != '#') {
							nextToken = nextToken.substring(nextToken
									.lastIndexOf('#'));
							// makes sure that the sequencing starts at the
							// hashtag
							// e.g. oooqps#ThisIsAnExample starts the camel case
							// at #ThisIsAnExample
						}

						seq[0] = nextToken.charAt(0);
						writer.write(seq[0]);// writes the hashtag first

						// loops through the char array ad checks
						for (int i = 1; i < nextToken.length(); i++) {
							seq[i] = nextToken.charAt(i);
							if (i <= 1) {
								writer.write(seq[i]);
							} else {
								char toCheck = seq[i];
								if (Character.isUpperCase(toCheck)
										&& capital == false) {
									writer.write("+");
									capital = true;
									writer.write(seq[i]);
									continue;
									// checks the case and if it is an upper
									// case letter
									// if it is and the letter before is lower
									// case it writes a space and then the
									// letter
								} else if (Character.isUpperCase(toCheck)
										&& capital == true) {
									writer.write(seq[i]);
									capital = true;
									continue;
									// if the letter before is upper case and
									// the current letter is upper case
									// it writes the letter without a space
								} else if (Character.isLowerCase(toCheck)
										&& capital == true) {
									capital = false;
									// sets capital to false because the current
									// letter isnt a capital
								} else {
									capital = false;
									// sets capital to false because the current
									// letter isnt a capital
								}

								if (toCheck == '_' || toCheck == '-') {
									writer.write("+");
								} else if (toCheck == '"' || toCheck == '.'
										|| toCheck == '(' || toCheck == ')'
										|| toCheck == '"' || toCheck == ':'
										|| toCheck == ';' || toCheck == '.'
										|| toCheck == ']' || toCheck == '[') {
									writer.write("");
								} else {
									writer.write(seq[i]);
								}
								// checks and replaces punctuation, and if it is
								// a letter, it will be a lower case letter
								// and will be written to the file
							}
						}

						writer.newLine();
						capital = true;
						// resets capital to true just in case there are
						// multiple
						// hashtags in the same tweet
					}
				}

				next = reader.readLine();
			} else {
				next = reader.readLine();
				// if there are no hashtags in the tweet
			}
		}

		writer.close();
		reader.close();
	}
}