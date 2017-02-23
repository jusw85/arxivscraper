package edu.soton.ecs.arxivscraper;

import edu.soton.ecs.arxivscraper.util.DateTimeUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArxivDbWrapper implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private Connection connection;
    private String dbFile;
    private String tablename = "arxiv_raw";

    public ArxivDbWrapper(String dbFile, String tablename) {
        this.dbFile = dbFile;
        this.tablename = tablename;
    }

    public void initalize() throws SQLException, IOException, ClassNotFoundException {
        openDBConnection();
        initDb();
    }

    private void openDBConnection() throws SQLException, IOException, ClassNotFoundException {
        FileUtils.touch(new File(dbFile));
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
    }

    private void initDb() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + tablename + " (id TEXT PRIMARY KEY, ts TEXT, uri TEXT, raw BLOB)");) {
            statement.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            LOGGER.warn(e);
        }
    }

    public boolean isExtracted(String uri) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + tablename + " WHERE uri=?");) {
            statement.setString(1, uri);
            ResultSet results = statement.executeQuery();
            return results.next();
        }
    }

    public int insert(String id, String ts, String url, byte[] raw) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + tablename + " VALUES (?, ?, ?, ?)");) {
            statement.setString(1, id);
            statement.setString(2, ts);
            statement.setString(3, url);
            statement.setBytes(4, raw);
            return statement.executeUpdate();
        }
    }

    public int defaultInsert(String url, Serializable obj) throws SQLException {
        byte[] raw = SerializationUtils.serialize(obj);
        return insert(UUID.randomUUID().toString(), DateTimeUtil.currentDateTimeISO8601(), url, raw);
    }

    public List<ArxivEntry> getAllArxivEntries() throws SQLException, InvocationTargetException, IllegalAccessException {
        List<ArxivEntry> arxivEntries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT raw FROM " + tablename + " WHERE 1=1");) {
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                byte[] raw = results.getBytes(1);
                ArxivEntry arxivEntry = (ArxivEntry) SerializationUtils.deserialize(raw);
                arxivEntries.add(arxivEntry);
            }
        }
        return arxivEntries;
    }

}
