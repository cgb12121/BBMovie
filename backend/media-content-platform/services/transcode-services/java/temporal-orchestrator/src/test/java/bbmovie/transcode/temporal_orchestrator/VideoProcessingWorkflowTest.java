package bbmovie.transcode.temporal_orchestrator;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.dto.UploadPurpose;
import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;
import bbmovie.transcode.contracts.dto.SubtitleJsonDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.temporal_orchestrator.workflow.VideoProcessingWorkflow;
import bbmovie.transcode.temporal_orchestrator.workflow.VideoProcessingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VideoProcessingWorkflowTest {

    private TestWorkflowEnvironment testEnv;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        MediaActivities stub = new TestStubMediaActivities();

        Worker orchestrator = testEnv.newWorker(TemporalTaskQueues.ORCHESTRATOR);
        orchestrator.registerWorkflowImplementationTypes(VideoProcessingWorkflowImpl.class);

        Worker analysis = testEnv.newWorker(TemporalTaskQueues.ANALYSIS);
        analysis.registerActivitiesImplementations(stub);

        Worker encoding = testEnv.newWorker(TemporalTaskQueues.ENCODING);
        encoding.registerActivitiesImplementations(stub);

        Worker validation = testEnv.newWorker(TemporalTaskQueues.VALIDATION);
        validation.registerActivitiesImplementations(stub);

        Worker quality = testEnv.newWorker(TemporalTaskQueues.QUALITY);
        quality.registerActivitiesImplementations(stub);

        Worker subtitle = testEnv.newWorker(TemporalTaskQueues.SUBTITLE);
        subtitle.registerActivitiesImplementations(stub);

        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void videoUploadRunsThroughStubPipeline() {
        WorkflowClient client = testEnv.getWorkflowClient();
        VideoProcessingWorkflow workflow = client.newWorkflowStub(
                VideoProcessingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TemporalTaskQueues.ORCHESTRATOR)
                        .setWorkflowId("test-wf-1")
                        .setWorkflowRunTimeout(Duration.ofMinutes(5))
                        .build()
        );
        TranscodeJobInput input = new TranscodeJobInput(
                "u1",
                "raw",
                "movies/u1/source.mp4",
                UploadPurpose.MOVIE_SOURCE,
                "video/mp4",
                1000L
        );
        assertDoesNotThrow(() -> workflow.processUpload(input));
    }

    private static class TestStubMediaActivities implements MediaActivities {
        @Override
        public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
            return new MetadataDTO(1920, 1080, 120.0, "h264");
        }

        @Override
        public RungResultDTO encodeResolution(EncodeRequest request) {
            String path = "bbmovie-hls/movies/" + request.uploadId() + "/" + request.resolution() + "/playlist.m3u8";
            return new RungResultDTO(request.resolution(), path, true);
        }

        @Override
        public QualityReportDTO validateAndScore(ValidationRequest request) {
            return new QualityReportDTO(request.renditionLabel(), true, 95.0, "stub");
        }

        @Override
        public FinalManifestDTO generateMasterManifest(java.util.List<RungResultDTO> rungs) {
            String first = rungs.stream().filter(RungResultDTO::success).map(RungResultDTO::playlistPath).findFirst()
                    .orElse("bbmovie-hls/movies/unknown/1080p/playlist.m3u8");
            String master = first.replaceFirst("/[^/]+/playlist\\.m3u8$", "/master.m3u8");
            return new FinalManifestDTO(master, true);
        }

        @Override
        public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
            return new SubtitleJsonDTO(uploadId, "{}");
        }

        @Override
        public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
            return new SubtitleJsonDTO(json.uploadId(), json.jsonPayload());
        }

        @Override
        public ManifestUpdateDTO integrateSubtitles(String uploadId, java.util.List<SubInfo> subs) {
            return new ManifestUpdateDTO("bbmovie-hls/movies/" + uploadId + "/master.m3u8", true);
        }
    }
}
