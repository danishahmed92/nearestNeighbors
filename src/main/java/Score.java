import config.Database;
import config.IniConfig;
import dbo.PropertyClassification;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Score {
    Score() {
        setPropertyPatternCountMap();
    }
    private PropertyClassification pc = PropertyClassification.propertyClassificationInstance;
//    private PropertyClassification pc = null;
    private HashMap<String, Integer> propertyPatternCountMap = new LinkedHashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, String>>> propertyPatternFreqSGMap = new HashMap<>();

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

    public HashMap<Integer, HashMap<String, String>> getOKETripleDetail() {
        String query = "select ot.id_oke_triple, ot.prop_uri, ot.subj_class, ot.obj_class from oke_triples ot " +
                "inner join oke_property_class opc on opc.prop_uri = ot.prop_uri " +
                "where id_oke_triple != 321 order by ot.id_oke_triple;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okeTripleIdDetailMap = new LinkedHashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                HashMap<String, String> tripleDetailMap = new HashMap<>();

                tripleDetailMap.put("property", rs.getString("prop_uri"));
                tripleDetailMap.put("subjClass", rs.getString("subj_class"));
                tripleDetailMap.put("objClass", rs.getString("obj_class"));

                okeTripleIdDetailMap.put(rs.getInt("id_oke_triple"), tripleDetailMap);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okeTripleIdDetailMap;
    }

    public HashMap<String, HashMap<String, String>> getOKEPatternForTripleId(int tripleId) {
        String query = String.format("select orig_root, root_lemma, pattern, sg_pretty, dist_nouns, dist_verbs from oke_patterns " +
                "where id_oke_triple = %s;", tripleId);
        HashMap<String, HashMap<String, String>> patternDetailMap = new HashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                HashMap<String, String> detailMap = new HashMap<>();

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

                detailMap.put("sgPretty", sgPretty);
                detailMap.put("root", rs.getString("orig_root"));
                detailMap.put("rootLemma", rs.getString("root_lemma"));
                detailMap.put("nouns", nouns);
                detailMap.put("verbs", verbs);

                patternDetailMap.put(rs.getString("pattern"), detailMap);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patternDetailMap;
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

    public double getConfidence(double alpha, double beta, String property, String compareWithPattern, HashMap<String, String> comparisonPatternMap,
                                String sentenceGeneratedPattern, String sentenceSGPretty, double cosineSimilarity) {
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

        /*List<String> roots = new ArrayList<>();
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

        double cosineSimilarity = rootCosine + nounsVerbCosine;*/

        JaroWinklerDistance similarityMetric = new JaroWinklerDistance();
        double patternSimilarity = similarityMetric.apply(sentenceGeneratedPattern, compareWithPattern);
        double sgSimilarity = similarityMetric.apply(sentenceSGPretty, comparisonPatternMap.get("sgPretty"));

        double betaCalculation = beta * patternSimilarity * sgSimilarity;

//        double confidence = ((alphaCalculation * betaCalculation) + (alpha + beta) * cosineSimilarity);
        double confidence = ((alphaCalculation * betaCalculation) + cosineSimilarity);
        double confidenceNormalized = (confidence - 0) / (3 - 0);
//        double confidenceNormalized = (confidence - 0) / ((3.6 + 1) - 0);

        return confidenceNormalized;
    }

    public HashMap<String, HashMap<String, String>> getPatternsFreqAndSGPrettyForProperty(String property) {
        if (propertyPatternFreqSGMap.containsKey(property))
            return propertyPatternFreqSGMap.get(property);

        String query = String.format("select pattern, sg_pretty, support, specificity, occur_prop, occur_pattern_freq from pattern_stats where prop_uri = \"%s\";", property);
        /*String query = String.format("select pp.pattern, " +
                "(select pa.sg_pretty from property_pattern pa where pa.prop_uri = \"%s\" and pa.pattern = pp.pattern limit 1) as sg_pretty, " +
                "count(pp.pattern) as support, " +
                "(select count(ps.pattern) from property_pattern  ps where ps.prop_uri != \"%s\" and ps.pattern = pp.pattern) as specificity, " +
                "(select distinct(count(ppa.prop_uri)) from property_pattern ppa where ppa.prop_uri != \"%s\" and ppa.pattern = pp.pattern) as occur_prop, " +
                "(select count(id_prop_pattern) from property_pattern sp where sp.prop_uri IN (select distinct(prop_uri) from property_pattern dp where dp.pattern = pp.pattern) and sp.prop_uri != \"%s\") as occur_pattern_freq " +
                "from property_pattern pp " +
                "where prop_uri = \"%s\" " +
                "group by pattern " +
                "order by support desc; ", property, property, property, property, property);*/
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

    public Set<String> getPropertiesValidatingDomainRange(String domain, String range) {
        String query = String.format("select prop_uri from oke_property_class where subj_class = \"%s\" and obj_class = \"%s\";",
                domain, range);
        Set<String> properties = new HashSet<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                properties.add(rs.getString("prop_uri"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void main(String[] args) {
//        double alpha = 0.1;
//        double beta = 0.1;

        Score score = new Score();
        HashMap<Integer, HashMap<String, String>> okeTripleIdDetailMap = score.getOKETripleDetail();
        int maxCorrect = -1;
        double bestAlpha = -1;
        double bestBeta = -1;

        for (double alpha = 0.1; alpha <= 0.9;) {
            for (double beta = 0.1; beta <= 0.9;) {
                HashMap<Integer, String> tripleIdentifiedPropertyMap = new LinkedHashMap<>();
                int correctlyIdentified = 0;
                int triplesEvaluated = 0;

                String outputFile = String.format("%s%salpha%1fbeta%1f",
                        IniConfig.configInstance.resultPath,
                        "w2v/synset/",
                        alpha,
                        beta);
                try {
//                    PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
                    PrintWriter writer;

                    for (int tripleId : okeTripleIdDetailMap.keySet()) {
                        double confidence = -1;
                        String maxProperty = "";

                        HashMap<String, String> tripleDetailMap = okeTripleIdDetailMap.get(tripleId);
                        HashMap<String, HashMap<String, String>> patternDetailMap = score.getOKEPatternForTripleId(tripleId);

                        if (patternDetailMap.size() > 0) {
                            Set<String> properties = score.getPropertiesValidatingDomainRange(tripleDetailMap.get("subjClass"), tripleDetailMap.get("objClass"));

                            for (String sentPattern : patternDetailMap.keySet()) {
                                HashMap<String, String> sentPatternDetail = patternDetailMap.get(sentPattern);
                                for (String property : properties) {
                                    double cosine = score.preCosineOfInputForProperty(property, sentPatternDetail);
                                    HashMap<String, HashMap<String, String>> patternFreqSGMap = score.getPatternsFreqAndSGPrettyForProperty(property);

                                    for (String pattern : patternFreqSGMap.keySet()) {
                                        double patternScore = score.getConfidence(alpha, beta,
                                                property, pattern, patternFreqSGMap.get(pattern),
                                                sentPattern, sentPatternDetail.get("sgPretty"), cosine);

                                        if (patternScore > confidence) {
                                            confidence = patternScore;
                                            maxProperty = property;
                                        }
                                    }
                                }
                            }
                            triplesEvaluated++;
                        } else {
                            continue;
                        }

                        int isCorrect = 0;
                        if (maxProperty.equals(tripleDetailMap.get("property"))) {
                            correctlyIdentified++;
                            isCorrect = 1;
                        }

//                        writer.println(String.format("%d\t%s\t%s\t%d",
//                                tripleId,
//                                tripleDetailMap.get("property"),
//                                maxProperty,
//                                isCorrect));

                        tripleIdentifiedPropertyMap.put(tripleId, maxProperty);
                    }

//                    writer.println();
//                    writer.println(triplesEvaluated + "\t" + correctlyIdentified);
//                    writer.close();

                    if (correctlyIdentified > maxCorrect) {
                        maxCorrect = correctlyIdentified;
                        bestAlpha = alpha;
                        bestBeta = beta;
                    }
                } catch (NullPointerException /*FileNotFoundException | UnsupportedEncodingException*/ e) {
                    e.printStackTrace();
                }
                beta = beta + 0.1;
            }
            alpha = alpha + 0.1;
        }

        System.out.println("Max identified:\t" + maxCorrect);
        System.out.println("at alpha:\t" + bestAlpha);
        System.out.println("at beta:\t" + bestBeta);
    }
}
