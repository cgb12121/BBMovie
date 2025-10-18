package com.bbmovie.fileservice.repository;

import com.bbmovie.fileservice.entity.cdc.OutboxFileRecord;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TempFileRecordRepository extends R2dbcRepository<OutboxFileRecord, String> {

    @Modifying
    @Query(""" 
        insert into outbox_file_record (
            id, file_name, extension, size, temp_dir, temp_store_for, uploaded_by, is_removed, created_at
        )
        values (
            :#{#newRecord.id}, :#{#newRecord.fileName}, :#{#newRecord.extension}, :#{#newRecord.size},:#{#newRecord.tempDir},
            :#{#newRecord.tempStoreFor}, :#{#newRecord.uploadedBy}, :#{#newRecord.isRemoved}, :#{#newRecord.createdAt}
        )
    """)
    Mono<Void> saveTempFile(@Param("newRecord") OutboxFileRecord newRecord);

    @Query("SELECT * FROM temp_file WHERE is_removed = false")
    Flux<OutboxFileRecord> findAllByIsRemovedFalse();

    @Query("SELECT * FROM temp_file WHERE file_name = :fileName")
    Mono<OutboxFileRecord> findByFileName(String fileName);

    @Query("""
        UPDATE temp_file
        SET is_removed = true,
            removed_at = CURRENT_TIMESTAMP
        WHERE file_name = :fileName
    """)
    Mono<Integer> markAsRemovedByFileName(String fileName);
}