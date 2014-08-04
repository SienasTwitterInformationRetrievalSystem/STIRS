package Query_ExpansionExternal_Wordnet;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PoST {

	String sentence = null;
	ArrayList<Word> words = null;
	ArrayList<Phrase> phrases = null;

	public PoST(String sentence) {
		this.sentence = sentence.trim();
		words = new ArrayList<Word>();
		phrases = new ArrayList<Phrase>();

		// Split up sentence by any of the following punctuation/spaces
		Pattern pattern = Pattern.compile("(\\w)+");
		Matcher matcher = pattern.matcher(this.sentence);
		while (matcher.find()) {
			words.add(new Word(matcher.group()));
		}
	}

	private void tagPhrases() {
		for (int i = 0; i < words.size(); i++) {
			Word word = words.get(i);
			String wordString = word.getWord();
			int size = words.get(i).getPosTags().size();

			if ((size == 0 && Character.isUpperCase(wordString.charAt(0)))
					|| Character.isUpperCase(wordString.charAt(0))
					|| word.getPosTags().contains("noun")) {
				int j = phraseTag(i,
						Character.isUpperCase(wordString.charAt(0)));
				int k = i;
				String newString = "";

				while (k <= j) {
					newString += words.get(k).getWord() + " ";
					k++;
				}

				Phrase newPhrase;
				if (newString.equals("and")) {
					newPhrase = new Phrase(i, j, 0, newString.trim());
				} else {
					newPhrase = new Phrase(i, j, 1, newString);
				}

				if (i != j && !phrases.contains(newPhrase) && j == words.size()) {
					phrases.add(newPhrase);
					break;
				} else if (j != words.size()) {
					phrases.add(newPhrase);
				}

				if (i < j) {
					i = j;
				}
			} else if (wordString.equals("and")) {
				phrases.add(new Phrase(i, i, 0, wordString));
			} else {
				phrases.add(new Phrase(i, i, 1, wordString));
			}
		}
	}

	private int phraseTag(int j, boolean capital) {
		int m, numPos = words.get(j).getPosTags().size();
		String word = words.get(j).getWord();
		char firstLetter = word.charAt(0);

		if ((numPos != 0 && !Character.isUpperCase(firstLetter) && !words
				.get(j).getPosTags().contains("noun"))
				|| ((Character.isLowerCase(firstLetter)) && (!words.get(j)
						.isArticle(word) && !words.get(j).isPreposition(word) && !words
						.get(j).getPosTags().contains("noun")))) {
			return j - 1;
		} else if (j + 1 != words.size()) {
			if (capital && Character.isUpperCase(firstLetter) || !capital
					&& Character.isUpperCase(firstLetter)) {
				m = phraseTag(j + 1, true);
				return m;
			} else if (((!capital && (Character.isLowerCase(firstLetter))
					&& !words.get(j).isArticle() && !words.get(j)
					.isPreposition()) || (capital && (words.get(j).isArticle() || words
					.get(j).isPreposition()))
					&& !Character.isUpperCase(firstLetter))) {
				m = phraseTag(j + 1, false);
				return m;
			} else {
				return j - 1;
			}
		} else if ((Character.isLowerCase(firstLetter)
				&& (!words.get(j).isArticle() || !words.get(j).isPreposition()) && capital)
				|| (Character.isUpperCase(firstLetter) && !capital)) {
			return j - 1;
		} else {
			return j;
		}
	}

	private void getPosTags() {
		for (int i = 0; i < words.size(); i++) {
			Word newWord = words.get(i);
			words.set(i, LucQeWord.determinePos(newWord));
		}
	}

	public String getSentence() {
		return sentence;
	}

	public String getWords() {
		String returnString = "";

		for (Word word : words) {
			returnString += word.toString() + " ";
		}

		return returnString;
	}

	public String getPhrases() {
		return phrases.toString();
	}

	public void tagSentence() {
		getPosTags();
		tagPhrases();
	}

	public static void main(String[] args) {
		while (true) {
			@SuppressWarnings("resource")
			Scanner input = new Scanner(System.in);
			PoST pos = new PoST(input.nextLine());
			System.out.println("Sentence: " + pos.getSentence());
			pos.tagSentence();
			System.out.println(pos.getWords());
			System.out.println(pos.getPhrases());
		}
	}

	public String runPoST() {
		String newQuery = "";
		tagSentence();

		for (Phrase phrase : phrases) {
			newQuery += phrase.getPhrase() + "\t";
		}

		return newQuery;
	}
}