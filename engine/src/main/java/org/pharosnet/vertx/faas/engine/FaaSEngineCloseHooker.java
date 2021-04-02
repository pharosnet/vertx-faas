package org.pharosnet.vertx.faas.engine;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FaaSEngineCloseHooker extends Thread {

    private static final Logger log = LoggerFactory.getLogger(FaaSEngineCloseHooker.class);

    public FaaSEngineCloseHooker(Vertx vertx, List<String> deploymentIds) {
        this.vertx = vertx;
        this.callbackPromise = Promise.promise();
        this.deploymentIds = deploymentIds;
    }

    private final Vertx vertx;
    private final Promise<Void> callbackPromise;

    private final List<String> deploymentIds;

    protected Future<Void> closeCallback() {
        return this.callbackPromise.future();
    }

    private void undeploy() {
        if (this.deploymentIds != null && !this.deploymentIds.isEmpty()) {
            List<Future> futures = new ArrayList<>();
            this.deploymentIds.forEach(id -> {
                futures.add(this.vertx.undeploy(id));
            });

            CompositeFuture compositeFuture = CompositeFuture.all(futures);

            compositeFuture.onSuccess(r -> {
                if (log.isDebugEnabled()) {
                    log.debug("服务卸载成功，共卸载{}个，{}", r.list().size(), r.list().toArray());
                }
                this.callbackPromise.complete();
                this.notify();
            });
            compositeFuture.onFailure(e -> {
                log.error("服务卸载失败", e);
                this.callbackPromise.fail(e);
                this.notify();
            });

            Duration timeout = null;
            String waitTime = Optional.ofNullable(System.getenv("FAAS_CLOSE_TIMEOUT")).orElse("3s");
            try {
                if (waitTime.length() > 0) {
                    timeout = Duration.parse(waitTime);
                }
            } catch (Exception e) {
                log.error("卸载服务的超时时间解析错误，预设值为{}", waitTime, e);
            }
            try {
                if (timeout != null) {
                    this.wait(timeout.toMillis());
                } else {
                    this.wait();
                }
                this.vertx.close();
            } catch (InterruptedException e) {
                log.error("等待服务卸载失败", e);
                this.callbackPromise.fail(new Exception("等待服务卸载失败，超时！"));
            }
        } else {
            this.callbackPromise.complete();
        }
    }


    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("开始卸载服务。");
        }
        try {
            this.undeploy();
        } catch (Exception e) {
            log.error("close failed", e);
            throw new RuntimeException(e);
        }
    }

}
