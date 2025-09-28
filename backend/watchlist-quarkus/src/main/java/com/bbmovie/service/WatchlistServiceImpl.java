package com.bbmovie.service;

import com.bbmovie.entity.CollectionItem;
import com.bbmovie.entity.enums.WatchStatus;
import com.bbmovie.entity.WatchlistCollection;
import com.bbmovie.repo.CollectionItemRepository;
import com.bbmovie.repo.WatchlistCollectionRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistCollectionRepository collectionRepo;
    private final CollectionItemRepository itemRepo;

    @Inject
    public WatchlistServiceImpl(WatchlistCollectionRepository collectionRepo, CollectionItemRepository itemRepo) {
        this.collectionRepo = collectionRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    public PanacheQuery<WatchlistCollection> listCollections(UUID userId, int page, int size) {
        return collectionRepo.findByUser(userId).page(Page.of(page, size));
    }

    @Override
    public WatchlistCollection createCollection(UUID userId, String name, String description, boolean isPublic) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Collection name must not be blank");
        }
        if (collectionRepo.existsByUserAndName(userId, name)) {
            throw new IllegalArgumentException("Collection name already exists");
        }

        WatchlistCollection collection = new WatchlistCollection();
        collection.setUserId(userId);
        collection.setName(name.trim());
        collection.setDescription(description == null ? null : description.trim());
        collection.setPublic(isPublic);

        collectionRepo.persist(collection);
        return collection;
    }

    @Override
    public WatchlistCollection renameCollection(UUID userId, UUID collectionId, String newName, String description, boolean isPublic) {
        WatchlistCollection collection = collectionRepo.findByIdOptional(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        if (!collection.getUserId().equals(userId)) {
            throw new SecurityException("Forbidden: not owner");
        }

        if (!collection.getName().equals(newName) && collectionRepo.existsByUserAndName(userId, newName)) {
            throw new IllegalArgumentException("Collection name already exists");
        }

        collection.setName(newName.trim());
        collection.setDescription(description == null ? null : description.trim());
        collection.setPublic(isPublic);

        return collection;
    }

    @Override
    public void deleteCollection(UUID userId, UUID collectionId) {
        collectionRepo.findByIdOptional(collectionId).ifPresent(collection -> {
            if (!collection.getUserId().equals(userId)) {
                throw new SecurityException("Forbidden: not owner");
            }
            collectionRepo.delete(collection);
        });
    }

    @Override
    public PanacheQuery<CollectionItem> listItems(UUID userId, UUID collectionId, int page, int size) {
        WatchlistCollection collection = collectionRepo.findByIdOptional(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        return itemRepo.findByCollection(collection).page(Page.of(page, size));
    }

    @Override
    public CollectionItem addItem(UUID userId, UUID collectionId, UUID movieId, WatchStatus status, String notes) {
        WatchlistCollection collection = collectionRepo.findByIdOptional(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        CollectionItem existing = itemRepo.findByCollectionAndMovie(collection, movieId);
        if (existing != null) {
            return existing;
        }

        CollectionItem item = new CollectionItem();
        item.setCollection(collection);
        item.setMovieId(movieId);
        item.setWatchStatus(status != null ? status : WatchStatus.PLANNING);
        item.setNotes((notes == null || notes.isBlank()) ? null : notes);

        itemRepo.persist(item);
        return item;
    }

    @Override
    public CollectionItem updateItem(UUID collectionId, UUID movieId, WatchStatus status, String notes) {
        WatchlistCollection collection = collectionRepo.findByIdOptional(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        CollectionItem existing = itemRepo.findByCollectionAndMovie(collection, movieId);
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in collection");
        }

        if (status != null) {
            existing.setWatchStatus(status);
        }
        existing.setNotes(notes);

        return existing;
    }

    @Override
    public void removeItem(UUID userId, UUID collectionId, UUID movieId) {
        collectionRepo.findByIdOptional(collectionId).ifPresent(collection -> {
            if (!collection.getUserId().equals(userId)) {
                throw new SecurityException("Forbidden: not owner");
            }
            itemRepo.deleteByCollectionAndMovie(collection, movieId);
        });
    }
}


