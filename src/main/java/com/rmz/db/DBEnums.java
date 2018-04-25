package com.rmz.db;

public class DBEnums {

    public interface TypeName {
        String getTypeName();
    }


    public enum TYPE implements TypeName {
        ORACLE("oracle"),
        MYSQL("mysql"),
        POSTGRES("postgresql");

        private String typeName;

        TYPE(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        public static TYPE ifContains(String line) {
            for (TYPE enumValues : values()) {
                if (line.contains(enumValues.getTypeName())) {
                    return enumValues;
                }
            }
            return null;
        }
    }


}
