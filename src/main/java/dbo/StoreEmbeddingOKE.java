package dbo;

import config.Database;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class StoreEmbeddingOKE {

    public static void storeScore(String embeddingScoreTable, int patternId, HashMap<String, Double> rootMap, HashMap<String, Double> nounMap) {
        String insertQuery = String.format("INSERT INTO `%s` (id_oke_pattern, property_uri, embedding_root, support, specificity) " +
                "VALUES (?, ?, ?, ?, ?);", embeddingScoreTable);
        String pattern = getPatternFromDB(patternId);

        for (String property : rootMap.keySet()) {
            int support = getSupport(property, pattern);
            int specificity = getSpecificity(property, pattern);
            Double embeddingScore = rootMap.get(property);

            PreparedStatement prepareStatement = null;
            try {
                prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                prepareStatement.setInt(1, patternId);
                prepareStatement.setString(2, property);
                prepareStatement.setDouble(3, embeddingScore);
                prepareStatement.setInt(4, support);
                prepareStatement.setInt(5, specificity);

                prepareStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String insertNounQuery = String.format("INSERT INTO `%s` (id_oke_pattern, property_uri, embedding_noun, support, specificity) " +
                "VALUES (?, ?, ?, ?, ?);", embeddingScoreTable);
        for (String property : nounMap.keySet()) {
            int support = getSupport(property, pattern);
            int specificity = getSpecificity(property, pattern);
            Double embeddingScore = nounMap.get(property);

            PreparedStatement prepareStatement = null;
            try {
                prepareStatement = Database.databaseInstance.conn.prepareStatement(insertNounQuery, Statement.RETURN_GENERATED_KEYS);
                prepareStatement.setInt(1, patternId);
                prepareStatement.setString(2, property);
                prepareStatement.setDouble(3, embeddingScore);
                prepareStatement.setInt(4, support);
                prepareStatement.setInt(5, specificity);

                prepareStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getPatternFromDB(int patternId) {
        String patternQuery = String.format("SELECT `pattern` from oke_orig_pattern where id_oke_pattern = %s;", patternId);
        Statement statement = null;
        String pattern = null;

        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(patternQuery);

            while (rs.next()) {
                pattern = rs.getString("pattern");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pattern;
    }

    public static int getSupport(String property, String pattern) {
        String supportQuery = String.format("SELECT count(id_prop_pattern) as pcount from property_pattern where prop_uri = \"%s\" and pattern = \"%s\";",
                property, pattern);

        Statement statement = null;
        int support = 1;

        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(supportQuery);

            while (rs.next()) {
                support = support + rs.getInt("pcount");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return support;
    }

    public static int getSpecificity(String property, String pattern) {
        String specificityQuery = String.format("SELECT count(id_prop_pattern) as pcount from property_pattern where prop_uri NOT IN (\"%s\") and pattern = \"%s\";",
                property, pattern);
        Statement statement = null;
        int specificity = 0;

        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(specificityQuery);

            while (rs.next()) {
                specificity = rs.getInt("pcount");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return specificity;
    }
}
