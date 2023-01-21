package org.sgrewritten.stargate.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import org.jetbrains.annotations.NotNull;
import org.sgrewritten.stargate.Stargate;
import org.sgrewritten.stargate.config.TableNameConfiguration;
import org.sgrewritten.stargate.database.SQLDatabaseAPI;
import org.sgrewritten.stargate.network.StorageType;
import org.sgrewritten.stargate.util.FileHelper;
import org.sgrewritten.stargate.util.database.DatabaseHelper;

public class SQLDatabaseMigrator {
    private @NotNull TableNameConfiguration nameConfiguration;
    private @NotNull File sqlFilesPath;
    private boolean interServerEnabled;
    private @NotNull SQLDatabaseAPI database;

    public SQLDatabaseMigrator(@NotNull SQLDatabaseAPI database, @NotNull TableNameConfiguration nameConfiguration,@NotNull File sqlFilesPath,boolean interServerEnabled) throws SQLException {
        this.nameConfiguration = Objects.requireNonNull(nameConfiguration);
        this.sqlFilesPath = Objects.requireNonNull(sqlFilesPath);
        this.interServerEnabled = interServerEnabled;
        this.database = Objects.requireNonNull(database);
        
    }
    
    public void run() throws SQLException, IOException {
        Connection connection = null;
        try {
            connection = database.getConnection();
            run(StorageType.LOCAL,connection);
            if (interServerEnabled) {
                run(StorageType.INTER_SERVER,connection);
            }
        } finally {
            if(connection != null) {
                connection.close();
            }
        }
    }
    
    private void run(StorageType type, Connection connection) throws SQLException, IOException {
        File path = new File(sqlFilesPath, type.toString().toLowerCase());
        int count = 0;
        while (true) {
            InputStream stream = FileHelper.getInputStreamForInternalFile("/" + new File(path, count + ".sql").getPath());
            if (stream == null) {
                break;
            }

            String queryString = nameConfiguration.replaceKnownTableNames(FileHelper.readStreamToString(stream));
            Stargate.log(Level.WARNING,queryString);
            DatabaseHelper.runStatement(connection.prepareStatement(queryString));
            count++;
        }
    }
}
