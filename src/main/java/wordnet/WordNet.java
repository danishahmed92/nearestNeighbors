package wordnet;

import config.IniConfig;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * @author DANISH AHMED on 12/17/2018
 */
public class WordNet {
    public static WordNet wordNet;
    private IDictionary dict;
    static {
        try {
            wordNet = new WordNet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WordNet() throws IOException {
        String path = IniConfig.configInstance.wordNet;
        URL url = null;
        try {
            url = new URL("file", null, path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null)
            return;

        dict = new Dictionary(url);
        dict.open();
    }

    public String getVerbForNoun(String noun) {
        IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);
        try {
            IWordID wordID = idxWord.getWordIDs().get(0);
            IWord word = dict.getWord(wordID);
            String nounLemma = word.getLemma();

            for (IWordID iWordID : word.getRelatedWords()) {
                IWord relWord = dict.getWord(iWordID);
                if (relWord.getPOS() == POS.VERB) {
                    String verb = relWord.toString().split("-")[4];

                    IIndexWord iinWord = dict.getIndexWord(verb, POS.VERB);
                    IWordID iwordID = iinWord.getWordIDs().get(0);
                    IWord iword = dict.getWord(iwordID);

                    return iword.getLemma();
                }
            }
            return nounLemma;
        } catch (NullPointerException ne) {
            return noun;
        }
    }

    public POS getBestPOS(String word) {
        try {
            IIndexWord iwN = dict.getIndexWord(word, POS.NOUN);
            int iwNSenseCount = (iwN == null || iwN.getWordIDs() == null || iwN.getWordIDs().size() == 0) ?
                    0 : iwN.getTagSenseCount();

            IIndexWord iwV = dict.getIndexWord(word, POS.VERB);
            int iwVSenseCount = (iwV == null || iwV.getWordIDs() == null || iwV.getWordIDs().size() == 0) ?
                    0 : iwV.getTagSenseCount();

            IIndexWord iwAdv = dict.getIndexWord(word, POS.ADVERB);
            int iwAdvSenseCount = (iwAdv == null || iwAdv.getWordIDs() == null || iwAdv.getWordIDs().size() == 0) ?
                    0 : iwAdv.getTagSenseCount();

            IIndexWord iwAdj = dict.getIndexWord(word, POS.ADJECTIVE);
            int iwAdjSenseCount = (iwAdj == null || iwAdj.getWordIDs() == null || iwAdj.getWordIDs().size() == 0) ?
                    0 : iwAdj.getTagSenseCount();

            int maxOccur = -1;
            POS pos = null;
            if (iwNSenseCount > maxOccur) {
                if (iwN != null)
                    pos = iwN.getPOS();
                maxOccur = iwNSenseCount;
            }
            if (iwVSenseCount > maxOccur) {
                if (iwV != null)
                    pos = iwV.getPOS();
                maxOccur = iwVSenseCount;
            }
            if (iwAdvSenseCount > maxOccur) {
                if (iwAdv != null)
                    pos = iwAdv.getPOS();
                maxOccur = iwAdvSenseCount;
            }
            if (iwAdjSenseCount > maxOccur) {
                if (iwAdj != null)
                    pos = iwAdj.getPOS();
                maxOccur = iwAdjSenseCount;
            }
            return pos;
        } catch (IllegalArgumentException iae) {
            System.out.println(word);
            return null;
        }
    }

    private IWord getIWord(String word, POS pos) {
        IIndexWord idxWord = dict.getIndexWord(word, pos);
        if (idxWord.getWordIDs() == null || idxWord.getWordIDs().size() == 0)
            return null;

        IWordID wordID = idxWord.getWordIDs().get(0);
        return dict.getWord(wordID);
    }

    public String getLemma(String word, POS pos) {
        IWord iword = getIWord(word, pos);
        if (iword == null)
            return word;

        return iword.getLemma().length() > 0 ? iword.getLemma() : word;
    }

    public String getLemma(String word){
        POS pos = getBestPOS(word);
        if (pos == null)
            return word;
        return getLemma(word, pos);
    }

    public String getGloss(String word, POS pos) {
        IWord iword = getIWord(word, pos);
        if (iword == null)
            return null;

        return iword.getSynset().getGloss();
    }

    public String getGloss(String word) {
        POS pos = getBestPOS(word);
        if (pos == null)
            return null;

        return getGloss(word, pos);
    }

    public HashMap<String, String> getGlossFromString(String wordsSpaceSeparated,
                                                      boolean removeStopWordsFromGloss,
                                                      boolean exampleSentence) {
        String[] words = wordsSpaceSeparated.split(" ");
        HashMap<String, String> wordGlossMap = new HashMap<>();
        for (String word : words) {
            if (!wordGlossMap.containsKey(word)) {
                if (!removeStopWordsFromGloss)
                    wordGlossMap.put(word, getGloss(word));
                else {
                    Utils utils = new Utils();

                    String gloss = getGloss(word);
                    if (gloss != null) {
                        if (!exampleSentence) {
                            if (gloss.contains(";")) {
                                String[] glossSplit = gloss.split(";");
                                gloss = glossSplit[0];
                            }
                        }

                        gloss = Utils.filterAlphaNum(gloss);
                        gloss = utils.removeStopWordsFromString(gloss);
                        if (gloss.length() > 0)
                            wordGlossMap.put(word, gloss.trim());
                    }
                }
            }
        }
        return wordGlossMap;
    }

    public static void main(String[] args) {
        WordNet wordNet = WordNet.wordNet;
        String word = "advisor";
        POS pos = wordNet.getBestPOS(word);

        System.out.println(word.toUpperCase());
        System.out.println("POS:\t" + wordNet.getBestPOS(word));
        System.out.println("Lemma:\t" + wordNet.getLemma(word, pos));
        System.out.println("Gloss:\t" + wordNet.getGloss(word, pos));
    }
}
