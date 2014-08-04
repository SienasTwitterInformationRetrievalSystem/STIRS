package Query_ExpansionExternal_Wordnet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.*;

import API.LuceneQuery;
import LinkCrawling.Links;

import edu.smu.tspell.wordnet.*;

/**
 * This class utilizes QueryExpansion by using WordNet to find synonyms of
 * verbs. Each part of speech is displayed along with definitions. Duplicate
 * synonyms are removed and the user's query will display all of the words and
 * phrases within the WordNet library.
 * 
 * Version 1.5: Created several updates. Noticed a tread in [noun, verb] for
 * words; always better to return the verb, if possible, instead of noun. Added
 * in a stopWords collection - and words in query that were considered a
 * "stop word" skipped.
 * 
 * @author Carl Tompkins & Chan Tran v1.0
 * @author Lauren Mathews v1.5
 * @version 7/21/2011 v1.0
 * @version 6/18/2014 v1.5
 */
public class LucQeWord{
	private final static Logger LOGGER = Logger.getLogger(LucQeWord.class
			.getName());
	static WordNetDatabase database;
	NounSynset noun;
	VerbSynset verb;
	AdjectiveSynset adj;
	AdjectiveSynset[] pertainyms;
	AdverbSynset adv;
	WordSense[] sense;
	Pattern pattern = null;
	Matcher matcher;
	ArrayList<String> allSyn, duplicate;
	ArrayList<LuceneQuery> lucQueryArray = new ArrayList<LuceneQuery>();
	String luceneQuery;
	LuceneQuery luc;
	Logger logger = null;

	/**
	 * This empty constructor calls the findDirectory method in order to set up
	 * the WordNet dictionary.
	 */
	public LucQeWord() {
		findDirectory();
	}

	/**
	 * This constructor calls the findDirectory method in order to set up the
	 * WordNet dictionary and copies the ArrayList of LuceneQuery to a new
	 * ArrayList of LuceneQuery in order to be used throughout this class.
	 */
	public LucQeWord(ArrayList<LuceneQuery> luceneArray, Logger logger) {
		findDirectory();

		for (int i = 0; i < luceneArray.size(); i++) {
			luc = luceneArray.get(i);
			lucQueryArray.add(luc);
		}

		this.logger = logger;
	}

	/**
	 * This method sets up the user's directory. It will add the dictionary to
	 * LucQeWord class in order to use WordNet's library correctly. The user
	 * must place the dictionary inside his/her Eclipse workspace folder to
	 * enable the use of the WordNet dictionary.
	 */
	private static void findDirectory() {
		//try downloading WordNet 2.1 and linking to it here
		File dirFile = new File("/home/lmathews/workspace/TREC2014/src/Query_ExpansionExternal_Wordnet/WordNet/2.1");
		String path = dirFile.toString();
		String filePath = path + "/dict/";
		System.setProperty("wordnet.database.dir", filePath);
		database = WordNetDatabase.getFileInstance();
	}

	/**
	 * This method finds the synonyms of the given query by looking through the
	 * WordNet dictionary. Each part of speech (adjective, adverb, noun, and
	 * verb) can be displayed and synonyms will go along with each word.
	 */
	public void phraseSynonyms(String query, Word pos) {
		allSyn = new ArrayList<String>();
		String adjectiveSt;
		String adverbSt;
		String nounSt;
		String verbSt;

		if (pos.getPosTags().contains("noun")
				&& pos.getPosTags().contains("verb")
				&& !(pos.getPosTags().contains("adverb") || pos.getPosTags()
						.contains("adjective"))) {
			verbSt = verbs(query);

			if (verbSt.isEmpty()) {
				nounSt = nouns(query);

				wordFinder(nounSt, "noun");
			} else {
				wordFinder(verbSt, "verb");
			}
		} else {

			// Find synonyms for adjectives.
			if (pos.getPosTags().contains("adjective")) {
				adjectiveSt = adjectives(query);

				wordFinder(adjectiveSt, "adjective");
			}

			// Find synonyms for adverbs.
			if (pos.getPosTags().contains("adverb")) {
				adverbSt = adverbs(query);

				wordFinder(adverbSt, "adverb");
			}

			// Find synonyms for nouns.
			if (pos.getPosTags().contains("noun")) {
				nounSt = nouns(query);

				wordFinder(nounSt, "noun");
			}

			// Find synonyms for verbs.
			if (pos.getPosTags().contains("verb")) {
				verbSt = verbs(query);

				wordFinder(verbSt, "verb");
			}
		}
	}

	/**
	 * This method finds the synonyms of the given query by looking through the
	 * WordNet dictionary. Each part of speech (adjective, adverb, noun, and
	 * verb) can be displayed and synonyms will go along with each word. If the
	 * query contains more than one word, it will split up the phrase into
	 * individual words and search synonyms for each word. It will also search
	 * for synonyms of the query itself.
	 */
	public void wordPhraseSynonyms(String query) {
		allSyn = new ArrayList<String>();
		StringTokenizer strtok = new StringTokenizer(query, " ");
		int numTokens = 0;
		int numLoops = 0;
		String adjectiveSt;
		String adverbSt;
		String nounSt;
		String verbSt;

		// Splits the query into individual tokens. (ie. "machine gun" will be
		// divided into "machine" and "gun")
		while (strtok.hasMoreTokens()) {
			String term = strtok.nextToken();
			// Counts the number of tokens in the query after the first token
			// only once.
			if (numLoops == 0)
				numTokens = strtok.countTokens();

			// Find synonyms for adjectives.
			adjectiveSt = adjectives(term);
			wordFinder(adjectiveSt, "adjective");

			// Find synonyms for adverbs.
			adverbSt = adverbs(term);
			wordFinder(adverbSt, "adverb");

			// Find synonyms for nouns.
			nounSt = nouns(term);
			wordFinder(nounSt, "noun");

			// Find synonyms for verbs.
			verbSt = verbs(term);
			wordFinder(verbSt, "verb");

			numLoops++;
		}

		// Find synonyms for adj, adv, noun, and verb if query contains more
		// than one word. If the number of tokens
		// exceed 0, this if statement will run in order to read the entire
		// query as a whole.
		if (numTokens > 0) {
			// Find synonyms for adjectives.
			adjectiveSt = adjectives(query);
			wordFinder(adjectiveSt, "adjective");

			// Find synonyms for adverbs.
			adverbSt = adverbs(query);
			wordFinder(adverbSt, "adverb");

			// Find synonyms for nouns.
			nounSt = nouns(query);
			wordFinder(nounSt, "noun");

			// Find synonyms for verbs.
			verbSt = verbs(query);
			wordFinder(verbSt, "verb");
		}
	}

	/**
	 * This method returns all of the adjectives found according to the given
	 * query.
	 */
	private String adjectives(String query) {
		String var = "";
		Synset[] synsets = database.getSynsets(query, SynsetType.ADJECTIVE);

		for (int i = 0; i < synsets.length; i++) {
			adj = (AdjectiveSynset) (synsets[i]);
			var += adj;
			pertainyms = adj.getSimilar();
			for (int j = 0; j < pertainyms.length; j++) {
				var += pertainyms[j] + "\n";
			}
		}

		return var;
	}

	/**
	 * This method returns all of the adverbs according to the given query.
	 */
	private String adverbs(String query) {
		String var = "";
		Synset[] synsets = database.getSynsets(query, SynsetType.ADVERB);

		for (int i = 0; i < synsets.length; i++) {
			adv = (AdverbSynset) (synsets[i]);
			var += adv;
			sense = adv.getPertainyms(query);
			for (int j = 0; j < sense.length; j++) {
				var += sense[j].toString() + "\n";
			}
		}

		return var;
	}

	/**
	 * This method returns all of the nouns according to the given query.
	 */
	private String nouns(String query) {
		if (!Character.isUpperCase(query.charAt(0))) {
			String var = "";
			Synset[] synsets = database.getSynsets(query, SynsetType.NOUN);

			for (int i = 0; i < synsets.length; i++) {
				noun = (NounSynset) (synsets[i]);
				String definition = noun.getDefinition();

				if (query.compareTo(noun.getWordForms()[0]) > 1
						|| query.compareTo(noun.getWordForms()[0]) < 0) {
					var += noun.getWordForms()[0] + " - " + definition + "\n";
					StringTokenizer strtok = new StringTokenizer(definition,
							" ");

					while (strtok.hasMoreTokens()) {
						String wordInDef = strtok.nextToken();
						PoST pos = new PoST(wordInDef);
						wordInDef = pos.runPoST();
						StringTokenizer str = new StringTokenizer(wordInDef,
								"\t;");
						while (str.hasMoreTokens()) {
							String temp = str.nextToken().trim();
							char first = temp.charAt(0);
							if (Character.isUpperCase(first))
								var += temp + " ";
						}
					}
				}
			}
			return var;
		}
		return null;
	}

	/**
	 * This method returns all of the verbs according to the given query.
	 */
	private String verbs(String query) {
		String var = "";
		Synset[] synsets = database.getSynsets(query, SynsetType.VERB);

		for (int i = 0; i < synsets.length; i++) {
			verb = (VerbSynset) (synsets[i]);
			if (query.compareTo(verb.getWordForms()[0]) > 1) {
				var += verb.getWordForms()[0] + " - " + verb.getDefinition()
						+ "\n";
			}
		}

		return var;
	}

	/**
	 * This method uses pattern analysis in order to read the given output. It
	 * will then re-order the output to look neater for the user.
	 */
	private void wordFinder(String wordNetString, String pos) {
		if (wordNetString != null) {
			// This is the pattern for nouns and verbs.
			if ("noun".equals(pos.toLowerCase())
					|| "verb".equals(pos.toLowerCase())) {
				// This catches any pattern that looks like:
				// (word+\t+definition+\n)
				// (word + tab + definition + new line...)
				pattern = Pattern
						.compile("([A-Za-z \\t0-9()':;,=\\/?.^-]){1,}");
				matcher = pattern.matcher(wordNetString);
			} else {
				// This catches any pattern that looks like:
				// (Adjective@123456[word] - definition)
				// (source@numericValue + [word] + definition)
				// It will ignore the source & value and return only the word
				// and definition. (ie. [word] - definition)
				pattern = Pattern
						.compile("(\\[[a-zA-Z, -:()'=\\.;]*\\])([- a-z()\\.:,'; -]*)");
				matcher = pattern.matcher(wordNetString);
			}

			// When the match is found, this loop will replace and change the
			// style of the word and definition.
			while (matcher.find()) {
				String synonyms = matcher.group();

				/*
				 * Replaces any brackets with empty quotes, replaces any dash
				 * with a tab, and replaces any closing parenthesis with a tab.
				 */
				synonyms = synonyms.replaceFirst("\\[", "");
				synonyms = synonyms.replaceFirst("\\]", "");
				synonyms = synonyms.replaceFirst(" - ", "\t");

				StringTokenizer tok = new StringTokenizer(synonyms, "\t");
				ArrayList<String> list = new ArrayList<String>();
				while (tok.hasMoreTokens()) {
					String words = tok.nextToken().trim();
					StringTokenizer getSynonyms = new StringTokenizer(words,
							",");
					String actualWord = getSynonyms.nextToken();
					allSyn.add(actualWord);

					while (getSynonyms.hasMoreTokens()) {
						list.add(getSynonyms.nextToken());
					}

					for (int i = 0; i < list.size(); i++) {
						allSyn.add(list.get(i));
					}

					if (tok.hasMoreTokens()) {
						@SuppressWarnings("unused")
						Word word = new Word(actualWord,
								tok.nextToken().trim(), list);
					}
				}
			}
		}
	}

	/**
	 * Removes all of the duplicated synonyms and creates a new ArrayList of
	 * Strings that contains all of the synonyms for the given query.
	 */
	public String getAllSynonyms() {
		String out = new String();
		duplicate = new ArrayList<String>();
		for (int i = 0; i < allSyn.size(); i++)
			duplicate.add(allSyn.get(i));
		for (int i = 0; i < allSyn.size(); i++) {
			for (int j = 0; j < allSyn.size(); j++) {
				if (allSyn.get(i).equals(allSyn.get(j)) && i != j) {
					allSyn.remove(j);
					i = 0;
					j = 0;
				}
			}
		}

		for (int i = 0; i < allSyn.size(); i++) {
			if (i == allSyn.size() - 1)
				out += allSyn.get(i);
			else
				out += allSyn.get(i) + ", ";
		}
		return out;
	}

	/**
	 * This is the main method to run LucQeWord class.
	 */
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner input = new Scanner(System.in);
		while (true) {
			String userInput = input.nextLine();
			ArrayList<LuceneQuery> lucQuery = new ArrayList<LuceneQuery>();
			lucQuery.add(new LuceneQuery("897", userInput, "e8w9r",
					(long) 389723));
			LucQeWord newLuc = new LucQeWord(lucQuery, LOGGER);
			lucQuery = newLuc.runWordNet(null);
		}
	}

	/**
	 * This method is used in coherence with STIRS. This is used to run the
	 * WordNet database and find synonyms for a given query. It will return back
	 * an ArrayList of LuceneQuery with modifications based on the found
	 * synonyms. It will add the given synonyms into the ArrayList in
	 * conjunction with the original ArrayList.
	 */
	public ArrayList<LuceneQuery> runWordNet(HashSet<String> stopWords) {
		Word comparison = new Word();

		for (int i = 0; i < lucQueryArray.size(); i++) {
			luc = lucQueryArray.get(i);
			luceneQuery = luc.getQuery();
			// PoST pos = new PoST(luceneQuery);
			// luc.setQuery(pos.runPoST());

			StringTokenizer indiv = new StringTokenizer(luc.getQuery(), " ");
			String indivWord = "";
			String all = "";
			while (indiv.hasMoreTokens()) {
				indivWord = indiv.nextToken().trim().toLowerCase();

				if (stopWords != null) {
					if (stopWords.contains(indivWord)) {
						continue;
					}
				}
				
				if(!Links.isAlphaNumeric(indivWord)){
					continue;
				}

				while (indivWord.contains("\"")) {
					String temp = indiv.nextToken().trim().toLowerCase();
					indivWord = indivWord + " " + temp;

					if (temp.contains("\"")) {
						break;
					}
				}

				Word pos = determinePos(new Word(indivWord));
				String newWord = comparison.compareTest(indivWord, pos);

				if (!newWord.isEmpty()) {
					if (!(indivWord.equalsIgnoreCase(newWord)
							|| luceneQuery.contains(newWord) || all
								.contains(newWord))) {
						all += "\"" + newWord + "\" ";
					}
				}
			}

			luc.setQuery(all.replace("\t", ""));
			lucQueryArray.set(i, luc);
			logger.fine("Expanded query for queryNum: " + (i + 1));
		}

		return lucQueryArray;
	}

	/**
	 * determinePos gives POS tags to a Word object
	 * 
	 * @param newWord
	 *            The word to get POS tags for
	 * @return An updated word object that contains the possible POS tags
	 */
	public static Word determinePos(Word newWord) {
		Synset[] synsets = setStaticMode(newWord);
		for (int i = 0; i < synsets.length; i++) {
			int typeNum = Integer.parseInt(synsets[i].getType().toString());
			switch (typeNum) {
			case 1:
				if (!newWord.getPosTags().contains("noun")) {
					newWord.addPosTag("noun");
				}
				break;
			case 2:
				if (!newWord.getPosTags().contains("verb")) {
					newWord.addPosTag("verb");
				}
				break;
			case 3:
				break;
			case 5:
				if (!newWord.getPosTags().contains("adjective")) {
					newWord.addPosTag("adjective");
				}
				break;
			case 4:
				if (!newWord.getPosTags().contains("adverb")) {
					newWord.addPosTag("adverb");
				}

				break;
			}
		}
		return newWord;
	}

	public static String getDefinition(Word newWord) {
		String definition = null;
		@SuppressWarnings("unused")
		Synset[] synsets = setStaticMode(newWord);
		return definition;
	}

	private static Synset[] setStaticMode(Word newWord) {
		findDirectory();
		String term = newWord.getWord();
		return database.getSynsets(term);
	}
}