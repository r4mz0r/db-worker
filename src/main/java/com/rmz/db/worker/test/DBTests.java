package com.rmz.db.worker.test;

import com.rmz.db.worker.DB;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * DB tests
 */
public class DBTests {

    private static final Logger LOGGER = Logger.getLogger(DBTests.class);

    private static void test1() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("projectName", "Сбербанк Бизнес");
        try {
            DB db = new DB("local")
                    .queryFile("D:\\stash\\sbbol-desktop\\src\\test\\resources\\sql\\getProjectId.sql", hashMap).close();
            LOGGER.info(db.getString(0, "Id"));
            db.close();
        } catch (IOException | SQLException sqlEx) {
            LOGGER.error("Ошибка при выполнении SQL-запроса!", sqlEx);
        }
    }

    private static void test2() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("projectName", "Сбербанк Бизнес");
        try {
            DB db = new DB("local"
                    , "jdbc:mysql://10.116.99.43:3306/qa?autoReconnect=true&useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"
                    , "qa_jenkins"
                    , "123456")
                    .queryFile("D:\\stash\\sbbol-desktop\\src\\test\\resources\\sql\\getProjectId.sql", hashMap).close();
            LOGGER.info(db.getString(0, "Id"));
            db.close();
        } catch (IOException | SQLException sqlEx) {
            LOGGER.error("Ошибка при выполнении SQL-запроса!", sqlEx);
        }
    }

    /**
     * Тестируем
     *
     * @throws Exception - исключение
     */

    public static void main(String[] arg) throws Exception {
        test1();
       test2();
    }
}
