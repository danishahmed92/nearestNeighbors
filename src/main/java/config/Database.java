package config;

import org.ini4j.Ini;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author DANISH AHMED on 12/19/2018
 */
public class Database {
    public static Database databaseInstance;
    static {
        try {
            databaseInstance = new Database();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection conn;

    public Database() throws SQLException {
        String dbHost = DatabaseConfig.dbConfigInstance.dbHost;
        String dbPort = DatabaseConfig.dbConfigInstance.dbPort;
        String database = DatabaseConfig.dbConfigInstance.database;
        String dbUser = DatabaseConfig.dbConfigInstance.dbUser;
        String dbPassword = DatabaseConfig.dbConfigInstance.dbPassword;

        String url = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, database);

        conn = DriverManager.getConnection(url, dbUser, dbPassword);
    }
}

class DatabaseConfig {
    static DatabaseConfig dbConfigInstance;

    static {
        try {
            dbConfigInstance = new DatabaseConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String dbHost;
    String dbPort;
    String database;
    String dbUser;
    String dbPassword;

    private DatabaseConfig() throws IOException {
        String CONFIG_FILE = "dbConfig.ini";
        Ini configIni = new Ini(IniConfig.class.getClassLoader().getResource(CONFIG_FILE));

        dbHost = configIni.get("mysql", "dbHost");
        dbPort = configIni.get("mysql", "port");
        database = configIni.get("mysql", "database");
        dbUser = configIni.get("mysql", "dbUser");
        dbPassword = configIni.get("mysql", "dbPassword");
    }
}
