package com.example.bbmoviesearch.service;

import ai.djl.engine.Engine;
import ai.djl.util.Utils;
import ai.djl.Device;
import ai.djl.util.cuda.CudaUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.MemoryUsage;
import java.nio.file.*;

@Log4j2
@Component
public class SystemInfo {

    private final Environment environment;
    private final boolean shouldLogSystemProperties;
    private final boolean isDjlEnabled;

    @Autowired
    public SystemInfo(Environment environment) {
        this.environment = environment;
        this.shouldLogSystemProperties = environment.getProperty("log.system.properties", Boolean.class, false);
        this.isDjlEnabled = environment.getProperty("embedding.provider.djl.enable", Boolean.class, false);
    }

    @PostConstruct
    public void inspect() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDev = true;
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                isDev = false;
                break;
            }
        }

        if (shouldLogSystemProperties && isDev && isDjlEnabled) {
            log.fatal("Logging system properties. This is a security risk, should only be enabled for debugging, not for production.");
            log.warn("To disable this warning, set log.system.properties=false in application.properties.");
            log.warn("Should not use DjL Engine.debugEnvironment() in production.");
            debugEnvironmentSafe();
        }
    }

    private static void debugEnvironmentSafe() {
        log.info("----------- System Properties -----------");
        System.getProperties().forEach((k, v) -> {
            String key = (String) k;
            if (!"java.class.path".equalsIgnoreCase(key)) {
                print(key, v);
            }
        });

        log.info("--------- Environment Variables ---------");
        Utils.getenv().forEach(SystemInfo::print);

        log.info("-------------- Directories --------------");
        try {
            Path temp = Paths.get(System.getProperty("java.io.tmpdir"));
            log.info("temp directory: {}", temp);
            Path tmpFile = Files.createTempFile("test", ".tmp");
            Files.delete(tmpFile);

            Path cacheDir = Utils.getCacheDir();
            log.info("DJL cache directory: {}", cacheDir.toAbsolutePath());

            Path path = Utils.getEngineCacheDir();
            log.info("Engine cache directory: {}", path.toAbsolutePath());
            Files.createDirectories(path);
            if (!Files.isWritable(path)) {
                log.info("Engine cache directory is not writable!!!");
            }
        } catch (Throwable e) {
            log.error("Failed to open directories", e);
        }

        log.info("------------------ CUDA -----------------");
        int gpuCount = CudaUtils.getGpuCount();
        log.info("GPU Count: {}", gpuCount);
        if (gpuCount > 0) {
            log.info("CUDA: {}", CudaUtils.getCudaVersionString());
            log.info("ARCH: {}", CudaUtils.getComputeCapability(0));
        }
        for (int i = 0; i < gpuCount; ++i) {
            Device device = Device.gpu(i);
            MemoryUsage mem = CudaUtils.getGpuMemory(device);
            log.info("GPU({}) memory used: {} bytes", i, mem.getCommitted());
        }

        log.info("----------------- Engines ---------------");
        log.info("DJL version: {}", Engine.getDjlVersion());
        log.info("Default Engine: {}", Engine.getInstance());
        log.info("Default Device: {}", Engine.getInstance().defaultDevice());
    }

    private static void print(String k, Object v) {
        log.info("{}: {}", k, v);
    }
}
