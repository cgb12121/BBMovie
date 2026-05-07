package bbmovie.transcode.vvs.processing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VvsSegmentSelectionTest {

    @Test
    void selectsFirstWhenSampleCountIsOne() {
        List<String> segs = List.of("a.ts", "b.ts", "c.ts");
        assertEquals(List.of("a.ts"), VvsQualityProcessingService.selectSampleSegments(segs, 1));
    }

    @Test
    void selectsFirstAndLastWhenSampleCountIsTwo() {
        List<String> segs = List.of("a.ts", "b.ts", "c.ts", "d.ts");
        assertEquals(List.of("a.ts", "d.ts"), VvsQualityProcessingService.selectSampleSegments(segs, 2));
    }

    @Test
    void selectsFirstMiddleLastWhenSampleCountIsThree() {
        List<String> segs = List.of("a.ts", "b.ts", "c.ts", "d.ts", "e.ts");
        assertEquals(List.of("a.ts", "c.ts", "e.ts"), VvsQualityProcessingService.selectSampleSegments(segs, 3));
    }
}

