package com.bbmovie.repo;

import com.bbmovie.entity.CollectionItem;
import com.bbmovie.entity.WatchlistCollection;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class CollectionItemRepository implements PanacheRepositoryBase<CollectionItem, UUID> {
    public PanacheQuery<CollectionItem> findByCollection(WatchlistCollection collection) {
        return find("collection", collection);
    }

    public CollectionItem findByCollectionAndMovie(WatchlistCollection collection, UUID movieId) {
        return find("collection = ?1 and movieId = ?2", collection, movieId).firstResult();
    }

    public void deleteByCollectionAndMovie(WatchlistCollection collection, UUID movieId) {
        delete("collection = ?1 and movieId = ?2", collection, movieId);
    }
}


