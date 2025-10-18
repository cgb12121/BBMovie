package com.bbmovie.fileservice.service.concurrency;

public record PrioritizedTask(Runnable task, TaskPriority priority) implements Runnable, Comparable<PrioritizedTask> {

    @Override
    public void run() {
        task.run();
    }

    @Override
    public int compareTo(PrioritizedTask other) {
        return Integer.compare(this.priority.ordinal(), other.priority.ordinal());
    }
}
