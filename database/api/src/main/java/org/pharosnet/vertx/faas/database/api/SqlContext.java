package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.pharosnet.vertx.faas.core.context.Context;

import java.util.UUID;

@DataObject
public class SqlContext extends Context {

    public static SqlContext create(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("new sql context failed, context is null.");
        }
        SqlContext sqlContext = new SqlContext(UUID.randomUUID().toString());
        sqlContext.join(context);
        return sqlContext;
    }

    protected SqlContext(String id) {
        super(id);
    }

    public SqlContext(JsonObject jsonObject) {
        super(jsonObject);
    }

    public Context asContext() {
        return this;
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

}
