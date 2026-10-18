package com.traceiq.demo.chaos;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ChaosState {
    private final AtomicLong latencyDelayMs = new AtomicLong(0);
    private final AtomicInteger errorRatePercent = new AtomicInteger(0);
    private final AtomicBoolean memoryLeakActive = new AtomicBoolean(false);

    public void setLatencyDelayMs(long delay) {
        this.latencyDelayMs.set(delay);
    }

    public long getLatencyDelayMs() {
        return this.latencyDelayMs.get();
    }

    public void setErrorRatePercent(int rate) {
        this.errorRatePercent.set(Math.max(0, Math.min(100, rate)));
    }

    public int getErrorRatePercent() {
        return this.errorRatePercent.get();
    }

    public void reset() {
        this.latencyDelayMs.set(0);
        this.errorRatePercent.set(0);
        this.memoryLeakActive.set(false);
    }
}
