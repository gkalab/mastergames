package com.kalab.database;

public interface Progress {
    boolean isCancelled();

    void publishProgress(int value);
}
