package com.bbmovie.fileservice.service.concurrency;

public enum TaskPriority {
    HIGH,   // For Admins, critical tasks
    MEDIUM, // For paying users
    LOW     // For free users, non-critical tasks
}
