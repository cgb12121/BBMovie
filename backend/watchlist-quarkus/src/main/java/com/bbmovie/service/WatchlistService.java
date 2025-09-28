package com.bbmovie.service;


import com.bbmovie.entity.CollectionItem;
import com.bbmovie.entity.enums.WatchStatus;
import com.bbmovie.entity.WatchlistCollection;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

import java.util.UUID;

public interface WatchlistService {
    PanacheQuery<WatchlistCollection> listCollections(UUID userId, int page, int size);
    WatchlistCollection createCollection(UUID userId, String name, String description, boolean isPublic);
    WatchlistCollection renameCollection(UUID userId, UUID collectionId, String newName, String description, boolean isPublic);
    void deleteCollection(UUID userId, UUID collectionId);

    PanacheQuery<CollectionItem> listItems(UUID userId, UUID collectionId, int page, int size);
    CollectionItem addItem(UUID userId, UUID collectionId, UUID movieId, WatchStatus status, String notes);
    CollectionItem updateItem(UUID collectionId, UUID movieId, WatchStatus status, String notes);
    void removeItem(UUID userId, UUID collectionId, UUID movieId);
}


