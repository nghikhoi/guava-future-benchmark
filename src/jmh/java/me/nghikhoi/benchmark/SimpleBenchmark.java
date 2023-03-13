package me.nghikhoi.benchmark;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.All)
public class SimpleBenchmark {

    @Setup(Level.Trial)
    public void setup() {

    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {

    }

    @Benchmark
    public void benchmark() {

    }

}
