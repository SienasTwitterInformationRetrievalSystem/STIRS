package Query_ExpansionExternal_Wordnet;

public class Phrase {
	int begin = -1, end = -1, rank = -1;
	String phrase = null;

	Phrase(int begin, int end, int rank, String phrase) {
		this.begin = begin;
		this.end = end;
		this.phrase = phrase;
		this.rank = rank;
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	public String getPhrase() {
		return phrase;
	}

	public int getRank() {
		return rank;
	}

	public String toString() {
		return "Phrase [begin=" + begin + ", end=" + end + ", phrase=" + phrase
				+ ", rank=" + rank + "]";
	}
}