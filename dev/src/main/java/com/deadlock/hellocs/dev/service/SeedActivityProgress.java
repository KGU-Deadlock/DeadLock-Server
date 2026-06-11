package com.deadlock.hellocs.dev.service;

import org.springframework.stereotype.Component;

@Component
public class SeedActivityProgress {

    public enum Phase { IDLE, USERS, GRADING, STREAK, RANKING, DONE, ERROR }

    private volatile Phase  phase        = Phase.IDLE;
    private volatile String errorMessage = null;
    private volatile long   startedAt    = 0;

    public void start() {
        this.phase        = Phase.USERS;
        this.startedAt    = System.currentTimeMillis();
        this.errorMessage = null;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public void error(String message) {
        this.phase        = Phase.ERROR;
        this.errorMessage = message;
    }

    public Snapshot snapshot() {
        long elapsed = startedAt > 0 ? (System.currentTimeMillis() - startedAt) / 1000 : 0;
        return new Snapshot(phase, elapsed, errorMessage);
    }

    public record Snapshot(Phase phase, long elapsedSeconds, String errorMessage) {}
}
