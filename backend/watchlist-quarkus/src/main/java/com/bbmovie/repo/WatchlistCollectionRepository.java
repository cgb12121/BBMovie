package com.bbmovie.repo;

import com.bbmovie.entity.WatchlistCollection;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class WatchlistCollectionRepository implements PanacheRepositoryBase<WatchlistCollection, UUID> {
    public PanacheQuery<WatchlistCollection> findByUser(UUID userId) {
        return find("userId", userId);
    }
    public boolean existsByUserAndName(UUID userId, String name) {
        return find("userId = ?1 and name = ?2", userId, name).firstResultOptional().isPresent();
    }
}


