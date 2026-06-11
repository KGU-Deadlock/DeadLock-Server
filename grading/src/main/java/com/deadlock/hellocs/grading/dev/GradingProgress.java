package com.deadlock.hellocs.grading.dev;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GradingProgress {

    private final AtomicInteger processedUsers = new AtomicInteger(0);
    private final AtomicInteger totalUsers     = new AtomicInteger(0);
    private final AtomicLong    insertedDocs   = new AtomicLong(0);
    private volatile boolean    running        = false;

    public void start(int total) {
        processedUsers.set(0);
        insertedDocs.set(0);
        totalUsers.set(total);
        running = true;
    }

    public void advance(int processedUsers, long insertedDocs) {
        this.processedUsers.set(processedUsers);
        this.insertedDocs.set(insertedDocs);
    }

    public void finish() {
        running = false;
    }

    public Snapshot snapshot() {
        return new Snapshot(running, processedUsers.get(), totalUsers.get(), insertedDocs.get());
    }

    public record Snapshot(boolean running, int processedUsers, int totalUsers, long insertedDocs) {}
}
