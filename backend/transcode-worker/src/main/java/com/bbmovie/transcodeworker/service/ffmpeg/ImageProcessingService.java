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

/**
 * Service class for processing image files using FFmpeg.
 * Handles image resizing and format conversion for different upload purposes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    /** FFmpeg instance used for image processing operations */
    private final FFmpeg ffmpeg;

    /**
     * Record class that represents the size parameters for image processing.
     *
     * @param name identifier for the size configuration
     * @param width the width for the output image
     * @param height the height for the output image
     */
    public record ImageSize(String name, int width, int height) {}

    /**
     * Process an image file to create multiple resized versions based on the upload purpose.
     * This method generates different sizes of the input image according to the requirements
     * defined for the specific upload purpose.
     *
     * @param input the input image file path
     * @param outputDir the directory where processed images should be stored
     * @param format the target format for the output images
     * @param purpose the upload purpose that determines which sizes to generate
     * @return a list of paths to the generated output images
     */
    @SuppressWarnings("UnusedReturnValue")
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

    /**
     * Determines the appropriate image sizes to generate based on the upload purpose.
     * This method returns different size configurations depending on the purpose of the upload,
     * such as avatar sizes for user avatars or poster sizes for movie posters.
     *
     * @param purpose the upload purpose that determines which sizes to generate
     * @return a list of ImageSize objects that define the dimensions for the output images
     */
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

    /**
     * Process a single image to resize and convert it to the specified format.
     * This method resizes the input image to the specified dimensions and saves it in the target format.
     *
     * @param input the input image file path
     * @param output the output path where the processed image should be saved
     * @param width the target width for the image (-1 for proportional scaling)
     * @param height the target height for the image (-1 for proportional scaling)
     * @param format the target format for the output image
     * @return the path to the processed image
     */
    @SuppressWarnings("UnusedReturnValue")
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
