package com.example.bbmovieuploadfile.repository;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatus(OutboxStatus status);

    void deleteAllByStatus(OutboxStatus status);
}
