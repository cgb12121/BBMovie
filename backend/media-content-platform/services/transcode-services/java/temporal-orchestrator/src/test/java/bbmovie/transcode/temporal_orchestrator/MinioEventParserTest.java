package bbmovie.transcode.temporal_orchestrator;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.dto.UploadPurpose;
import bbmovie.transcode.temporal_orchestrator.nats.MinioEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioEventParserTest {

    @Test
    void parsesObjectCreatedWithMetadata() {
        String json = """
                {
                  "Records": [
                    {
                      "eventName": "s3:ObjectCreated:Put",
                      "s3": {
                        "bucket": { "name": "bbmovie-raw" },
                        "object": {
                          "key": "movies%2Fabc%2Ffile.mp4",
                          "size": 12345,
                          "userMetadata": {
                            "X-Amz-Meta-Purpose": "MOVIE_SOURCE",
                            "X-Amz-Meta-Upload-Id": "upload-uuid-1"
                          }
                        }
                      }
                    }
                  ]
                }
                """;
        MinioEventParser parser = new MinioEventParser(new ObjectMapper());
        List<TranscodeJobInput> out = parser.parseAll(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, out.size());
        TranscodeJobInput job = out.getFirst();
        assertEquals("upload-uuid-1", job.uploadId());
        assertEquals("bbmovie-raw", job.bucket());
        assertEquals("movies/abc/file.mp4", job.key());
        assertEquals(UploadPurpose.MOVIE_SOURCE, job.purpose());
        assertEquals(12345L, job.fileSizeBytes());
    }

    @Test
    void parsesMultipleRecordsIndependently() {
        String json = """
                {"Records":[
                  {"eventName":"s3:ObjectCreated:Put","s3":{"bucket":{"name":"b1"},"object":{"key":"a.mp4","userMetadata":{"X-Amz-Meta-Purpose":"MOVIE_SOURCE","X-Amz-Meta-Upload-Id":"u1"}}}},
                  {"eventName":"s3:ObjectCreated:Put","s3":{"bucket":{"name":"b2"},"object":{"key":"c.mp4","userMetadata":{"X-Amz-Meta-Purpose":"MOVIE_TRAILER","X-Amz-Meta-Upload-Id":"u2"}}}},
                  {"eventName":"s3:ObjectRemoved:Delete","s3":{"bucket":{"name":"x"},"object":{"key":"gone"}}}
                ]}
                """;
        MinioEventParser parser = new MinioEventParser(new ObjectMapper());
        List<TranscodeJobInput> out = parser.parseAll(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, out.size());
        assertEquals("u1", out.getFirst().uploadId());
        assertEquals("u2", out.get(1).uploadId());
    }

    @Test
    void skipsNonCreationEvents() {
        String json = """
                {"Records":[{"eventName":"s3:ObjectRemoved:Delete","s3":{"bucket":{"name":"b"},"object":{"key":"k"}}}]}
                """;
        MinioEventParser parser = new MinioEventParser(new ObjectMapper());
        assertTrue(parser.parseAll(json.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }
}
