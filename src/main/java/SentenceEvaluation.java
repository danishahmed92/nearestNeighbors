import config.Database;

import java.io.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 2/12/2019
 */
public class SentenceEvaluation {
    public HashMap<Integer, HashMap<String, String>> entityPredictionMap = new LinkedHashMap<>();
    public HashMap<String, Double> propLowerThreshold = new HashMap<>();

    public SentenceEvaluation(String evaluationResultFile, double alpha, double beta) {
        setEntityScoresFromResultFile(evaluationResultFile);
        setThresholdMap(alpha, beta);

        removeEntityPropertiesBelowThreshold();
    }

    private void removeEntityPropertiesBelowThreshold() {
        Set<Integer> removeEntityIds = new HashSet<>();
        for (int entityId : entityPredictionMap.keySet()) {
            HashMap<String, String> predictionMap = entityPredictionMap.get(entityId);
            String propertyPrediction = predictionMap.get("property");
            double score = Double.parseDouble(predictionMap.get("score"));

            if (propLowerThreshold.containsKey(propertyPrediction.toLowerCase())) {
                double threshold = propLowerThreshold.get(propertyPrediction.toLowerCase());
                if (score < threshold)
                    removeEntityIds.add(entityId);
            }
        }

        for (int entityId : removeEntityIds) {
            entityPredictionMap.remove(entityId);
        }
    }

    public Set<Integer> getSentIdOKE() {
        String selectQuery = "SELECT oks.id_oke_sent from oke_sent oks\n" +
                "INNER JOIN oke_triples okt ON oks.sentence = okt.sentence\n" +
                "WHERE okt.prop_uri NOT IN (\"location\", \"country\")\n" +
                "GROUP BY oks.id_oke_sent\n" +
                "ORDER BY oks.id_oke_sent;";

        Statement statement = null;
        Set<Integer> sentenceIdSet = new LinkedHashSet<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next())
                sentenceIdSet.add(rs.getInt("id_oke_sent"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sentenceIdSet;
    }

    public HashMap<Integer, String> getEntitiesFromSentId(int sentId) {
        String selectQuery = String.format("SELECT okcp.id_oke_sent_entity, okse.subj, okse.obj from oke_coref_pattern okcp\n" +
                "INNER JOIN oke_coref okc ON okc.id_oke_coref = okcp.id_oke_coref \n" +
                "INNER JOIN oke_sent_entity okse ON okse.id_oke_sent_entity = okcp.id_oke_sent_entity\n" +
                "WHERE okc.id_oke_sent = %d;", sentId);

        HashMap<Integer, String> entitySubjObjMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                String subject = rs.getString("subj");
                String object = rs.getString("obj");
                String subjObj = subject + "|" + object;

                entitySubjObjMap.put(rs.getInt("id_oke_sent_entity"), subjObj);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entitySubjObjMap;
    }

    public void setThresholdMap(double alpha, double beta) {
        String selectQuery = String.format("select prop_uri_lower, mean, sd, variance from property_threshold where alpha = %.1f and beta = %.1f;",
                alpha, beta);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                double mean = rs.getDouble("mean");
                double sd = rs.getDouble("sd");
                double variance = rs.getDouble("variance");

                double threshold = mean - (sd + variance);
                propLowerThreshold.put(rs.getString("prop_uri_lower"), threshold);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, List<String>> getOKEPropertiesForSentence(int sentenceId) {
        String selectQuery = String.format("SELECT okt.prop_uri, okt.subj_label, okt.obj_label from oke_triples okt\n" +
                "INNER JOIN oke_sent oks ON oks.sentence = okt.sentence\n" +
                "WHERE okt.prop_uri NOT IN (\"location\", \"country\")\n" +
                "AND oks.id_oke_sent = %d " +
                "GROUP BY okt.prop_uri, okt.subj_label, okt.obj_label;", sentenceId);
        HashMap<String, List<String>> propSubjObjList = new HashMap<>();

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while(rs.next()) {
                String property = rs.getString("prop_uri");
                String subj = rs.getString("subj_label");
                String obj = rs.getString("obj_label");

                String subjObj = subj + "|" + obj;
                List<String> subjObjList;
                if (!propSubjObjList.containsKey(property)) {
                    subjObjList = new ArrayList<>();
                    subjObjList.add(subjObj);
                } else {
                    subjObjList = propSubjObjList.get(property);
                    subjObjList.add(subjObj);
                }
                propSubjObjList.put(property, subjObjList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propSubjObjList;
    }

    public void setEntityScoresFromResultFile(String resultFile) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(resultFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                String[] result = strLine.split("\t");
                int entityId = Integer.parseInt(result[0]);
                String predictedProperty = result[1];
                double score = Double.parseDouble(result[2]);

                HashMap<String, String> predictionMap = new HashMap<>();
                predictionMap.put("property", predictedProperty);
                predictionMap.put("score", String.valueOf(score));
                entityPredictionMap.put(entityId, predictionMap);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SentenceEvaluation se = new SentenceEvaluation("alpha0.4beta0.7", 0.4, 0.7);
        Set<Integer> sentIdsSet = se.getSentIdOKE();

        int truePositive = 0;
        int falsePositive = 0;
        int falseNegative = 0;

        for (int sentId : sentIdsSet) {
            // get OKE resultant properties
            HashMap<String, List<String>> propSubjObjList = se.getOKEPropertiesForSentence(sentId);

            // get our entities gathered from the same sentence
            HashMap<Integer, String> entitySubjObjMap = se.getEntitiesFromSentId(sentId);

            // get entities after threshold
            HashMap<Integer, String> evaluationEntitySubjObjMap = new LinkedHashMap<>();
            HashMap<String, List<Integer>> evaluationPropertyEntitiesMap = new HashMap<>();
            for (int entityId :  entitySubjObjMap.keySet()) {
                if (se.entityPredictionMap.containsKey(entityId)) {
                    evaluationEntitySubjObjMap.put(entityId, entitySubjObjMap.get(entityId));

                    String predictedProperty = se.entityPredictionMap.get(entityId).get("property");
                    List<Integer> entitiesForProperty = new LinkedList<>();
                    if (!evaluationPropertyEntitiesMap.containsKey(predictedProperty)) {
                        entitiesForProperty.add(entityId);
                    } else {
                        entitiesForProperty = evaluationPropertyEntitiesMap.get(predictedProperty);
                        entitiesForProperty.add(entityId);
                    }
                    evaluationPropertyEntitiesMap.put(predictedProperty, entitiesForProperty);
                }
            }

            // for each OKE result property get subj and obj, and match if triple exist in our prediction
            for (String okeProperty : propSubjObjList.keySet()) {
                List<String> subjObjForPropety = propSubjObjList.get(okeProperty);
                // get our entities which predicted this property
                if (evaluationPropertyEntitiesMap.containsKey(okeProperty.toLowerCase())) {
                    List<Integer> entities = evaluationPropertyEntitiesMap.get(okeProperty.toLowerCase());

                    int identifiedForCurrentProperty = 0;
                    for (String subjObj : subjObjForPropety) {
                        String subj = subjObj.split("\\|")[0];
                        String obj = subjObj.split("\\|")[1];

                        boolean propertyIdentified = false;
                        for (int entityId : entities) {
                            String entitySubjObj = entitySubjObjMap.get(entityId);
                            String entitySubj = entitySubjObj.split("\\|")[0];
                            String entityObj = entitySubjObj.split("\\|")[1];

                            if ((subj.contains(entitySubj) || entitySubj.contains(subj))
                                    && (obj.contains(entityObj) || entityObj.contains(obj))) {
                                // since predicted and oke properties are alredy matched before, no need to check again
                                propertyIdentified = true;
                                break;
                            }
                        }

                        if (propertyIdentified) {
                            truePositive++;
                            identifiedForCurrentProperty++;
                        }
                        else
                            falseNegative++;
                    }

                    int entitiesPredictedForCurrentProperty = entities.size();
                    if ((entitiesPredictedForCurrentProperty - identifiedForCurrentProperty) > 0) {
                        falsePositive = falsePositive + (entitiesPredictedForCurrentProperty - identifiedForCurrentProperty);
                    }

                } else {
                    // increment false negative count
                    falseNegative = falseNegative + propSubjObjList.get(okeProperty).size();
                }
            }
        }
        System.out.println("TP:\t" + truePositive);
        System.out.println("FP:\t" + falsePositive);
        System.out.println("FN:\t" + falseNegative);
    }
}
