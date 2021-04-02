package org.pharosnet.vertx.faas.database.api;

import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * if affected == 0 or affected != stream.count(), return empty.
 * @param <R> Table
 * @param <ID> Id
 */
public interface AbstractDAL<R, ID> {

    Future<Void> begin(SqlContext context);
    Future<Void> commit(SqlContext context);
    Future<Void> rollback(SqlContext context);
    Future<QueryResult> query(SqlContext context, QueryArg arg);

    Future<Optional<R>> get(SqlContext context, ID id);
    Future<Optional<Stream<R>>> get(SqlContext context, Stream<ID> ids);
    Future<Optional<R>> insert(SqlContext context, R row);
    Future<Optional<Stream<R>>> insert(SqlContext context, Stream<R> rows);
    Future<Optional<R>> update(SqlContext context, R row);
    Future<Optional<Stream<R>>> update(SqlContext context, Stream<R> rows);
    Future<Optional<R>> delete(SqlContext context, R row);
    Future<Optional<Stream<R>>> delete(SqlContext context, Stream<R> rows);
    Future<Optional<R>> deleteForce(SqlContext context, R row);
    Future<Optional<Stream<R>>> deleteForce(SqlContext context, Stream<R> rows);

}
