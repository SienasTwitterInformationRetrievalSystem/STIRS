package Query_ExpansionExternal_Google;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import LinkCrawling.Links;
import LinkCrawling.URLListGen;

/**
 * This class is used to remove all unwanted words from a given text file or
 * query. It will find stop words, articles and prepositions, in order to
 * determine the importance of that word.
 * 
 * @author Chan Tran v1.0
 * @author Lauren Mathews v2.0
 * @version 7/21/2011 v1.0
 * @version 6/24/2014 v2.0
 */
public class Article {

	private ArrayList<String> wantedQuery = new ArrayList<String>();
	private String wanted;
	private static String stopWordsColl;

	public Article(String stopWords) {
		// checking arguments
		if (stopWords.isEmpty() || stopWords == null) {
			System.err
					.println("In Article.java (in constructor). stopWorsColl doesn't contain file path.");
			System.exit(0);
		}

		Article.stopWordsColl = stopWords;
	}

	/**
	 * This method takes in a query from the parameter. It will split up the
	 * query into individual words. An ArrayList of String will be used to store
	 * the individual words. If any of the words contains a stop word in the
	 * StopWord class, it will be removed from the ArrayList. It will return the
	 * ArrayList with unnecessary words taken out (if any).
	 */
	public String notWantedQuery(String query, String topic, String url) {
		wantedQuery.clear();
		StringTokenizer token = new StringTokenizer(query, " ");

		ArrayList<String> queries = new ArrayList<String>();
		StringTokenizer sT = new StringTokenizer(topic, "+");

		// creates a customized list of the topic query
		// this list contains variations of each word in topic
		while (sT.hasMoreTokens()) {
			String curTok = sT.nextToken().toLowerCase().trim();

			// always adds the normal word
			queries.add(curTok);

			// if it contains ", removes it
			if (curTok.contains("\"")) {
				queries.add(curTok.replace("\"", ""));
			}

			// if it contains an ', removes it
			if (curTok.contains("'")) {
				queries.add(curTok.substring(0, curTok.indexOf("'")));
			} else {
				queries.add(curTok + "'s");
			}

			// if "s", removes it, else adds it
			if (curTok.endsWith("s")) {
				queries.add(curTok.substring(0, curTok.length() - 1));
			} else {
				queries.add(curTok + "s");
			}
		}

		// adds all words from document
		while (token.hasMoreTokens()) {
			wantedQuery.add(token.nextToken());
		}

		// makes all words without punctuation
		for (int i = 0; i < wantedQuery.size(); i++) {
			String newWord = updatesWord(wantedQuery.get(i));

			if (!newWord.isEmpty()) {
				wantedQuery.set(i, newWord);
			} else {
				wantedQuery.remove(i);
				i--;
			}
		}
		
		//removes all non-alphanumeric
		for (int i = 0; i < wantedQuery.size(); i++) {
			if(!Links.isAlphaNumeric(wantedQuery.get(i))){
				wantedQuery.remove(i);
				i--;
			}
		}

		// Removes any words from the StopWords class under the constant LONG.
		for (int i = 0; i < StopWords.LONG.length; i++) {
			for (int j = 0; j < wantedQuery.size(); j++) {
				if (wantedQuery.get(j).equalsIgnoreCase(StopWords.LONG[i])) {
					wantedQuery.remove(j);
					j--;
				}
			}
		}

		// Removes any symbols from StopWords class under the constant SYMBOLS.
		for (int i = 0; i < StopWords.SYMBOLS.length; i++) {
			for (int j = 0; j < wantedQuery.size(); j++) {
				if (wantedQuery.get(j).equalsIgnoreCase(StopWords.SYMBOLS[i])) {
					wantedQuery.remove(j);
					j--;
				}
			}
		}

		// used for removing words that have the url title in it
		String newUrl = "";
		if (!url.isEmpty()) {
			if (url.contains("www.")) {
				newUrl = url.substring(url.indexOf("www.") + 4);
				newUrl = newUrl.substring(0, newUrl.indexOf("."));
			} else {
				newUrl = url.substring(url.indexOf("//") + 2);
				newUrl = newUrl.substring(0, newUrl.indexOf("."));
			}
		}

		HashSet<String> stopWords = createStopWords();
		for (int i = 0; i < wantedQuery.size(); i++) {
			// removes all stop words
			if (stopWords.contains(wantedQuery.get(i))) {
				wantedQuery.remove(i);
				i--;
				continue;
			}

			// removes words with the url in it
			if (newUrl.contains(wantedQuery.get(i))) {
				wantedQuery.remove(i);
				i--;
				continue;
			}

			if (URLListGen.containsUrl(wantedQuery.get(i))) {
				wantedQuery.remove(i);
				i--;
			}
		}

		// removes all words that are already in query
		for (int i = 0; i < wantedQuery.size(); i++) {
			for (int j = 0; j < queries.size(); j++) {
				if (wantedQuery.get(i).equalsIgnoreCase(queries.get(j))) {
					wantedQuery.remove(i);
					i--;
					break;
				}
			}
		}

		wanted = "";

		for (int i = 0; i < wantedQuery.size(); i++) {
			if (i == wantedQuery.size() - 1) {
				wanted += wantedQuery.get(i);
			} else {
				wanted += wantedQuery.get(i) + " ";
			}
		}

		return wanted;
	}

	/**
	 * Removes certain punctuation from the word.
	 * 
	 * @param currentWord
	 *            the word to be updated
	 * @return an updated word with only numbers/letters
	 */
	private static String updatesWord(String currentWord) {
		if (currentWord.isEmpty() || currentWord == null) {
			System.err
					.println("In Article.java (in updatesWord). currentWord is empty or null!");
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
				if (curLetter == 39
						&& (currentWord.lastIndexOf(curLetter) != currentWord
								.length() - 1)
						&& (currentWord.indexOf(curLetter) != 0)) {
					// checks if not a number
				} else if (curLetter < 48 || curLetter > 57) {
					currentWord = currentWord.substring(0, i)
							+ currentWord.substring(i + 1);

					// checks to make sure last character still isn't "'"
					if ((currentWord.lastIndexOf("'") == currentWord.length() - 1)
							&& currentWord.length() > 2) {
						currentWord = currentWord.substring(0,
								currentWord.length() - 1);
					}
				}
			}
		}

		return currentWord;
	}

	/**
	 * Creates an arraylist based off of the words in englishStop.txt
	 * 
	 * @param stopWordsColl
	 *            The full file path of the file (englishStop.txt)
	 * @return an arraylist containing the words in the englishStop.txt file
	 */
	private static HashSet<String> createStopWords() {
		// will contain the list of stop words
		HashSet<String> stopWords = new HashSet<String>();

		// reads in list of all stop words (englishStop.txt)
		try {
			BufferedReader bfr = new BufferedReader(new FileReader(
					stopWordsColl));

			String inputLine = bfr.readLine();
			// adds in all words to arraylist
			while (inputLine != null) {
				stopWords.add(inputLine.toLowerCase().trim());
				inputLine = bfr.readLine();
			}

			bfr.close();
		} catch (FileNotFoundException e) {
			System.out
					.println("In Article.java (in createStopWords). englishStop.txt not found. Java error: "
							+ e.getMessage());
			System.exit(0);
		} catch (IOException e) {
			System.out
					.println("In Article.java (in createStopWords). An error occurred created stopWords. Error: "
							+ e.getMessage());
			System.exit(0);
		}

		return stopWords;
	}
}