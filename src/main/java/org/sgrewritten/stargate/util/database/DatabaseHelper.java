package org.sgrewritten.stargate.util.database;

import org.sgrewritten.stargate.Stargate;
import org.sgrewritten.stargate.api.config.ConfigurationOption;
import org.sgrewritten.stargate.api.network.portal.flag.StargateFlag;
import org.sgrewritten.stargate.api.network.portal.PositionType;
import org.sgrewritten.stargate.config.ConfigurationHelper;
import org.sgrewritten.stargate.config.TableNameConfiguration;
import org.sgrewritten.stargate.database.DatabaseDriver;
import org.sgrewritten.stargate.database.MySqlDatabase;
import org.sgrewritten.stargate.database.SQLDatabaseAPI;
import org.sgrewritten.stargate.database.SQLQueryGenerator;
import org.sgrewritten.stargate.database.SQLiteDatabase;
import org.sgrewritten.stargate.exception.StargateInitializationException;
import org.sgrewritten.stargate.network.StorageType;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private DatabaseHelper(){
        throw new IllegalStateException("Utility class");
    }

    /**
     * Executes and closes the given statement
     *
     * @param statement <p>The statement to execute</p>
     * @throws SQLException <p>If an SQL exception occurs</p>
     */
    public static void runStatement(PreparedStatement statement) throws SQLException {
        statement.execute();
        statement.close();
    }

    /**
     * Create all tables used by the sql database
     * @param database <p>SQL database</p>
     * @param sqlQueryGenerator <p>Something that generates SQL queries</p>
     * @param useInterServerNetworks <p>Whether to create tables for inter server portals</p>
     * @throws SQLException <p>IF anything went wrong with creating the tables</p>
     */
    public static void createTables(SQLDatabaseAPI database, SQLQueryGenerator sqlQueryGenerator, boolean useInterServerNetworks) throws SQLException {
        Connection connection = database.getConnection();
        PreparedStatement localPortalsStatement = sqlQueryGenerator.generateCreatePortalTableStatement(connection, StorageType.LOCAL);
        runStatement(localPortalsStatement);
        PreparedStatement flagStatement = sqlQueryGenerator.generateCreateFlagTableStatement(connection);
        runStatement(flagStatement);
        addMissingFlags(connection, sqlQueryGenerator);

        PreparedStatement portalPositionTypesStatement = sqlQueryGenerator.generateCreatePortalPositionTypeTableStatement(connection);
        runStatement(portalPositionTypesStatement);
        addMissingPositionTypes(connection, sqlQueryGenerator);

        PreparedStatement portalPositionsStatement = sqlQueryGenerator.generateCreatePortalPositionTableStatement(connection, StorageType.LOCAL);
        runStatement(portalPositionsStatement);
        PreparedStatement portalPositionIndex = sqlQueryGenerator.generateCreatePortalPositionIndex(connection, StorageType.LOCAL);
        if (portalPositionIndex != null) {
            runStatement(portalPositionIndex);
        }

        PreparedStatement lastKnownNameStatement = sqlQueryGenerator.generateCreateLastKnownNameTableStatement(connection);
        runStatement(lastKnownNameStatement);
        PreparedStatement portalRelationStatement = sqlQueryGenerator.generateCreateFlagRelationTableStatement(connection, StorageType.LOCAL);
        runStatement(portalRelationStatement);
        PreparedStatement portalViewStatement = sqlQueryGenerator.generateCreatePortalViewStatement(connection, StorageType.LOCAL);
        runStatement(portalViewStatement);

        if (!useInterServerNetworks) {
            connection.close();
            return;
        }

        PreparedStatement serverInfoStatement = sqlQueryGenerator.generateCreateServerInfoTableStatement(connection);
        runStatement(serverInfoStatement);
        PreparedStatement interServerPortalsStatement = sqlQueryGenerator.generateCreatePortalTableStatement(connection, StorageType.INTER_SERVER);
        runStatement(interServerPortalsStatement);
        PreparedStatement interServerRelationStatement = sqlQueryGenerator.generateCreateFlagRelationTableStatement(connection, StorageType.INTER_SERVER);
        runStatement(interServerRelationStatement);
        PreparedStatement interPortalViewStatement = sqlQueryGenerator.generateCreatePortalViewStatement(connection, StorageType.INTER_SERVER);
        runStatement(interPortalViewStatement);
        PreparedStatement interPortalPositionsStatement = sqlQueryGenerator.generateCreatePortalPositionTableStatement(connection, StorageType.INTER_SERVER);
        runStatement(interPortalPositionsStatement);
        PreparedStatement interPortalPositionIndex = sqlQueryGenerator.generateCreatePortalPositionIndex(connection, StorageType.INTER_SERVER);
        if (interPortalPositionIndex != null) {
            runStatement(interPortalPositionIndex);
        }

        connection.close();
    }


    /**
     * Adds any flags not already in the database
     *
     * @param connection        <p>The database connection to use</p>
     * @param sqlQueryGenerator <p>The SQL Query Generator to use for generating queries</p>
     * @throws SQLException <p>If unable to get from, or update the database</p>
     */
    private static void addMissingFlags(Connection connection, SQLQueryGenerator sqlQueryGenerator) throws SQLException {
        PreparedStatement statement = sqlQueryGenerator.generateGetAllFlagsStatement(connection);
        PreparedStatement addStatement = sqlQueryGenerator.generateAddFlagStatement(connection);

        ResultSet resultSet = statement.executeQuery();
        List<String> knownFlags = new ArrayList<>();
        while (resultSet.next()) {
            knownFlags.add(resultSet.getString("character"));
        }
        for (StargateFlag flag : StargateFlag.values()) {
            if (!knownFlags.contains(String.valueOf(flag.getCharacterRepresentation()))) {
                addStatement.setString(1, String.valueOf(flag.getCharacterRepresentation()));
                addStatement.execute();
            }
        }
        statement.close();
        addStatement.close();
    }

    /**
     * Adds any position types not already in the database
     *
     * @param connection        <p>The database connection to use</p>
     * @param sqlQueryGenerator <p>The SQL Query Generator to use for generating queries</p>
     * @throws SQLException <p>If unable to get from, or update the database</p>
     */
    private static void addMissingPositionTypes(Connection connection, SQLQueryGenerator sqlQueryGenerator) throws SQLException {
        PreparedStatement statement = sqlQueryGenerator.generateGetAllPortalPositionTypesStatement(connection);
        PreparedStatement addStatement = sqlQueryGenerator.generateAddPortalPositionTypeStatement(connection);

        ResultSet resultSet = statement.executeQuery();
        List<String> knownPositionTypes = new ArrayList<>();
        while (resultSet.next()) {
            knownPositionTypes.add(resultSet.getString("positionName"));
        }
        for (PositionType type : PositionType.values()) {
            if (!knownPositionTypes.contains(type.toString())) {
                addStatement.setString(1, type.toString());
                addStatement.execute();
            }
        }
        statement.close();
        addStatement.close();
    }

    /**
     * Loads the database
     *
     * @param stargate <p>The Stargate instance to use for initialization</p>
     * @return <p>The loaded database</p>
     * @throws SQLException                                                       <p>If an SQL exception occurs</p>
     * @throws org.sgrewritten.stargate.exception.StargateInitializationException
     */
    public static SQLDatabaseAPI loadDatabase(Stargate stargate) throws SQLException, StargateInitializationException {
        if (ConfigurationHelper.getBoolean(ConfigurationOption.USING_REMOTE_DATABASE)) {
            if (ConfigurationHelper.getBoolean(ConfigurationOption.SHOW_HIKARI_CONFIG)) {
                return new MySqlDatabase(stargate);
            }

            DatabaseDriver driver = DatabaseDriver.valueOf(ConfigurationHelper.getString(ConfigurationOption.BUNGEE_DRIVER).toUpperCase());
            String bungeeDatabaseName = ConfigurationHelper.getString(ConfigurationOption.BUNGEE_DATABASE);
            int port = ConfigurationHelper.getInteger(ConfigurationOption.BUNGEE_PORT);
            String address = ConfigurationHelper.getString(ConfigurationOption.BUNGEE_ADDRESS);
            String username = ConfigurationHelper.getString(ConfigurationOption.BUNGEE_USERNAME);
            String password = ConfigurationHelper.getString(ConfigurationOption.BUNGEE_PASSWORD);
            boolean useSSL = ConfigurationHelper.getBoolean(ConfigurationOption.BUNGEE_USE_SSL);

            return switch (driver) {
                case MARIADB, MYSQL ->
                        new MySqlDatabase(driver, address, port, bungeeDatabaseName, username, password, useSSL);
                default ->
                        throw new SQLException("Unsupported driver: Stargate currently supports MariaDb and MySql for remote databases");
            };
        } else {
            String databaseName = ConfigurationHelper.getString(ConfigurationOption.DATABASE_NAME);
            File file = new File(stargate.getAbsoluteDataFolder(), databaseName + ".db");
            return new SQLiteDatabase(file);
        }
    }

    /**
     * @param usingRemoteDatabase <p>Whether the database is remote or not</p>
     * @return <p>A new sql query generator</p>
     */
    public static SQLQueryGenerator getSQLGenerator(boolean usingRemoteDatabase) {
        TableNameConfiguration config = DatabaseHelper.getTableNameConfiguration(usingRemoteDatabase);
        DatabaseDriver databaseEnum = usingRemoteDatabase ? DatabaseDriver.MYSQL : DatabaseDriver.SQLITE;
        return new SQLQueryGenerator(config, databaseEnum);
    }

    /**
     * @param usingRemoteDatabase <p>Whether the database is remote or not</p>
     * @return <p>A new table name configuration</p>
     */
    public static TableNameConfiguration getTableNameConfiguration(boolean usingRemoteDatabase) {
        String prefix = usingRemoteDatabase ? ConfigurationHelper.getString(ConfigurationOption.BUNGEE_INSTANCE_NAME)
                : "";
        String serverPrefix = usingRemoteDatabase ? Stargate.getServerUUID() : "";
        return new TableNameConfiguration(prefix, serverPrefix.replace("-", ""));
    }
}
