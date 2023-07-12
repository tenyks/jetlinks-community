package org.jetlinks.community.north.manager.executor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetlinks.rule.engine.api.Logger;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.Slf4jLogger;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.scope.GlobalScope;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Input;
import org.jetlinks.rule.engine.api.task.Output;
import org.jetlinks.rule.engine.defaults.scope.InMemoryGlobalScope;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

public class DummyExecutionContext implements ExecutionContext {

    private ScheduleJob sJob = new ScheduleJob();

    private Logger logger = new Slf4jLogger("ExecutionContext");

    public DummyExecutionContext() {
        this.sJob.setName("DummyExecutionContext");
        this.sJob.setContext(new HashMap<>());
        this.sJob.setExecutor("DummyExecutor");
        this.sJob.setNodeId("DummyExecutionContext");
        this.sJob.setInstanceId("I0001");
    }

    @Override
    public String getInstanceId() {
        return "EC-001";
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public ScheduleJob getJob() {
        return sJob;
    }

    @Override
    public <T> Mono<T> fireEvent(@NotNull String event, @NotNull RuleData data) {
        return null;
    }

    @Override
    public <T> Mono<T> onError(@Nullable Throwable e, @Nullable RuleData sourceData) {
        return null;
    }

    @Override
    public Input getInput() {
        return () -> Flux.empty();
    }

    @Override
    public Output getOutput() {
        return new Output() {
            @Override
            public Mono<Boolean> write(Publisher<RuleData> data) {
                return Mono.just(true);
            }

            @Override
            public Mono<Void> write(String nodeId, Publisher<RuleData> data) {
                return Mono.empty();
            }
        };
    }

    @Override
    public Mono<Void> shutdown(String code, String message) {
        return Mono.empty();
    }

    @Override
    public RuleData newRuleData(Object data) {
        return new RuleData();
    }

    @Override
    public void onShutdown(Runnable runnable) {
        runnable.run();
    }

    @Override
    public boolean isDebug() {
        return true;
    }

    @Override
    public GlobalScope global() {
        return new InMemoryGlobalScope();
    }
}
