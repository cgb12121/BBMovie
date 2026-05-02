# LGS — Ladder Generation Service

| Short name | **LGS** |
| Full name | **Ladder Generation Service** |
| Source | Ported from `transcode-worker` `LadderGenerationService` + `ResolutionCostCalculator` + `RecipeHints` |

Java package: `bbmovie.transcode.lgs.analysis`. Spring Boot auto-config: `LgsAnalysisAutoConfiguration` (beans `LgsLadderGenerationService`, `LgsResolutionCostCalculator`).

**Temporal:** there is no `ladder-queue`; ladder policy is used from **CAS** (embedded copy) and from **VIS** (via Maven dependency on this module). This module is the shared **library** artifact for VIS and future HTTP services.
