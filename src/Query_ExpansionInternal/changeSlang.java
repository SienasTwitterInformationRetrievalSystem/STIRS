package Query_ExpansionInternal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import LinkCrawling.URLListGen;

/**
 * This program filters out all of the slang words from our slang dictionary. It
 * reads in the slang dictionary into an array list and then cross-references
 * with our output.csv file, and writes it to a separate file.
 * 
 * @author Matt Roberts and Lauren Mathews v1.0
 * @version 6/27/2014 1.0
 */
public class changeSlang {
	/**
	 * Reads in the slangDict.txt into a HashMap
	 * 
	 * @param slangDict
	 *            slangDict.txt, found in QueryExpansionInternal
	 * @return a new HashMap with the slang and the corresponding definition
	 */
	@SuppressWarnings("unused")
	private static HashMap<String, String> addSlangAndDef(String slangDict)
			throws IOException {

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(slangDict));
		} catch (FileNotFoundException e) {
			System.err
					.println("In changeSlang.java (in addSlangAndDef). Something occurred when creating slangDict file. ERROR: "
							+ e.getMessage());
			System.err.println("slangDict: " + slangDict);
			System.exit(0);
		}

		String line = reader.readLine();
		int value = Integer.parseInt(line);
		String throwaway = reader.readLine();

		HashMap<String, String> list = new HashMap<String, String>();

		String slang = "test";
		String def = "test";

		while (slang != null) {
			slang = reader.readLine();
			def = reader.readLine();
			throwaway = reader.readLine();

			if (slang == null || def == null) {
				break;
			}

			list.put(slang, def);
		}

		reader.close();

		return list;
	}

	/**
	 * Goes through each of the lines in output.csv, finds the tweet and
	 * replaces all slang foound.
	 * 
	 * @param inputname
	 *            output.csv, in the TREC2014 folder
	 * @param slangColl
	 *            slangDict.txt, found in QueryExpansionInternal
	 */
	public static void checkWords(String inputname, String slangColl,
			String testbook) throws IOException {
		// sets up the writer
		BufferedWriter writer = null;
		// sets up the reader
		BufferedReader reader = null;

		try {
			writer = new BufferedWriter(new FileWriter(testbook));
			reader = new BufferedReader(new FileReader(inputname));
		} catch (FileNotFoundException e) {
			System.err
					.println("In changeSlang.java (in checkWords). You probably have to close the file. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		HashMap<String, String> slangDict = null;
		try {
			slangDict = addSlangAndDef(slangColl);
		} catch (IOException e) {
			System.err
					.println("In changeSlang.java (in checkWords). Something happened when creating slang dictionary. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		String next = reader.readLine();
		writer.append(next);
		writer.newLine();

		// adds run
		next = reader.readLine();
		writer.append(next);
		writer.newLine();

		// adds judge
		next = reader.readLine();
		writer.append(next);
		writer.newLine();

		next = reader.readLine();

		// loops through each line
		while (next != null) {
			// splits up the tweets
			String[] line = next.split(",");
			String toCheck = "";

			if (line.length > 2) {
				// sets the variable to be the tweet
				toCheck = line[2];
			}

			ArrayList<String> allWords = new ArrayList<String>();
			for (int h = 0; h < line.length; h++) {
				if (h == 2) {
					StringTokenizer lineSplit = new StringTokenizer(line[h]);
					while (lineSplit.hasMoreTokens()) {
						allWords.add(lineSplit.nextToken());
					}
				}
			}

			toCheck = URLListGen.removeURLs(toCheck);
			toCheck = CommonWords.updatesWord(toCheck);

			StringTokenizer st = new StringTokenizer(toCheck);

			while (st.hasMoreTokens()) {
				String to = st.nextToken();
				String token = to.trim();

				// checks to see if he words are equal
				if (slangDict.containsKey(token)
						|| slangDict.containsKey(token.toLowerCase())) {
					for (int g = 0; g < allWords.size(); g++) {
						String word = updatesWord(allWords.get(g));

						if (allWords.get(g).contains("-")) {
							continue;
						}

						StringTokenizer wordSplit = new StringTokenizer(word);
						while (wordSplit.hasMoreTokens()) {
							word = wordSplit.nextToken();

							if (word.equalsIgnoreCase(token)) {
								int index = allWords.get(g).indexOf(word);
								String begin = allWords.get(g).substring(0,
										index);
								String ending = allWords.get(g).substring(
										index + word.length());
								String temp = begin + slangDict.get(token)
										+ ending;
								allWords.set(g, temp);
							}
						}
					}
				}
			}

			String newLine = "";
			for (int f = 0; f < allWords.size(); f++) {
				newLine += allWords.get(f) + " ";
			}

			if (line.length == 3) {
				writer.append(line[0] + "," + line[1] + "," + newLine);
			} else {
				String finalLine = "";
				for (int d = 0; d < line.length; d++) {
					if (d == 2) {
						finalLine += "," + newLine;
					} else if (d == 0) {
						finalLine = line[d];
					} else {
						finalLine += "," + line[d];
					}
				}

				writer.append(finalLine);
			}

			writer.newLine();

			next = reader.readLine();
			allWords.clear();
		}

		reader.close();
		writer.close();
	}

	/**
	 * Removes certain punctuation from the word.
	 * 
	 * @param currentWord
	 *            the word to be updated
	 * @return an updated word with only numbers/letters
	 */
	public static String updatesWord(String currentWord) {
		if (currentWord.isEmpty() || currentWord == null) {
			System.err
					.println("In changeSlang.java (in updatesWord). currentWord is empty or null!");
			System.exit(0);
		}

		// goes through each letter of the word
		for (int i = 0; i < currentWord.length(); i++) {
			// gets the character at the current spot
			char curLetter = currentWord.toLowerCase().charAt(i);

			// checks if it's not a letter
			if ((curLetter < 97 || curLetter > 122) && curLetter != ' ') {
				// makes sure not to get rid of "'" in the middle of words
				// this way words like "I'm" and "let's" stay intact
				if (curLetter != 39) {
					// checks if it's not a number
					if (curLetter < 48 || curLetter > 57) {
						// if not a letter and number, removes it
						currentWord = currentWord.substring(0, i) + " "
								+ currentWord.substring(i + 1);
					}
				}
			}
		}

		return currentWord;
	}
}