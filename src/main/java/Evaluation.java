import config.IniConfig;
import utils.Utils;

import java.io.*;
import java.util.*;

public class Evaluation {
    private HashMap<String, HashMap<String, Integer>> propertyResultsMap = new HashMap<>();

    public void setEvaluationValuesFromTripleResult(String result) {
        // file format: tipleId actualProperty predictedProperty equals ? 1 : 0
        // if actualProperty = predictedProperty then TP++ else { FN++, predictedProperty's FP++;

        String[] resultSplit = result.split("\t");
        if (resultSplit.length < 4)
            return;

        String actualProperty = resultSplit[1].toLowerCase().trim();
        String predictedProperty = resultSplit[2].toLowerCase().trim();

        if (actualProperty.equals(predictedProperty)) {
            if (!propertyResultsMap.containsKey(actualProperty)) {
                HashMap<String, Integer> resultStatMap = new HashMap<>();
                resultStatMap.put("TP", 1);
                resultStatMap.put("FN", 0);
                resultStatMap.put("FP", 0);

                propertyResultsMap.put(actualProperty, resultStatMap);
            } else {
                HashMap<String, Integer> resultStatMap = propertyResultsMap.get(actualProperty);
                int tp = resultStatMap.get("TP") + 1;
                resultStatMap.put("TP", tp);
                propertyResultsMap.put(actualProperty, resultStatMap);
            }
        } else {
            if (!propertyResultsMap.containsKey(actualProperty)) {
                HashMap<String, Integer> resultStatMap = new HashMap<>();
                resultStatMap.put("TP", 0);
                resultStatMap.put("FN", 1);
                resultStatMap.put("FP", 0);

                propertyResultsMap.put(actualProperty, resultStatMap);
            } else {
                HashMap<String, Integer> resultStatMap = propertyResultsMap.get(actualProperty);
                int fn = resultStatMap.get("FN") + 1;
                resultStatMap.put("FN", fn);
                propertyResultsMap.put(actualProperty, resultStatMap);
            }

            if (!propertyResultsMap.containsKey(predictedProperty)) {
                HashMap<String, Integer> resultStatMap = new HashMap<>();
                resultStatMap.put("TP", 0);
                resultStatMap.put("FN", 0);
                resultStatMap.put("FP", 1);

                propertyResultsMap.put(predictedProperty, resultStatMap);
            } else {
                HashMap<String, Integer> resultStatMap = propertyResultsMap.get(predictedProperty);
                int fp = resultStatMap.get("FP") + 1;
                resultStatMap.put("FP", fp);
                propertyResultsMap.put(predictedProperty, resultStatMap);
            }
        }
    }

    public double calculatePrecision(String property) {
        HashMap<String, Integer> resultStatMap = propertyResultsMap.get(property);
        double truePositive = resultStatMap.get("TP");
        double falsePositive = resultStatMap.get("FP");

        double precision = (truePositive / (truePositive + falsePositive));
        if (Double.isNaN(precision))
            return 0.0;
        return precision;
    }

    public double calculateRecall(String property) {
        HashMap<String, Integer> resultStatMap = propertyResultsMap.get(property);
        double truePositive = resultStatMap.get("TP");
        double falseNegative = resultStatMap.get("FN");

        double recall = (truePositive / (truePositive + falseNegative));
        if (Double.isNaN(recall))
            return 0.0;
        return recall;
    }

    public double fMeasure(double precision, double recall) {
        double fMeasure = (2 * precision * recall) / (precision + recall);
        if (Double.isNaN(fMeasure))
            return 0.0;
        return fMeasure;
    }

    public double propertyFMeasure(String property) {
        double precision = calculatePrecision(property);
        double recall = calculateRecall(property);

        return fMeasure(precision, recall);
    }

    public static void main(String[] args) {
        String resultDirectory = IniConfig.configInstance.resultPath + "w2v/synset/";
        List<String> filesInDirectory = Utils.getFilesInDirectory(resultDirectory);

        HashMap<String, Double> fileFMeasure = new HashMap<>();
        double maxFmeasure = -1;
        String maxFile = "";

        for (String file : filesInDirectory) {
            String[] fileSplit = file.split(".");
//            double alpha = Double.parseDouble("0." + fileSplit[1].replaceAll("beta0", ""));
//            double beta = Double.parseDouble("0." + fileSplit[2]);
            double avgPrecision = 0.0;
            double avgRecall = 0.0;
            double avgFMeasure = 0.0;

            FileInputStream fstream = null;
            Evaluation evaluation = new Evaluation();
            Map<String, HashMap<String, Double>> sortedPropEvalMap = null;
            try {
                fstream = new FileInputStream(resultDirectory + file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                while ((strLine = br.readLine()) != null)   {
                    // Print the content on the console
                    evaluation.setEvaluationValuesFromTripleResult(strLine);
                }
                fstream.close();

                HashMap<String, HashMap<String, Double>> propertyEvalMap = new HashMap<>();
                for (String property : evaluation.propertyResultsMap.keySet()) {
                    double precision = evaluation.calculatePrecision(property);
                    double recall = evaluation.calculateRecall(property);
                    double fMeasure = evaluation.fMeasure(precision, recall);

                    avgPrecision = avgPrecision + precision;
                    avgRecall = avgRecall + recall;
                    avgFMeasure = avgFMeasure + fMeasure;

                    double tp = evaluation.propertyResultsMap.get(property).get("TP");
                    double fp = evaluation.propertyResultsMap.get(property).get("FP");
                    double fn = evaluation.propertyResultsMap.get(property).get("FN");

                    HashMap<String, Double> evalMap = new HashMap<>();
                    evalMap.put("TP", tp);
                    evalMap.put("FP", fp);
                    evalMap.put("FN", fn);
                    evalMap.put("P", precision);
                    evalMap.put("R", recall);
                    evalMap.put("F", fMeasure);

                    propertyEvalMap.put(property, evalMap);
                }

                avgPrecision = avgPrecision / (double)evaluation.propertyResultsMap.keySet().size();
                avgRecall = avgRecall / (double)evaluation.propertyResultsMap.keySet().size();
                avgFMeasure = avgFMeasure / (double)evaluation.propertyResultsMap.keySet().size();

                sortedPropEvalMap = new TreeMap<>(propertyEvalMap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (avgFMeasure > maxFmeasure) {
                maxFmeasure = avgFMeasure;
                maxFile = "PF" + file;
            }

            /*String outputFile = resultDirectory + "PF" + file;
            try {
                PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
                for (String property : sortedPropEvalMap.keySet()) {
                    HashMap<String, Double> evalMap = sortedPropEvalMap.get(property);

                    writer.println(String.format("%s\t%f\t%f\t%f\t%f\t%f\t%f",
                            property,
                            evalMap.get("TP"),
                            evalMap.get("FP"),
                            evalMap.get("FN"),
                            evalMap.get("P"),
                            evalMap.get("R"),
                            evalMap.get("F")));

                }
                writer.println();
                writer.println(avgPrecision + "\t" + avgRecall + "\t" + avgFMeasure);
                writer.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/
            fileFMeasure.put(file, avgFMeasure);
        }

        System.out.println(maxFile + ":\t" + maxFmeasure);
        System.out.println();
        System.out.println(entriesSortedByValues(fileFMeasure));


    }

    static <K,V extends Comparable<? super V>>
    List<Map.Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

        List<Map.Entry<K,V>> sortedEntries = new ArrayList<Map.Entry<K,V>>(map.entrySet());

        Collections.sort(sortedEntries,
                new Comparator<Map.Entry<K,V>>() {
                    @Override
                    public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );

        return sortedEntries;
    }

}
