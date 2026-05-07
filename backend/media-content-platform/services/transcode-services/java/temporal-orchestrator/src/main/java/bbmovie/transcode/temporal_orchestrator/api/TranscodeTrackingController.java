package bbmovie.transcode.temporal_orchestrator.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeTrackingResponse;

@RestController
@RequestMapping("/api/transcode")
@RequiredArgsConstructor
public class TranscodeTrackingController {

    private final TranscodeTrackingService transcodeTrackingService;

    @GetMapping("/{uploadId}/status")
    public ResponseEntity<TranscodeTrackingResponse> getStatus(@PathVariable String uploadId) {
        return ResponseEntity.ok(transcodeTrackingService.getStatus(uploadId));
    }
}
