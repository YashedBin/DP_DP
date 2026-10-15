package com.eoms.repository;

import com.eoms.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TeacherRepository {
    private final DatabaseConnection database;
    public TeacherRepository(DatabaseConnection database) { this.database = database; }

    public Credentials credentials(String username) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, password_hash FROM teachers WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new Credentials(rows.getLong(1), rows.getString(2)) : null;
            }
        }
    }

    public void bootstrap(String username, String hash) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            // Serialize concurrent first-run setup even when the table is empty.
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(349057)")) {
                lock.execute();
            }
            try (PreparedStatement count = connection.prepareStatement("SELECT 1 FROM teachers LIMIT 1")) {
                try (ResultSet rows = count.executeQuery()) {
                    if (rows.next()) throw new IllegalStateException("Teacher already configured");
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO teachers (username, password_hash) VALUES (?, ?)")) {
                    insert.setString(1, username);
                    insert.setString(2, hash);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
    }

    public void setPassword(String username, String hash) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE teachers SET password_hash = ? WHERE username = ?")) {
            statement.setString(1, hash);
            statement.setString(2, username);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Teacher not found");
        }
    }

    public record Credentials(long id, String passwordHash) {}
}
