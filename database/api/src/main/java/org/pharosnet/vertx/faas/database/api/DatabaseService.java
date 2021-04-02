package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

@VertxGen
@ProxyGen
public interface DatabaseService {

    String SERVICE_ADDRESS = "faas-database-service";
    String SERVICE_NAME = "faas-database-service";

    static DatabaseService proxy(Vertx vertx) {
        return new DatabaseServiceVertxEBProxy(vertx, SERVICE_ADDRESS);
    }

    void begin(SqlContext context, Handler<AsyncResult<TransactionResult>> handler);

    void commit(SqlContext context, Handler<AsyncResult<TransactionResult>> handler);

    void rollback(SqlContext context, Handler<AsyncResult<TransactionResult>> handler);

    void query(SqlContext context, QueryArg arg, Handler<AsyncResult<QueryResult>> handler);

}
