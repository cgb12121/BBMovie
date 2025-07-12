package com.example.bbmovieuploadfile.repository;

import com.example.bbmovieuploadfile.entity.TempFileRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempFileRecordRepository extends ReactiveCrudRepository<TempFileRecord, String> {

}
