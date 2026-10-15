package com.eoms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens a fresh JDBC connection for each operation; never stores credentials in source. */
public final class DatabaseConnection {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnection() {
        this(System.getenv("EOMS_DB_URL"), System.getenv("EOMS_DB_USER"),
                System.getenv("EOMS_DB_PASSWORD"));
    }

    public DatabaseConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection open() throws SQLException {
        if (url == null || url.isBlank() || user == null || user.isBlank()
                || password == null || password.isBlank()) {
            throw new SQLException("Set EOMS_DB_URL, EOMS_DB_USER and EOMS_DB_PASSWORD "
                    + "before using database features (see README.md).");
        }
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new SQLException("EOMS_DB_URL must be a PostgreSQL JDBC URL.");
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new SQLException("Cannot connect to PostgreSQL. Check the server, "
                    + "database name, JDBC driver and EOMS_DB_* settings. SQL state: "
                    + ex.getSQLState(), ex);
        }
    }
}
