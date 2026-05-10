package bbmovie.ai_platform.agentic_ai.repository;

import bbmovie.ai_platform.agentic_ai.entity.ApprovalRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRepository extends ReactiveCrudRepository<ApprovalRequest, String> {
}
