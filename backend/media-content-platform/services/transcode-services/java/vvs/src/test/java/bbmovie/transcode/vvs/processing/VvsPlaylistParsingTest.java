package bbmovie.transcode.vvs.processing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VvsPlaylistParsingTest {

    @Test
    void parsesSegmentsFollowingExtinfOnly() {
        String playlist = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-TARGETDURATION:6
                #EXT-X-MEDIA-SEQUENCE:0
                #EXTINF:6.000,
                seg-000.ts
                #EXTINF:6.000,
                seg-001.ts
                #EXT-X-ENDLIST
                """;
        List<String> segs = VvsQualityProcessingService.parseMediaPlaylistSegments(playlist);
        assertEquals(List.of("seg-000.ts", "seg-001.ts"), segs);
    }

    @Test
    void ignoresNonSegmentLines() {
        String playlist = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-TARGETDURATION:6
                # comment
                seg-no-extinf.ts
                #EXTINF:6.000,
                seg-000.ts
                """;
        List<String> segs = VvsQualityProcessingService.parseMediaPlaylistSegments(playlist);
        assertEquals(1, segs.size());
        assertTrue(segs.contains("seg-000.ts"));
    }
}

