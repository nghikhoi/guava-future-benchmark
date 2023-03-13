package me.nghikhoi.benchmark;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public enum ThreadSet {

    BOTH_DIRECT(() -> Executors.newFixedThreadPool(12), MoreExecutors::directExecutor, MoreExecutors::directExecutor)
    , BOTH_POOL(() -> Executors.newFixedThreadPool(12), () -> Executors.newFixedThreadPool(12), () -> Executors.newFixedThreadPool(24))
    , CALLBACK_DIRECT(() -> Executors.newFixedThreadPool(12), MoreExecutors::directExecutor, () -> Executors.newFixedThreadPool(24));

    private Executor callExecutor;
    private Executor callbackExecutor;
    private Executor transformExecutor;

    public Executor getCallExecutor() {
        return callExecutor;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    public Executor getTransformExecutor() {
        return transformExecutor;
    }

    private Supplier<Executor> callExecutorSupplier;
    private Supplier<Executor> callbackExecutorSupplier;
    private Supplier<Executor> transformExecutorSupplier;

    ThreadSet(Supplier<Executor> callExecutorSupplier, Supplier<Executor> callbackExecutorSupplier, Supplier<Executor> transformExecutorSupplier) {
        this.callExecutorSupplier = callExecutorSupplier;
        this.callbackExecutorSupplier = callbackExecutorSupplier;
        this.transformExecutorSupplier = transformExecutorSupplier;
    }

    public void init() {
        callExecutor = callExecutorSupplier.get();
        callbackExecutor = callbackExecutorSupplier.get();
        transformExecutor = transformExecutorSupplier.get();
    }

    public void shutdown() {
        if (callExecutor instanceof ExecutorService) {
            ((ExecutorService) callExecutor).shutdown();
        }
        if (callbackExecutor instanceof ExecutorService) {
            ((ExecutorService) callbackExecutor).shutdown();
        }
        if (transformExecutor instanceof ExecutorService) {
            ((ExecutorService) transformExecutor).shutdown();
        }
    }

}
