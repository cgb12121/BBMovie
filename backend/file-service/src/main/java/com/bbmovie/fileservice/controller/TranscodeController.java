package com.bbmovie.fileservice.controller;

import com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

@RestController
@RequestMapping("/video/edit")
@RequiredArgsConstructor
public class TranscodeController {

    private final VideoTranscoderService videoTranscoderService;

    @PostMapping("/remove-audio")
    public Mono<ResponseEntity<String>> removeAudio(
            @RequestParam String inputPath, @RequestParam String outputDir, @RequestParam String outputFilename
    ) {
        return videoTranscoderService.removeAudio(Paths.get(inputPath), outputDir, outputFilename)
                .map(path -> ResponseEntity.ok(path.toString()))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body("Error when remove audio from file: " + e.getMessage()))
                );
    }

    @PostMapping("/add-audio")
    public Mono<ResponseEntity<String>> addAudio(
            @RequestParam String videoPath, @RequestParam String audioPath,
            @RequestParam String outputDir, @RequestParam String outputFilename
    ) {
        return videoTranscoderService.addAudio(
                        Paths.get(videoPath), Paths.get(audioPath), outputDir, outputFilename
                )
                .map(path -> ResponseEntity.ok(path.toString()))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body("Error when trying to add audio to file: " + e.getMessage()))
                );
    }

    @PostMapping("/add-subtitles")
    public Mono<ResponseEntity<String>> addSubtitles(
            @RequestParam String videoPath, @RequestParam String subtitlePath,
            @RequestParam String outputDir, @RequestParam String outputFilename
    ) {
        return videoTranscoderService.addSubtitles(
                        Paths.get(videoPath), Paths.get(subtitlePath), outputDir, outputFilename)
                .map(path -> ResponseEntity.ok(path.toString()))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body("Error when trying to add subtitles: " + e.getMessage()))
                );
    }

    @PostMapping("/add-commentary")
    public Mono<ResponseEntity<String>> addCommentary(
            @RequestParam String videoPath, @RequestParam String audioPath,
            @RequestParam String outputDir, @RequestParam String outputFilename,
            @RequestParam double volumeReduction
    ) {
        String safeVideoPath = FilenameUtils.getFullPath(videoPath);
        String safeAudioPath = FilenameUtils.getFullPath(audioPath);
        return videoTranscoderService.addCommentaryWithLoweredAudio(
                        Paths.get(safeVideoPath), Paths.get(safeAudioPath), outputDir, outputFilename, volumeReduction
                )
                .map(path -> ResponseEntity.ok(path.toString()))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body("Error when trying to add commentary: " + e.getMessage()))
                );
    }
}
