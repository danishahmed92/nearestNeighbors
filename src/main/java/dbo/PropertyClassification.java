package dbo;

import config.Database;
import config.IniConfig;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import vector.VectorModel;
import vector.VectorModelUtils;

import javax.validation.constraints.Null;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 1/27/2019
 */
public class PropertyClassification {
    public Word2Vec sourceModel;
    public Word2Vec generatedModel;

    public static PropertyClassification propertyClassificationInstance;

    static {
        propertyClassificationInstance = new PropertyClassification();
    }

    private PropertyClassification() {
        VectorModel vectorModelSource = new VectorModel();
        VectorModel vectorModelGenerated = new VectorModel();

        IniConfig config = IniConfig.configInstance;
        sourceModel = vectorModelSource.setVectorModel(config.word2vec);
        generatedModel = vectorModelGenerated.setVectorModel(config.propertySynsetW2V);
//        generatedModel = vectorModelGenerated.setVectorModel(config.propertyGlossFT);
    }

    public HashMap<String, Double> getNearestProperties(List<String> wordList, int limit) {
        try {
            INDArray wordListVecMean = VectorModelUtils.getMeanVecFromWordList(sourceModel, wordList);

            //        the above calculated mean vector will be compared against generated model to get nearest property
            VectorModelUtils modelUtils = new VectorModelUtils();
            return modelUtils.vectorNearest(generatedModel, wordListVecMean, limit);
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public double getSimilarityOfWordsWithProperty(List<String> wordList, String property) {
        try {
            INDArray wordListVecMean = VectorModelUtils.getMeanVecFromWordList(sourceModel, wordList);
            return VectorModelUtils.getSimilarity(generatedModel, property, wordListVecMean);
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public HashMap<String, Double> getNearestPropertiesFromWord(String word, int limit) {
        INDArray wordVec = sourceModel.getWordVectorMatrix(word);
        if (wordVec == null)
            return null;

        VectorModelUtils modelUtils = new VectorModelUtils();
        return modelUtils.vectorNearest(generatedModel, wordVec, limit);
    }

    public HashMap<Integer, HashMap<String, String>> getOKEPatterns() {
        String selectQuery = "SELECT id_oke_pattern, orig_root, root_lemma, dist_nouns from `oke_orig_pattern` " +
                "ORDER BY id_oke_pattern;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okePatternMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int sentenceId = rs.getInt("id_oke_pattern");

                HashMap<String, String> detailMap = new HashMap<>();
                detailMap.put("origRoot", rs.getString("orig_root"));
                detailMap.put("rootLemma", rs.getString("root_lemma"));
                detailMap.put("nouns", rs.getString("dist_nouns"));

                okePatternMap.put(sentenceId, detailMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okePatternMap;
    }

    public static void main(String[] args) {
        PropertyClassification pc = PropertyClassification.propertyClassificationInstance;
        HashMap<Integer, HashMap<String, String>> okePatternMap = pc.getOKEPatterns();

        for (int patternId : okePatternMap.keySet()) {
            HashMap<String, String> detailMap = okePatternMap.get(patternId);
            String nouns = detailMap.get("nouns");
            nouns = nouns.substring(1, nouns.length()-1);
            nouns = nouns.replaceAll("-", ", ");
            nouns = nouns.replaceAll("_", ", ");

            List<String> roots = new ArrayList<>();
            roots.add(detailMap.get("origRoot"));
            roots.add(detailMap.get("rootLemma"));

            HashMap<String, Double> rootPropertyScore = pc.getNearestProperties(roots, 3);
            HashMap<String, Double> nounPropertyScore = new LinkedHashMap<>();

            System.out.println(patternId);
            System.out.println(rootPropertyScore);

            if (nouns.length() > 0) {
                String[] nounsSplit = nouns.split(", ");
                List<String> nounsList = Arrays.asList(nounsSplit);

                nounPropertyScore = pc.getNearestProperties(nounsList, 3);

                roots.addAll(nounsList);
                HashMap<String, Double> mergeRootNounScore = pc.getNearestProperties(roots, 3);

                System.out.println(nounPropertyScore);
                System.out.println("Merged:\t" + mergeRootNounScore);
            }
            StoreEmbeddingOKE.storeScore("oke_score_glove", patternId, rootPropertyScore, nounPropertyScore);
            System.out.println();
        }
    }
}
