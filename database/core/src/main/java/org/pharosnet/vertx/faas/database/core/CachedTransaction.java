package org.pharosnet.vertx.faas.database.core;

import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

public class CachedTransaction {

    public CachedTransaction() {
    }

    public CachedTransaction(SqlConnection connection, Transaction transaction) {
        this.connection = connection;
        this.transaction = transaction;
    }

    private SqlConnection connection;
    private Transaction transaction;

    public SqlConnection getConnection() {
        return connection;
    }

    public void setConnection(SqlConnection connection) {
        this.connection = connection;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

}
