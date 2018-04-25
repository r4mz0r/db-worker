package com.rmz.db;

import org.apache.log4j.Logger;
import org.postgresql.PGResultSetMetaData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataBase class
 *
 * @author mekhdiev-rr
 */
public class DB {

    private static HashMap<String, String> tns = new HashMap<>();
    private static boolean loaded = false;

    private String dbType;
    private String dbURL;

    private ResultSetMetaData resultSetMetaData;
    private PGResultSetMetaData pgResultSetMetaData;
    private List<Object[]> data = new ArrayList<>();
    private HashMap<String, Integer> headersIndex = new HashMap<>();
    private ArrayList<String> header = new ArrayList<>();

    private Connection connection;

    private CallableStatement enableStmt;
    private CallableStatement disableStmt;
    private CallableStatement showStmt;

    private DBConfig dbc;

    private static final Logger LOGGER = Logger.getLogger(DB.class);

    /**
     * Create new DB instance
     *
     * @param dbName - DB key
     */
    public DB(String dbName) {
        // Get debug mode
        boolean debugMode = "true".equals(Props.get("db.debug.mode"));
        dbc = new DBConfig(dbName
                , Props.get("db." + dbName + ".server")
                , Props.get("db." + dbName + ".username")
                , Props.get("db." + dbName + ".password")
                , debugMode);
        if (dbc.isDebug()) {
            LOGGER.info("Connection to DB -> " + dbName);
        }

        // Open DB connection
        openConnection();
    }

    /**
     * Create new DB instance
     *
     * @param dbName     - DB Name
     * @param dbServer   - DB server
     * @param dbUser     - DB user
     * @param dbPassword - DB password
     * @param isDebug    - Debug Mode
     */
    public DB(String dbName, String dbServer, String dbUser, String dbPassword, boolean isDebug) {
        dbc = new DBConfig(dbName, dbServer, dbUser, dbPassword, isDebug);
        if (dbc.isDebug()) {
            LOGGER.info("Connection to DB -> " + dbName);
        }
        // Open DB connection
        openConnection();
    }

    public DB(String dbName, String dbServer, String dbUser, String dbPassword) {
        dbc = new DBConfig(dbName, dbServer, dbUser, dbPassword);
        // Open DB connection
        openConnection();
    }

    /**
     * Open new DB connection
     */
    private void openConnection() {
        try {

            dbURL = dbc.getTnsName();
            dbType = dbURL.split(":")[1];

            if (dbURL.contains("jdbc:")) {
                DBEnums.TYPE matchedValue = DBEnums.TYPE.ifContains(dbType);
                if (matchedValue != null) {
                    switch (matchedValue) {
                        case ORACLE:
                            if (dbc.isDebug()) {
                                LOGGER.info("Connect to Oracle database to: " + dbURL);
                            }
                            dbURL = "jdbc:oracle:thin:@" + getConnectionString(dbURL);
                            String strUserID = dbc.getUserName();
                            String strPassword = dbc.getPassword();
                            connection = java.sql.DriverManager.getConnection(dbURL, strUserID, strPassword);
                            prepareDbms();
                            break;
                        case MYSQL:
                            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
                            break;
                        case POSTGRES:
                            Class.forName("org.postgresql.Driver").newInstance();
                            break;

                    }
                } else {
                    LOGGER.error("DB type " + dbType + " is not available!");
                }

                Properties properties = new Properties();
                properties.put("user", dbc.getUserName());
                properties.put("password", dbc.getPassword());
                connection = DriverManager.getConnection(dbURL, properties);
            } else {
                LOGGER.error("Cannot read a connection string!");
            }
            if (dbc.isDebug()) {
                LOGGER.info("Connection established");
            }
        } catch (Exception ex) {
            LOGGER.error("Cannot connect to database server!", ex);
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlEx) {
                    LOGGER.error("Cannot close connection to database server!", sqlEx);
                }
            }
        }

    }

    /**
     * Execute the query
     *
     * @param sql - SQL-запрос
     * @return this
     * @throws SQLException - exception
     */

    public DB query(String sql) throws SQLException {
        if (dbc.isDebug()) {
            LOGGER.info("SQL REQUEST: \n" + sql);
        }
        // Data clear
        data.clear();
        headersIndex.clear();
        header.clear();

        Statement sqlStatement = connection.createStatement();
        boolean isDML = sql.toUpperCase().indexOf("UPDATE") == 0 ||
                sql.toUpperCase().indexOf("DELETE") == 0 ||
                sql.toUpperCase().indexOf("INSERT") == 0 ||
                sql.toUpperCase().indexOf("DECLARE") == 0 ||
                sql.toUpperCase().indexOf("BEGIN") == 0;
        // Requests without resultSet
        if (isDML) {
            sqlStatement.executeUpdate(sql);
            return this;
        }
        ResultSet resultSet = sqlStatement.executeQuery(sql);

        // Set resultSetMetaData
        resultSetMetaData = resultSet.getMetaData();
        if (DBEnums.TYPE.POSTGRES.getTypeName().equals(dbType)) {
            pgResultSetMetaData = (PGResultSetMetaData) resultSetMetaData;
        }
        // Get headers name
        for (int i = 0; i < resultSetMetaData.getColumnCount(); ++i) {
            headersIndex.put(resultSetMetaData.getColumnName(i + 1), i);
            header.add(resultSetMetaData.getColumnName(i + 1));
        }
        // Set data object
        while (resultSet.next()) {
            Object[] obj = new Object[resultSetMetaData.getColumnCount()];
            for (int i = 0; i < resultSetMetaData.getColumnCount(); ++i) {
                obj[i] = resultSet.getObject(i + 1);
            }
            data.add(obj);
        }
        resultSet.close();
        return this;
    }

    /**
     * Execute the query with parameters
     *
     * @param sql    - sql script
     * @param params - parameters
     * @return this
     * @throws SQLException - exception
     */
    public DB query(String sql, HashMap<String, String> params) throws SQLException {
        if (params == null)
            return query(sql);

        Iterator<String> iterator = params.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            sql = sql.replaceAll("@" + key, params.get(key).replace("$", "."));
        }
        return query(sql);
    }

    /**
     * Read SQL file and execute the query
     *
     * @param filename - file name
     * @param params   - parameters
     * @return this
     * @throws IOException  - exception
     * @throws SQLException - exception
     */
    public DB queryFile(String filename, HashMap<String, String> params) throws IOException, SQLException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        String sql = "";
        while ((line = br.readLine()) != null) {
            sql += line + "\n";
        }
        br.close();
        return query(sql, params);
    }

    /**
     * Enabled DBMS_OUTPUT
     *
     * @param size - DBMS_OUTPUT buffer size
     * @throws SQLException - exception
     */
    public DB enableDbmsOutput(int size) throws SQLException {
        enableStmt.setInt(1, size);
        enableStmt.executeUpdate();
        return this;
    }

    /**
     * Enabled DBMS_OUTPUT
     *
     * @throws SQLException - exception
     */
    public DB enableDbmsOutput() throws SQLException {
        return enableDbmsOutput(1000000);
    }

    /**
     * Disable DBMS_OUTPUT
     *
     * @throws SQLException - exception
     */
    public DB disableDbmsOutput() throws SQLException {
        disableStmt.executeUpdate();
        return this;
    }

    /**
     * Get DBMS_OUTPUT value
     *
     * @return String - DBMS_OUTPUT value string
     * @throws SQLException - exception
     */
    public String getDbmsOutput() throws SQLException {
        int done = 0;

        showStmt.registerOutParameter(2, java.sql.Types.INTEGER);
        showStmt.registerOutParameter(3, java.sql.Types.VARCHAR);
        String res = "";

        for (; ; ) {
            showStmt.setInt(1, 32000);
            showStmt.executeUpdate();
            String stmtString = showStmt.getString(3);
            res += ((res == "") ? "" : "\r\n") + stmtString;
            if ((done = showStmt.getInt(2)) == 1) break;
        }
        return res;
    }

    /**
     * Remove row from select results (not DB!)
     *
     * @param row - row ID
     */
    public void removeRow(int row) {
        if (data.size() <= row) {
            LOGGER.error("Cannot delete row, if this row doesn't exist!");
        }
        data.remove(row);
    }

    /**
     * Set TNS from file
     *
     * @throws IOException - исключение
     */
    private static void getTNS() throws IOException {
        Pattern p = Pattern.compile("^(\\w+)\\s*=(.+)$");
        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperties().getProperty("user.dir") + "/config/TNSNAMES.ora"))) {
            String line = br.readLine();

            while (line != null) {
                final Matcher matcher = p.matcher(line);
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#' && matcher.matches() && matcher.groupCount() > 1) {
                    tns.put(matcher.group(1).toUpperCase(), matcher.group(2));
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("Exception in TNS reading process : ", e);
        } finally {
            loaded = true;
        }
    }

    /**
     * Get connection String
     *
     * @param tnsString - tns String
     * @return String
     * @throws IOException - exception
     */
    private static String getConnectionString(String tnsString) throws IOException {
        if (!loaded) {
            getTNS();
        }
        return tns.get(tnsString.toUpperCase());
    }

    /**
     * Prepare DBMS
     *
     * @throws SQLException - exception
     */
    private void prepareDbms() throws SQLException {
        enableStmt = connection.prepareCall("begin dbms_output.enable(:1); end;");
        disableStmt = connection.prepareCall("begin dbms_output.disable; end;");
        showStmt = connection.prepareCall(
                "declare " +
                        "    l_line varchar2(255); " +
                        "    l_done number; " +
                        "    l_buffer long; " +
                        "begin " +
                        "  loop " +
                        "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; " +
                        "    dbms_output.get_line( l_line, l_done ); " +
                        "    l_buffer := l_buffer || l_line || chr(10); " +
                        "  end loop; " +
                        " :done := l_done; " +
                        " :buffer := l_buffer; " +
                        "end;");
    }

    /**
     * Get log data in rows
     *
     * @param rows - rows count
     * @return
     */
    public DB logData(int rows) {
        String str = "Заголовки:\n";
        for (int i = 0; i < headersIndex.size(); ++i) {
            str += headersIndex.keySet().toArray()[i].toString() + "\n";
        }
        str += "\nДанные:\n";
        for (int r = 0; r < rows && r < data.size(); ++r) {
            str += r + ") ";
            for (int i = 0; i < headersIndex.size() && i < 10; ++i) {
                str += data.get(r)[i].toString() + " \t";
            }
            if (headersIndex.size() > 10)
                str += "...";
            str += "\n";
        }
        if (data.size() > rows)
            str += "...\n";
        LOGGER.info("Rows(" + rows + ") data: " + str);
        return this;
    }

    /**
     * Get log data in rows
     *
     * @return
     */
    public DB logData() {
        return logData(20);
    }


    /**
     * Close Connection
     *
     * @return this
     * @throws SQLException - exception
     */
    public DB close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        return this;
    }

    /**
     * Get Row data
     *
     * @param row - row ID
     * @return Object[]
     */
    public Object[] getRow(int row) {
        return data.get(row);
    }

    /**
     * Get Row Data in HashMap
     *
     * @param row - row ID
     * @return HashMap
     */
    public HashMap<String, Object> getRowHashMap(int row) {
        HashMap<String, Object> res = new HashMap<>();
        for (int i = 0; i < header.size(); ++i) {
            res.put(header.get(i), data.get(row)[i]);
        }
        return res;
    }

    /**
     * Get Row Data in LinkedHashMap
     *
     * @param row - row ID
     * @return
     */
    public LinkedHashMap<String, Object> getRowLinkedHashMap(int row) {
        LinkedHashMap<String, Object> res = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); ++i) {
            res.put(header.get(i), data.get(row)[i]);
        }
        return res;
    }

    /**
     * Get Row count
     *
     * @return int - row count
     */
    public int getRows() {
        return data.size();
    }

    /**
     * Get MetaData results
     *
     * @return ResultSetMetaData - MetaData
     */
    public ResultSetMetaData getMetaData() {
        return resultSetMetaData;
    }

    public PGResultSetMetaData getPGMetaData() {
        return pgResultSetMetaData;
    }

    /**
     * Get Object from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return Object
     */
    public Object getObject(int i, String column) {
        int index = headersIndex.get(column);
        return data.get(i)[index];
    }

    /**
     * Get Column name using column ID
     *
     * @param i - column ID
     * @return header
     */
    public String getColumnName(int i) {
        return header.get(i);
    }

    /**
     * Get Int from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return int
     */
    public int getInt(int i, String column) {
        return (Integer) getObject(i, column);
    }

    /**
     * Get Float from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return float
     */
    public float getFloat(int i, String column) {
        return ((BigDecimal) getObject(i, column)).floatValue();
    }

    /**
     * Get Long from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return long
     */
    public long getLong(int i, String column) {
        return (long) getObject(i, column);
    }

    /**
     * Get Timestamp from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return Timestamp
     */
    public Timestamp getTimestamp(int i, String column) {
        return (Timestamp) getObject(i, column);
    }

    /**
     * Get Double from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return Double
     */
    public double getDouble(int i, String column) {
        return (Double) getObject(i, column);
    }


    /**
     * Get String from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return String
     */
    public String getString(int i, String column) {
        Object obj = getObject(i, column);
        if (obj != null) {
            return getObject(i, column).toString();
        } else {
            return null;
        }
    }

    /**
     * Get Date from rowID and column
     *
     * @param i      - row ID
     * @param column - column name
     * @return Date
     */
    public Date getDate(int i, String column) {
        Object obj = getObject(i, column);
        if (obj != null) {
            return new Date(((Timestamp) getObject(i, column)).getTime());
        } else {
            return null;
        }
    }

}
