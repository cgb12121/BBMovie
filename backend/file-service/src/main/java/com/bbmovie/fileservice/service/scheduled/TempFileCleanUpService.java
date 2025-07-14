package com.bbmovie.fileservice.service.scheduled;

import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class TempFileCleanUpService {

    private final TempFileRecordRepository tempFileRecordRepository;

    @Autowired
    public TempFileCleanUpService(TempFileRecordRepository tempFileRecordRepository) {
        this.tempFileRecordRepository = tempFileRecordRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public Mono<Void> removeAllTempFilesFromSystem() {
        // return type: Mono<Iterable<TempFileRecord>>
        return tempFileRecordRepository.findAllTempFiles()
                .flatMapIterable(e -> e)
                .then();
    }
}
