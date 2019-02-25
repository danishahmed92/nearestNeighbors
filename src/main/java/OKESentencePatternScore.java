import config.Database;
import dbo.PropertyClassification;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 2/21/2019
 */
public class OKESentencePatternScore {
    private HashMap<String, Integer> propertyPatternCountMap = new LinkedHashMap<>();
    private HashMap<Integer, Set<String>> patternIdPropertiesMap = new HashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, String>>> propertyPatternFreqSGMap = new HashMap<>();

    private PropertyClassification pc = PropertyClassification.propertyClassificationInstance;

    public OKESentencePatternScore() {
        setPropertyPatternCountMap();
    }

    private void setPropertyPatternCountMap() {
        String supportQuery = "SELECT prop_uri, count(id_prop_pattern) as pcount from property_pattern group by prop_uri;";
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(supportQuery);

            while (rs.next()) {
                propertyPatternCountMap.put(rs.getString("prop_uri"), rs.getInt("pcount"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String removeWordsFromSGPretty(String sgPretty) {
        String[] split = sgPretty.split("/");
        StringBuilder removedSG = new StringBuilder();
        for (String part : split) {
            if (part.contains(">")) {
                String[] partSplit = part.split(">");
                removedSG.append(partSplit[0]).append(">");
                if (partSplit[1] != null && partSplit[1].contains("["))
                    removedSG.append("[");
            } else if (part.contains("[")) {
                removedSG.append("[");
            } else if (part.contains("]")) {
                removedSG.append(part);
            }
        }
        return removedSG.toString();
    }

    public HashMap<Integer, HashMap<String, String>> getOKEPatternsMap() {
        String selectQuery = "SELECT p.id_oke_coref_pattern, p.orig_root, p.root_lemma, p.pattern, p.sg_pretty, p.dist_nouns, p.dist_verbs from oke_coref_entity_pattern p " +
                "order by p.id_oke_coref_pattern";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okePatternMap = new LinkedHashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int patternId = rs.getInt("id_oke_coref_pattern");

                String sgPretty = rs.getString("sg_pretty");
                sgPretty = removeWordsFromSGPretty(sgPretty);

                String nouns = rs.getString("dist_nouns");
                nouns = nouns.substring(1, nouns.length()-1);
                nouns = nouns.replaceAll("-", ", ");
                nouns = nouns.replaceAll("_", ", ");

                String verbs = rs.getString("dist_verbs");
                verbs = verbs.substring(1, verbs.length()-1);
                verbs = verbs.replaceAll("-", ", ");
                verbs = verbs.replaceAll("_", ", ");

                HashMap<String, String> patternDetailMap = new HashMap<>();
                patternDetailMap.put("root", rs.getString("orig_root"));
                patternDetailMap.put("rootLemma", rs.getString("root_lemma"));
                patternDetailMap.put("pattern", rs.getString("pattern"));
                patternDetailMap.put("sgPretty", sgPretty);
                patternDetailMap.put("nouns", nouns);
                patternDetailMap.put("verbs", verbs);

                okePatternMap.put(patternId, patternDetailMap);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return okePatternMap;
    }

    public Set<String> getCandidatePropertiesForPattern (int patternId) {
        if (patternIdPropertiesMap.containsKey(patternId))
            return patternIdPropertiesMap.get(patternId);

        String selectQuery = String.format("SELECT pc.prop_uri from oke_property_class pc\n" +
                "INNER JOIN oke_sent_coref_entity_comb e ON (e.subj_class = pc.subj_class AND e.obj_class = pc.obj_class)\n" +
                "INNER JOIN oke_coref_entity_pattern p ON p.id_oke_sent_entity = e.id_oke_sent_entity\n" +
                "WHERE p.id_oke_coref_pattern = %d;", patternId);
        Statement statement = null;
        Set<String> candidateProperties = new HashSet<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next())
                candidateProperties.add(rs.getString("prop_uri"));

            patternIdPropertiesMap.put(patternId, candidateProperties);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patternIdPropertiesMap.get(patternId);
    }

    public HashMap<String, HashMap<String, String>> getPatternsFreqAndSGPrettyForProperty(String property) {
        if (propertyPatternFreqSGMap.containsKey(property))
            return propertyPatternFreqSGMap.get(property);

        String query = String.format("select pattern, sg_pretty, support, specificity, occur_prop, occur_pattern_freq from pattern_stats where prop_uri = \"%s\";", property);
        HashMap<String, HashMap<String, String>> patternFreqSGMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                HashMap<String, String> freqSGMap = new HashMap<>();

                String sgPretty = rs.getString("sg_pretty");
                sgPretty = removeWordsFromSGPretty(sgPretty);
                int support = rs.getInt("support");
                int specificity = rs.getInt("specificity");
                int occurProp = rs.getInt("occur_prop");
                int occurPatternFreq = rs.getInt("occur_pattern_freq");

                freqSGMap.put("support", String.valueOf(support));
                freqSGMap.put("specificity", String.valueOf(specificity));
                freqSGMap.put("occurProp", String.valueOf(occurProp));
                freqSGMap.put("occurPatternFreq", String.valueOf(occurPatternFreq));
                freqSGMap.put("sgPretty", sgPretty);

                patternFreqSGMap.put(rs.getString("pattern"), freqSGMap);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        propertyPatternFreqSGMap.put(property, patternFreqSGMap);
        return patternFreqSGMap;
    }

    public double preCosineOfInputForProperty(String property, HashMap<String, String> patternDetailMap) {
        List<String> roots = new ArrayList<>();
        roots.add(patternDetailMap.get("root").toLowerCase());
        if (patternDetailMap.get("rootLemma") != null)
            roots.add(patternDetailMap.get("rootLemma").toLowerCase());

        String nouns = patternDetailMap.get("nouns");
        String verbs = patternDetailMap.get("verbs");

        nouns = nouns.toLowerCase();
        verbs = verbs.toLowerCase();

        List<String> nounsVerbList = new ArrayList<>();

        if (nouns != null && nouns.length() > 0) {
            String[] nounsSplit = nouns.split(", ");
            nounsVerbList.addAll(Arrays.asList(nounsSplit));
        }
        if (verbs != null && verbs.length() > 0) {
            String[] verbsSplit = verbs.split(", ");
            nounsVerbList.addAll(Arrays.asList(verbsSplit));
        }

        double rootCosine = pc.getSimilarityOfWordsWithProperty(roots, property);
        double nounsVerbCosine = pc.getSimilarityOfWordsWithProperty(nounsVerbList, property);

        return rootCosine + nounsVerbCosine;
    }

    public double getConfidence(double alpha, double beta,
                             String property,
                             HashMap<String, String> inputSentPatternMap, HashMap<String, String> comparisonPatternMap,
                             double preComputedWordSimilarity) {
        double support = Double.parseDouble(comparisonPatternMap.get("support"));
        try {
            support = support / propertyPatternCountMap.get(property);
            if (Double.isNaN(support))
                support = 0;
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("no patterns for property: " + property);
        }

        double specificity = Double.parseDouble(comparisonPatternMap.get("specificity"));
        double occurProp = Double.parseDouble(comparisonPatternMap.get("occurProp"));
        double occurPatternFreq = Double.parseDouble(comparisonPatternMap.get("occurPatternFreq"));
        specificity = (specificity * occurProp) / occurPatternFreq;
        if (Double.isNaN(specificity))
            specificity = 0;

        double alphaCalculation = ((alpha * support) + (1 - alpha) * specificity);

        String inputSentencePattern = inputSentPatternMap.get("pattern");
        String compareWithPattern = comparisonPatternMap.get("pattern");
        String sentenceSGPretty = inputSentPatternMap.get("sgPretty");

        JaroWinklerDistance similarityMetric = new JaroWinklerDistance();
        double patternSimilarity = similarityMetric.apply(inputSentencePattern, compareWithPattern);
        double sgSimilarity = similarityMetric.apply(sentenceSGPretty, comparisonPatternMap.get("sgPretty"));

        double betaCalculation = beta * patternSimilarity * sgSimilarity;

        double confidence = ((alphaCalculation * betaCalculation) + preComputedWordSimilarity);
        return (confidence - 0) / (3 - 0);  // normalizing value
    }

    public static void main(String[] args) {
        OKESentencePatternScore patternScore = new OKESentencePatternScore();
        HashMap<Integer, HashMap<String, String>> okePatternMap = patternScore.getOKEPatternsMap();

        for (double alpha = 0.1; alpha <= 0.9; alpha = alpha + 0.1) {
            for (double beta = 0.1; beta <= 0.9; beta = beta + 0.1) {
                String outputFile = "";
                try {
                    PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
                    writer.println(String.format("%.1f\t%.1f", alpha, beta));

                    for (int patternId : okePatternMap.keySet()) {
                        HashMap<String, String> patternDetailMap = okePatternMap.get(patternId);
                        double confidence = -1;
                        String maxMatchedProperty = "";

                        Set<String> candidateProperties = patternScore.getCandidatePropertiesForPattern(patternId);
                        for (String property : candidateProperties) {
                            double wordSimilarity = patternScore.preCosineOfInputForProperty(property, patternDetailMap);
                            HashMap<String, HashMap<String, String>> patternFreqSGMap = patternScore.getPatternsFreqAndSGPrettyForProperty(property);

                            // compare candidate property with patterns of trained property
                            for (String trainPattern : patternFreqSGMap.keySet()) {
                                String sentPattern = patternDetailMap.get("pattern");
                                HashMap<String, String> comparisonPatternMap = new HashMap<>(patternFreqSGMap.get(trainPattern));
                                comparisonPatternMap.put("pattern", trainPattern);

                                double score = patternScore.getConfidence(alpha, beta,
                                        property,
                                        patternDetailMap, comparisonPatternMap,
                                        wordSimilarity);

                                if (score > confidence) {
                                    confidence = score;
                                    maxMatchedProperty = property;
                                }
                            }
                        }
                        writer.println(String.format("%d\t" +   // patternId
                                "%s\t" +    // predicted property
                                "%f",       // confidence value
                                patternId,
                                maxMatchedProperty,
                                confidence));
                    }

                    writer.close();
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
