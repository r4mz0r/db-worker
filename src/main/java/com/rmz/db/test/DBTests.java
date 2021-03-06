package com.rmz.db.test;

import com.rmz.db.DB;

import java.sql.SQLException;
import java.util.LinkedHashMap;

/**
 * DB tests
 */
public class DBTests {

    private static void test1() {
        try {
            DB db = new DB("local")
                    .query("SELECT * FROM MP_SBBOL_DEVICES").close();
            System.out.println(db.getString(0, "Id"));
            LinkedHashMap linkedHashMap = db.getRowLinkedHashMap(0);
            db.close();
        } catch (SQLException sqlEx) {
            throw new Error("\r\nОшибка при выполнении SQL-запроса!", sqlEx);
        }
    }

    /**
     * Тестируем
     *
     * @throws Exception - исключение
     */

    public static void main(String[] arg) throws Exception {
        test1();
    }
}
