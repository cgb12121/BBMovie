package bbmovie.transcode.temporal_orchestrator;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.dto.UploadPurpose;
import bbmovie.transcode.temporal_orchestrator.stub.StubMediaActivities;
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
        StubMediaActivities stub = new StubMediaActivities();

        Worker orchestrator = testEnv.newWorker(TemporalTaskQueues.ORCHESTRATOR);
        orchestrator.registerWorkflowImplementationTypes(VideoProcessingWorkflowImpl.class);

        Worker analysis = testEnv.newWorker(TemporalTaskQueues.ANALYSIS);
        analysis.registerActivitiesImplementations(stub);

        Worker encoding = testEnv.newWorker(TemporalTaskQueues.ENCODING);
        encoding.registerActivitiesImplementations(stub);

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
}
