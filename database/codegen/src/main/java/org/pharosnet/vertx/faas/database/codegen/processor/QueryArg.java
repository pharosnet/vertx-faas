package org.pharosnet.vertx.faas.database.codegen.processor;

public class QueryArg {

    public QueryArg(int pos, String name) {
        this.pos = pos;
        this.name = name;
    }

    private int pos;
    private String name;

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
