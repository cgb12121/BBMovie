package bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo;

import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findByPaymentIdOrderByOccurredAtAscIdAsc(String paymentId);

    List<LedgerEntryEntity> findByUserIdOrderByOccurredAtDescIdDesc(String userId);

    List<LedgerEntryEntity> findBySubscriptionIdOrderByOccurredAtDescIdDesc(String subscriptionId);

    List<LedgerEntryEntity> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    @Query("""
        select le
        from LedgerEntryEntity le
        where (:provider is null or le.provider = :provider)
          and (:status is null or le.status = :status)
          and (:userId is null or le.userId = :userId)
          and (:subscriptionId is null or le.subscriptionId = :subscriptionId)
          and (:fromTs is null or le.occurredAt >= :fromTs)
          and (:toTs is null or le.occurredAt <= :toTs)
        order by le.occurredAt desc, le.id desc
    """)
    List<LedgerEntryEntity> search(
            @Param("provider") String provider,
            @Param("status") String status,
            @Param("userId") String userId,
            @Param("subscriptionId") String subscriptionId,
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            Pageable pageable
    );

    List<LedgerEntryEntity> findByOccurredAtBetweenOrderByOccurredAtAscIdAsc(Instant from, Instant to);

    long countByOccurredAtAfter(Instant since);

    @Query("""
        select coalesce(le.provider, 'UNKNOWN'), count(le) 
        from LedgerEntryEntity le 
        group by le.provider
    """)
    List<Object[]> countByProvider();

    @Query("""
        select coalesce(le.status, 'UNKNOWN'), count(le) 
        from LedgerEntryEntity le 
        group by le.status
    """)
    List<Object[]> countByStatus();

    @Query("""
        select le.entryType, count(le) 
        from LedgerEntryEntity le 
        group by le.entryType
    """)
    List<Object[]> countByEntryType();
}
