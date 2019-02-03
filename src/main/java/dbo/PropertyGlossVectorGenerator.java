package dbo;

import config.Database;
import org.deeplearning4j.models.word2vec.Word2Vec;
import utils.GenerateModel;
import utils.Utils;
import vector.fastText.FastTextModel;
import vector.glove.GloveModel;
import vector.word2vec.Word2VecModel;
import wordnet.WordNet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 12/17/2018
 */
public class PropertyGlossVectorGenerator {
    private HashMap<String, List<String>> propertyWordsForMeanMap = new LinkedHashMap<>();
    /*public PropertyGlossVectorGenerator() {
        getWordsForProperty();
    }*/

    public void setPropertyWordsForMeanMap(HashMap<String, List<String>> propertyWordsForMeanMap) {
        this.propertyWordsForMeanMap = propertyWordsForMeanMap;
    }

    public void generateVectorModel(String outputModelFile, Word2Vec sourceWordEmbeddingModel) {
        GenerateModel generateModel = new GenerateModel(sourceWordEmbeddingModel, propertyWordsForMeanMap);
        generateModel.generateVectorModel(outputModelFile);
    }

    private HashMap<String, HashMap<String, String>> getPropLabelCommentMapDB() throws SQLException {
        String DISTINCT_PROPERTIES = "SELECT DISTINCT `prop_uri`, `prop_label`, `prop_comment` FROM property ORDER BY `prop_uri` ASC";
        Statement statement = Database.databaseInstance.conn.createStatement();
        java.sql.ResultSet rs = statement.executeQuery(DISTINCT_PROPERTIES);

        HashMap<String, HashMap<String, String>> propLabelCommentMap = new LinkedHashMap<>();
        while (rs.next()) {
            HashMap<String, String> labelCommentMap = new HashMap<>();
            labelCommentMap.put("label", rs.getString("prop_label"));

            String comment = rs.getString("prop_comment");
            if (comment != null && comment.length() > 0) {
                labelCommentMap.put("comment", rs.getString("prop_comment"));
            }
            propLabelCommentMap.put(rs.getString("prop_uri"), labelCommentMap);
        }
        statement.close();
        return propLabelCommentMap;
    }

    private List<String> getGlossIncludedWordsForMeanVector(String labelCommentNoStopWords) {
        List<String> wordsForMean = new ArrayList<>();

//        Adding initially because we need repeated words
        String[] wordSplit = labelCommentNoStopWords.split(" ");
        wordsForMean.addAll(Arrays.asList(wordSplit));


        WordNet wordNet = WordNet.wordNet;
        HashMap<String, String> wordGlossMap = wordNet.getGlossFromString(labelCommentNoStopWords,
                true,
                false);

//            this map will only have repeated words in gloss but not in keys
        for (String word : wordGlossMap.keySet()) {
            String glossFiltered = wordGlossMap.get(word);
            String[] glossSplit = glossFiltered.split(" ");

            wordsForMean.addAll(Arrays.asList(glossSplit));
        }

        return wordsForMean;
    }

    public HashMap<String, List<String>> getPropertyGlossMapFromDB() {
        String query = "SELECT DISTINCT `prop_uri`, `wordnet_gloss` FROM property ORDER BY `prop_uri` ASC";
        HashMap<String, List<String>> propGlossMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String gloss = rs.getString("wordnet_gloss");
                String property = rs.getString("prop_uri");

                String[] glossSplit = gloss.split(", ");
                List<String> glossWords = new ArrayList<>();

                if (glossSplit.length == 0) {
                    glossWords.add(gloss);
                } else {
                    glossWords.addAll(Arrays.asList(glossSplit));
                }
                propGlossMap.put(property, glossWords);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propGlossMap;
    }

    public HashMap<String, List<String>> getPropertySynsetMapFromDB() {
        String query = "SELECT DISTINCT `prop_uri`, `wordnet_synset` FROM property ORDER BY `prop_uri` ASC";
        HashMap<String, List<String>> propGlossMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String synset = rs.getString("wordnet_synset").toLowerCase();
                String property = rs.getString("prop_uri");

                String[] glossSplit = synset.split(", ");
                List<String> glossWords = new ArrayList<>();

                if (glossSplit.length == 0) {
                    glossWords.add(synset);
                } else {
                    glossWords.addAll(Arrays.asList(glossSplit));
                }
                propGlossMap.put(property, glossWords);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propGlossMap;
    }

    public void getWordsForProperty() {
        try {
            HashMap<String, HashMap<String, String>> propLabelCommentMap = getPropLabelCommentMapDB();
            for (String propUri : propLabelCommentMap.keySet()) {
                HashMap<String, String> labelCommentMap = propLabelCommentMap.get(propUri);
                String label = labelCommentMap.get("label").trim();
                String comment = labelCommentMap.get("comment");

                if (comment != null)
                    label = label.join(" ", comment);

                label = label.trim();
                label = Utils.filterAlphaNum(label);

                Utils utils = new Utils();
                label = utils.removeStopWordsFromString(label);

                //get gloss for each word
                propertyWordsForMeanMap.put(propUri, getGlossIncludedWordsForMeanVector(label));
            }
            System.out.println(propertyWordsForMeanMap);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PropertyGlossVectorGenerator pgvg = new PropertyGlossVectorGenerator();
        HashMap<String, List<String>> propGlossMap = pgvg.getPropertyGlossMapFromDB();
        HashMap<String, List<String>> propSynsetMap = pgvg.getPropertySynsetMapFromDB();
        pgvg.setPropertyWordsForMeanMap(propSynsetMap);

        try {
            /*Word2Vec embeddingModel = new Word2VecModel().word2Vec;
            pgvg.generateVectorModel("propSynsetEmbedding_w2v.vec", embeddingModel);*/

            /*Word2Vec embeddingModel = new GloveModel().glove;
            pgvg.generateVectorModel("propSynsetEmbedding_glove.vec", embeddingModel);*/

            Word2Vec embeddingModel = new FastTextModel().fastText;
            pgvg.generateVectorModel("propSynsetEmbedding_ft.vec", embeddingModel);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
