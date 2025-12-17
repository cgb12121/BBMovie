package com.bbmovie.transcodeworker.service.ffmpeg;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final FFmpeg ffmpeg;

    public record ImageSize(String name, int width, int height) {}

    public List<Path> processImageHierarchy(Path input, String outputDir, String format, UploadPurpose purpose) {
        List<Path> outputs = new ArrayList<>();
        List<ImageSize> targetSizes = getSizesForPurpose(purpose);

        for (ImageSize size : targetSizes) {
            Path outputPath = Paths.get(outputDir, size.name() + "." + format);
            processImage(input, outputPath, size.width(), size.height(), format);
            outputs.add(outputPath);
        }
        return outputs;
    }

    private List<ImageSize> getSizesForPurpose(UploadPurpose purpose) {
        return switch (purpose) {
            case USER_AVATAR -> List.of(
                    new ImageSize("small", 64, 64),
                    new ImageSize("medium", 256, 256)
            );
            case MOVIE_POSTER -> List.of(
                    new ImageSize("thumbnail", 300, -1),
                    new ImageSize("large", 1080, -1)
            );
            default -> List.of(
                    new ImageSize("medium", 800, -1)
            );
        };
    }

    public Path processImage(Path input, Path output, int width, int height, String format) {
        try {
            String outputPath = output.toString();
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input.toString())
                    .overrideOutputFiles(true)
                    .addOutput(outputPath)
                    .setFormat(format)
                    .setVideoFilter("scale=" + width + ":" + height) // -1 or -2 for proportional
                    .addExtraArgs("-q:v", "3")
                    .done();
            
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            FFmpegJob job = executor.createJob(builder, progress -> {});
            job.run();

            return output;
        } catch (Exception e) {
            log.error("Image processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Image processing failed", e);
        }
    }
}
