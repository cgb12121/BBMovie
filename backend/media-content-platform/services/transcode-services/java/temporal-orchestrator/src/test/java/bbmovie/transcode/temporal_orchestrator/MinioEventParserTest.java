package bbmovie.transcode.temporal_orchestrator;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.dto.UploadPurpose;
import bbmovie.transcode.temporal_orchestrator.nats.MinioEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
        Optional<TranscodeJobInput> out = parser.parse(json.getBytes(StandardCharsets.UTF_8));
        assertTrue(out.isPresent());
        TranscodeJobInput job = out.get();
        assertEquals("upload-uuid-1", job.uploadId());
        assertEquals("bbmovie-raw", job.bucket());
        assertEquals("movies/abc/file.mp4", job.key());
        assertEquals(UploadPurpose.MOVIE_SOURCE, job.purpose());
        assertEquals(12345L, job.fileSizeBytes());
    }

    @Test
    void skipsNonCreationEvents() {
        String json = """
                {"Records":[{"eventName":"s3:ObjectRemoved:Delete","s3":{"bucket":{"name":"b"},"object":{"key":"k"}}}]}
                """;
        MinioEventParser parser = new MinioEventParser(new ObjectMapper());
        assertTrue(parser.parse(json.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }
}
