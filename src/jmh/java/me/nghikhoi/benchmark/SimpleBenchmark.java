package me.nghikhoi.benchmark;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class SimpleBenchmark {

    private static final int N = 1000;
    private static final List<Long> SLEEP_SET = new ArrayList<>(N);
    private static final String LONG_DUMP_JSON = "{\"id\": 1, \"name\": \"John Doe\", \"age\": 30, \"address\": {\"streetAddress\": \"21 2nd Street\", \"city\": \"New York\", \"state\": \"NY\", \"postalCode\": \"10021-3100\"}, \"phoneNumber\": [{\"type\": \"home\", \"number\": \"212 555-1234\"}, {\"type\": \"fax\", \"number\": \"646 555-4567\"}]}";
    private static Gson GSON = new GsonBuilder().create();

    static {
        for (int i = 0; i < N; i++) {
            SLEEP_SET.add(ThreadLocalRandom.current().nextInt(100) + 100L);
        }
    }

    private List<ListenableFuture<Object>> futures = new ArrayList<>(N);

    @Param({"BOTH_DIRECT", "BOTH_POOL", "CALLBACK_DIRECT"})
    private ThreadSet threadSet;


    private String[] dumpJsonArr = new String[]{
            "{}",
            LONG_DUMP_JSON
    };

    @Param({"1"})
    private int dumpJsonIndex;

    @Param({"STANDARD", "CUSTOM"})
    private FutureCreator futureCreator;

    @Setup(Level.Iteration)
    public void setup() {
        threadSet.init();
    }

    @TearDown(Level.Iteration)
    public void tearDown() throws Exception {
        futures.clear();
        threadSet.shutdown();
    }

    @Benchmark
    @Group()
    public void startFutures() throws ExecutionException, InterruptedException {
        assert futures.size() == 0;
        String dumpJson = dumpJsonArr[dumpJsonIndex];
        for (int i = 0; i < N; i++) {
            int finalI = i;
            ListenableFuture<Object> future = futureCreator.createFuture(threadSet, dumpJson, finalI);
            futures.add(future);
        }

        Futures.whenAllComplete(futures).call(() -> {
            return null;
        }, MoreExecutors.directExecutor()).get();
    }

    public static enum FutureCreator {

        STANDARD() {
            @Override
            public ListenableFuture<Object> createFuture(ThreadSet threadSet, String dumpJson, int finalI) {
                ListenableFuture<Object> future = Futures.submit(() -> {
                    long expired = System.currentTimeMillis() + SLEEP_SET.get(finalI);
                    while (System.currentTimeMillis() < expired) {
//                    Thread.sleep(1);
                    }
                    return null;
                }, threadSet.getCallExecutor());

                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(Object result) {
//                    System.out.println("Callback " + finalI + " completed");
                        String dump_log = "Callback " + finalI + " completed";
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        String dump_log = "Callback " + finalI + " failed";
                    }
                }, threadSet.getCallbackExecutor());

                future = Futures.transform(future, (input) -> {
                    return GSON.fromJson(dumpJson, Map.class);
                }, threadSet.getCallExecutor());
                return future;
            }
        }, CUSTOM() {
            @Override
            public ListenableFuture<Object> createFuture(ThreadSet threadSet, String dumpJson, int finalI) {
                ListenableFuture<Object> future = Futures.submit(() -> {
                    long expired = System.currentTimeMillis() + SLEEP_SET.get(finalI);
                    while (System.currentTimeMillis() < expired) {
//                    Thread.sleep(1);
                    }
                    return null;
                }, threadSet.getCallExecutor());
                LazyTransformFuture<Object, Object> n = new LazyTransformFuture<>(future, o -> {
                    String dump_log = "Callback " + finalI + " completed";
                    String dump_alog = "Callback " + finalI + " completed";
                    return GSON.fromJson(dumpJson, Map.class);
                });
                future.addListener(n, threadSet.getTransformExecutor());
                return n;
            }
        };

        public abstract ListenableFuture<Object> createFuture(ThreadSet threadSet, String dumpJson, int finalI);

    }

}
