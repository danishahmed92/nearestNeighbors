import config.Database;
import config.IniConfig;
import utils.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThresholdCalculation {
    public HashMap<String, List<Double>> propertyScoresMap = new HashMap<>();

    public void setPropertyScoresMap(String file) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;

            while ((strLine = br.readLine()) != null) {
                String[] resultSplit = strLine.split("\t");
                if (resultSplit.length < 4)
                    continue;

                String actualProperty = resultSplit[1].toLowerCase().trim();
                String predictedProperty = resultSplit[2].toLowerCase().trim();

                if (actualProperty.equals(predictedProperty)) {
                    double score = Double.parseDouble(resultSplit[4]);
                    List<Double> scores;

                    if (propertyScoresMap.containsKey(actualProperty)) {
                        scores = propertyScoresMap.get(actualProperty);
                        scores.add(score);
                    } else {
                        scores = new ArrayList<>();
                        scores.add(score);
                    }
                    propertyScoresMap.put(actualProperty, scores);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeThreshold(double alpha, double beta, String property, HashMap<String, Double> statsMap) {
        String insertQuery = "INSERT INTO property_threshold (alpha, beta, prop_uri_lower, highest, lowest, mean, sd, variance) " +
                "VALUES (?, ?, " +
                "?, " +
                "?, ?, " +
                "?, ?, ?);";
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setDouble(1, alpha);
            prepareStatement.setDouble(2, beta);

            prepareStatement.setString(3, property);

            prepareStatement.setDouble(4, statsMap.get("highest"));
            prepareStatement.setDouble(5, statsMap.get("lowest"));

            prepareStatement.setDouble(6, statsMap.get("mean"));
            prepareStatement.setDouble(7, statsMap.get("sd"));
            prepareStatement.setDouble(8, statsMap.get("variance"));

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String resultDirectory = IniConfig.configInstance.resultPath + "w2v/synset/";
        String file = resultDirectory + "alpha0.400000beta0.700000";

        ThresholdCalculation thresholdCalculation = new ThresholdCalculation();
        thresholdCalculation.setPropertyScoresMap(file);
        HashMap<String, List<Double>> propertyScoresMap = thresholdCalculation.propertyScoresMap;
        HashMap<String, HashMap<String, Double>> propertyStatsMap = new HashMap<>();

        for (String property : propertyScoresMap.keySet()) {
            List<Double> scores = propertyScoresMap.get(property);
            double mean = Utils.mean(scores.toArray());
            double sd = Utils.standardDeviation(scores.toArray());
            double variance = Utils.variance(scores.toArray());

            if (scores.size() == 1) {
                sd = 0;
                variance = 0;
            }

            double highest = -1;
            double lowest = 1;
            for (double score : scores) {
                if (score > highest)
                    highest = score;
                if (score < lowest)
                    lowest = score;
            }

            HashMap<String, Double> statsMap = new HashMap<>();
            statsMap.put("highest", highest);
            statsMap.put("lowest", lowest);
            statsMap.put("mean", mean);
            statsMap.put("variance", variance);
            statsMap.put("sd", sd);

            propertyStatsMap.put(property, statsMap);
        }

        for (String property : propertyStatsMap.keySet()) {
            thresholdCalculation.storeThreshold(0.4, 0.7, property, propertyStatsMap.get(property));
        }
    }
}
