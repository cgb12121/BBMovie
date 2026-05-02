package bbmovie.transcode.temporal_orchestrator.workflow;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.temporal.TemporalPolicies;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;

public class VideoProcessingWorkflowImpl implements VideoProcessingWorkflow {

    private record PlannedRung(String label, int width, int height) {
    }

    @Override
    public void processUpload(TranscodeJobInput input) {
        if (!input.purpose().isVideo()) {
            Workflow.getLogger(VideoProcessingWorkflowImpl.class)
                    .info("Skipping video pipeline for non-video purpose {}", input.purpose());
            return;
        }

        MediaActivities analysis = Workflow.newActivityStub(MediaActivities.class,
                ActivityOptions.newBuilder(TemporalPolicies.analyzerOptions())
                        .setTaskQueue(TemporalTaskQueues.ANALYSIS)
                        .build());

        MediaActivities encoding = Workflow.newActivityStub(MediaActivities.class,
                ActivityOptions.newBuilder(TemporalPolicies.encoderOptions())
                        .setTaskQueue(TemporalTaskQueues.ENCODING)
                        .build());

        MediaActivities quality = Workflow.newActivityStub(MediaActivities.class,
                ActivityOptions.newBuilder(TemporalPolicies.qualityOptions())
                        .setTaskQueue(TemporalTaskQueues.QUALITY)
                        .build());

        MetadataDTO metadata = analysis.analyzeSource(input.uploadId(), input.bucket(), input.key());

        List<PlannedRung> plan = planRungs(metadata.height());
        List<Promise<RungResultDTO>> encodePromises = new ArrayList<>();
        for (PlannedRung rung : plan) {
            EncodeRequest req = new EncodeRequest(
                    input.uploadId(),
                    rung.label(),
                    rung.width(),
                    rung.height(),
                    "00",
                    "00",
                    input.bucket(),
                    input.key()
            );
            encodePromises.add(Async.function(encoding::encodeResolution, req));
        }

        Promise.allOf(encodePromises.toArray(new Promise[0])).get();

        List<RungResultDTO> rungResults = new ArrayList<>();
        for (Promise<RungResultDTO> p : encodePromises) {
            rungResults.add(p.get());
        }

        for (RungResultDTO rung : rungResults) {
            if (!rung.success()) {
                continue;
            }
            int h = parseHeightFromLabel(rung.resolution());
            if (h >= 720) {
                int w = widthFromLabel(rung.resolution());
                ValidationRequest vreq = new ValidationRequest(
                        input.uploadId(),
                        rung.playlistPath(),
                        rung.resolution(),
                        w,
                        h
                );
                QualityReportDTO report = quality.validateAndScore(vreq);
                if (!report.passed()) {
                    throw new RuntimeException("Quality validation failed for " + rung.resolution());
                }
            }
        }

        FinalManifestDTO manifest = analysis.generateMasterManifest(rungResults);
        Workflow.getLogger(VideoProcessingWorkflowImpl.class)
                .info("Transcode finished uploadId={} master={}", input.uploadId(), manifest.masterPlaylistPath());
    }

    private static List<PlannedRung> planRungs(int sourceHeight) {
        List<PlannedRung> rungs = new ArrayList<>();
        if (sourceHeight >= 1080) {
            rungs.add(new PlannedRung("1080p", 1920, 1080));
        }
        if (sourceHeight >= 720) {
            rungs.add(new PlannedRung("720p", 1280, 720));
        }
        if (sourceHeight >= 480) {
            rungs.add(new PlannedRung("480p", 854, 480));
        }
        if (rungs.isEmpty()) {
            rungs.add(new PlannedRung("480p", 854, Math.min(480, sourceHeight)));
        }
        return rungs;
    }

    private static int widthFromLabel(String label) {
        if (label == null) {
            return 0;
        }
        return switch (label) {
            case "1080p" -> 1920;
            case "720p" -> 1280;
            case "480p" -> 854;
            default -> 0;
        };
    }

    private static int parseHeightFromLabel(String label) {
        if (label == null) {
            return 0;
        }
        return switch (label) {
            case "1080p" -> 1080;
            case "720p" -> 720;
            case "480p" -> 480;
            default -> {
                try {
                    if (label.endsWith("p")) {
                        yield Integer.parseInt(label.substring(0, label.length() - 1));
                    }
                } catch (NumberFormatException ignored) {
                }
                yield 0;
            }
        };
    }
}
