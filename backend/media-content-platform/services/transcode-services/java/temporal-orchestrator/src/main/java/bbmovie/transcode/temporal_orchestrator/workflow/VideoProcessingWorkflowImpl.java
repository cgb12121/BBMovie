package bbmovie.transcode.temporal_orchestrator.workflow;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.DecisionHintsV2;
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
import java.util.Objects;

/**
 * Temporal workflow implementation for end-to-end transcode processing.
 *
 * <p>Flow: analyze -> fan-out encodes -> fan-out quality checks (selected rungs) -> publish master
 * manifest. Failures in encode/quality paths fail the workflow to preserve output integrity.</p>
 */
public class VideoProcessingWorkflowImpl implements VideoProcessingWorkflow {

    private record PlannedRung(String label, int width, int height) {
    }

    @Override
    /** Executes orchestration pipeline for a single uploaded object notification. */
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

        // Plan once from source metadata, then fan-out independent per-rung encode activities.
        List<PlannedRung> plan = planRungs(metadata.height(), metadata.decisionHints());
        List<Promise<RungResultDTO>> encodePromises = new ArrayList<>();
        List<Promise<QualityReportDTO>> qualityPromises = new ArrayList<>();
        DecisionHintsV2 hints = metadata.decisionHints();
        for (PlannedRung rung : plan) {
            EncodeRequest req = new EncodeRequest(
                    input.uploadId(),
                    rung.label(),
                    rung.width(),
                    rung.height(),
                    "00",
                    "00",
                    input.bucket(),
                    input.key(),
                    hints != null ? hints.recommendedPreset() : null,
                    hints != null ? hints.minBitrateKbps() : null,
                    hints != null ? hints.maxBitrateKbps() : null,
                    hints != null && hints.conservativeMode(),
                    hints != null
                            ? Objects.requireNonNullElse(hints.encodeBitrateStrategy(), EncodeBitrateStrategy.VBV_ABR)
                            : EncodeBitrateStrategy.DEFAULT,
                    hints != null ? hints.recommendedCrf() : null
            );
            Promise<RungResultDTO> encodePromise = Async.function(encoding::encodeResolution, req);
            encodePromises.add(encodePromise);
            // Pipeline fan-out: quality of a rung starts as soon as that rung encode finishes.
            if (rung.height() >= 720) {
                Promise<QualityReportDTO> qualityPromise = encodePromise.thenCompose(result -> {
                    if (!result.success()) {
                        return Async.function(() -> new QualityReportDTO(result.resolution(), false, 0, "encode_failed"));
                    }
                    ValidationRequest vreq = new ValidationRequest(
                            input.uploadId(),
                            result.playlistPath(),
                            result.resolution(),
                            rung.width(),
                            rung.height()
                    );
                    return Async.function(quality::validateAndScore, vreq);
                });
                qualityPromises.add(qualityPromise);
            }
        }

        Promise.allOf(encodePromises.toArray(new Promise[0])).get();
        List<RungResultDTO> rungResults = encodePromises.stream().map(Promise::get).toList();
        // Any failed rung fails workflow; master should only be produced from fully successful plan.
        for (RungResultDTO rungResult : rungResults) {
            if (!rungResult.success()) {
                throw new RuntimeException(
                        "Encode failed for rendition " + rungResult.resolution());
            }
        }

        if (!qualityPromises.isEmpty()) {
            Promise.allOf(qualityPromises.toArray(new Promise[0])).get();
            for (Promise<QualityReportDTO> p : qualityPromises) {
                QualityReportDTO report = p.get();
                if (!report.passed()) {
                    throw new RuntimeException("Quality validation failed for " + report.renditionLabel());
                }
            }
        }

        FinalManifestDTO manifest = analysis.generateMasterManifest(rungResults);
        Workflow.getLogger(VideoProcessingWorkflowImpl.class)
                .info("Transcode finished uploadId={} master={}", input.uploadId(), manifest.masterPlaylistPath());
    }

    /** Builds rung plan from source height and optional decision-hint constraints. */
    private static List<PlannedRung> planRungs(int sourceHeight, DecisionHintsV2 decisionHints) {
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
        if (decisionHints == null) {
            return rungs;
        }
        List<PlannedRung> filtered = rungs.stream()
                .filter(r -> decisionHints.skipRungs() == null || !decisionHints.skipRungs().contains(r.label()))
                .toList();
        if (filtered.isEmpty()) {
            filtered = rungs;
        }
        int maxRungs = Math.max(1, decisionHints.maxRungs());
        if (filtered.size() > maxRungs) {
            filtered = filtered.subList(0, maxRungs);
        }
        return filtered;
    }
}
