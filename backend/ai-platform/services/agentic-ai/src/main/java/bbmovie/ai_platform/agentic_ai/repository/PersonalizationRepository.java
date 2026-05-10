package bbmovie.ai_platform.agentic_ai.repository;

import bbmovie.ai_platform.agentic_ai.entity.AgentPersonalization;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PersonalizationRepository extends R2dbcRepository<AgentPersonalization, UUID> {
}
