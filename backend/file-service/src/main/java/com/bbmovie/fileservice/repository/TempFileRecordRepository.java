package com.bbmovie.fileservice.repository;

import com.bbmovie.fileservice.entity.TempFileRecord;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

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

    @Modifying
    @Query("UPDATE temp_file SET is_removed = :isRemoved WHERE id = :id")
    Mono<Void> updateTempFile(String id, boolean isRemoved);

    @Query("SELECT * FROM temp_file WHERE is_removed = false")
    Mono<Iterable<TempFileRecord>> findAllTempFiles();

    @Query("SELECT * FROM temp_file WHERE uploaded_by = :uploadedBy AND is_removed = false")
    Mono<TempFileRecord> findTempFileByUploadedBy(String uploadedBy);

    @Query("SELECT * FROM temp_file WHERE temp_store_for = :tempStoreFor AND is_removed = false")
    Mono<TempFileRecord> findTempFileByTempStoreFor(String tempStoreFor);

    @Query("SELECT * FROM temp_file WHERE id = :id")
    Mono<TempFileRecord> findTempFileById(String id);

    @Modifying
    @Query("UPDATE temp_file SET is_removed = true WHERE id = :id")
    Mono<Void> removeTempFile(String id);

    @Query("UPDATE temp_file SET is_removed = true, removed_at = :removedAt WHERE id = :id")
    Mono<Void> markAsRemoved(String id, LocalDateTime removedAt);
}
