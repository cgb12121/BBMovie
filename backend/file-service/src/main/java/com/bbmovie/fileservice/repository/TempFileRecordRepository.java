package com.bbmovie.fileservice.repository;

import com.bbmovie.fileservice.entity.TempFileRecord;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TempFileRecordRepository extends ReactiveCrudRepository<TempFileRecord, String> {

    @Modifying
    @Query(""" 
        insert into temp_file (
            id, file_name, extension, size, temp_dir, temp_store_for, uploaded_by, is_removed, created_at
        )
        values (
            :#{#newRecord.id}, :#{#newRecord.fileName}, :#{#newRecord.extension}, :#{#newRecord.size},:#{#newRecord.tempDir},
            :#{#newRecord.tempStoreFor}, :#{#newRecord.uploadedBy}, :#{#newRecord.isRemoved}, :#{#newRecord.createdAt}
        )
    """)
    Mono<Void> saveTempFile(@Param("newRecord") TempFileRecord newRecord);

    @Query("SELECT * FROM temp_file WHERE is_removed = false")
    Flux<TempFileRecord> findAllByIsRemovedFalse();

    @Query("SELECT * FROM temp_file WHERE file_name = :fileName")
    Mono<TempFileRecord> findByFileName(String fileName);

    @Query("""
        UPDATE temp_file
        SET is_removed = true,
            removed_at = CURRENT_TIMESTAMP
        WHERE file_name = :fileName
    """)
    Mono<Integer> markAsRemovedByFileName(String fileName);
}