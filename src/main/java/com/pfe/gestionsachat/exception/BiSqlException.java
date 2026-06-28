package com.pfe.gestionsachat.exception;

public class BiSqlException extends RuntimeException {

    private final String sqlFautif;

    public BiSqlException(String message, String sqlFautif, Throwable cause) {
        super(message, cause);
        this.sqlFautif = sqlFautif;
    }

    public String getSqlFautif() {
        return sqlFautif;
    }
}
