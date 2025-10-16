package com.bbmovie.search.service;

import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.util.Utils;
import ai.djl.util.cuda.CudaUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
@Component
public class SystemInfo {

    private final Environment environment;
    private final boolean logGeneral;
    private final boolean logDjl;
    private final boolean logCuda;

    @Autowired
    public SystemInfo(Environment environment) {
        this.environment = environment;
        this.logGeneral = environment.getProperty("log.system.info.general.enabled", Boolean.class, true);
        this.logDjl = environment.getProperty("log.system.info.djl.enabled", Boolean.class, true);
        this.logCuda = environment.getProperty("log.system.info.cuda.enabled", Boolean.class, true);
    }

    @PostConstruct
    public void inspect() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("Active Spring Boot Profiles: [{}]", String.join(", ", activeProfiles));
        boolean isProd = false;
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                isProd = true;
                break;
            }
        }

        if (isProd) {
            return; // Do not log any system info in production
        }

        if (logGeneral) {
            logGeneralSystemInfo();
        }
        if (logDjl) {
            logDjlInfo();
        }
        if (logCuda) {
            logCudaInfo();
        }
    }

    private void logGeneralSystemInfo() {
        log.info("---------- General System Info ----------");
        // OS Info
        log.info("OS Name: {}", System.getProperty("os.name"));
        log.info("OS Version: {}", System.getProperty("os.version"));
        log.info("OS Arch: {}", System.getProperty("os.arch"));

        // JVM Info
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Java Vendor: {}", System.getProperty("java.vendor"));
        log.info("JVM Name: {}", System.getProperty("java.vm.name"));

        // CPU Info
        log.info("Available CPU Cores: {}", Runtime.getRuntime().availableProcessors());

        // Memory Info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        log.info("JVM Heap Memory: Used {} MB / Max {} MB", heapUsed, heapMax);
    }

    private void logDjlInfo() {
        log.info("--------------- DJL Info ----------------");
        try {
            log.info("DJL Version: {}", Engine.getDjlVersion());
            log.info("Default Engine: {}", Engine.getInstance().getEngineName());
            log.info("Default Device: {}", Engine.getInstance().defaultDevice());

            Path cacheDir = Utils.getCacheDir();
            log.info("DJL Cache Directory: {}", cacheDir.toAbsolutePath());

            Path engineCacheDir = Utils.getEngineCacheDir();
            log.info("Engine Cache Directory: {}", engineCacheDir.toAbsolutePath());
            Files.createDirectories(engineCacheDir);
            if (!Files.isWritable(engineCacheDir)) {
                log.warn("DJL Engine cache directory is not writable!");
            }
        } catch (Exception e) {
            log.error("Could not retrieve DJL information.", e);
        }
    }

    private void logCudaInfo() {
        log.info("---------------- CUDA Info ----------------");
        try {
            int gpuCount = CudaUtils.getGpuCount();
            log.info("Detected GPU Count: {}", gpuCount);

            if (gpuCount > 0) {
                log.info("CUDA Version: {}", CudaUtils.getCudaVersionString());
                for (int i = 0; i < gpuCount; ++i) {
                    Device device = Device.gpu(i);
                    MemoryUsage mem = CudaUtils.getGpuMemory(device);
                    long total = mem.getMax() / (1024 * 1024);
                    long used = mem.getCommitted() / (1024 * 1024);
                    log.info("GPU {}: Used: {} MB / Total: {} MB", i, used, total);
                }
            }
        } catch (Exception e) {
            log.error("Could not retrieve CUDA information. This is normal if CUDA is not installed.");
        }
    }
}
