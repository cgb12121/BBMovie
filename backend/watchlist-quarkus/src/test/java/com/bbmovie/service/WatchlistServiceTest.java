package com.bbmovie.service;

import com.bbmovie.entity.CollectionItem;
import com.bbmovie.entity.WatchlistCollection;
import com.bbmovie.entity.enums.WatchStatus;
import com.bbmovie.repo.CollectionItemRepository;
import com.bbmovie.repo.WatchlistCollectionRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//@QuarkusTest
//class WatchlistServiceTest {
//
//    private WatchlistCollectionRepository collectionRepo;
//    private CollectionItemRepository itemRepo;
//    private WatchlistServiceImpl service;
//
//    @BeforeEach
//    void setUp() {
//        collectionRepo = mock(WatchlistCollectionRepository.class);
//        itemRepo = mock(CollectionItemRepository.class);
//        service = new WatchlistServiceImpl(collectionRepo, itemRepo);
//    }
//
//    @Test
//    void createCollection_success() {
//        UUID userId = UUID.randomUUID();
//        when(collectionRepo.existsByUserAndName(eq(userId), anyString())).thenReturn(false);
//
//        WatchlistCollection result = service.createCollection(userId, "My List", "desc", true);
//
//        assertEquals("My List", result.getName());
//        verify(collectionRepo).persist(any(WatchlistCollection.class));
//    }
//
//    @Test
//    void createCollection_duplicateName_throws() {
//        UUID userId = UUID.randomUUID();
//        when(collectionRepo.existsByUserAndName(eq(userId), anyString())).thenReturn(true);
//        assertThrows(IllegalArgumentException.class,
//                () -> service.createCollection(userId, "My List", null, false));
//    }
//
//    @Test
//    void renameCollection_success() {
//        UUID userId = UUID.randomUUID();
//        UUID collectionId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setId(collectionId);
//        collection.setUserId(userId);
//        collection.setName("Old");
//
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        when(collectionRepo.existsByUserAndName(userId, "New"))
//                .thenReturn(false);
//
//        WatchlistCollection updated = service.renameCollection(userId, collectionId, "New", "Desc", true);
//        assertEquals("New", updated.getName());
//        assertTrue(updated.isPublic());
//    }
//
//    @Test
//    void renameCollection_notOwner_throws() {
//        UUID userId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setUserId(UUID.randomUUID());
//        when(collectionRepo.findByIdOptional(any())).thenReturn(Optional.of(collection));
//
//        Executable action = () -> service.renameCollection(userId, UUID.randomUUID(), "New", null, false);
//        assertThrows(SecurityException.class, action);
//    }
//
//    @Test
//    void listCollections_paginates() {
//        UUID userId = UUID.randomUUID();
//        @SuppressWarnings("unchecked")
//        PanacheQuery<WatchlistCollection> query = mock(PanacheQuery.class);
//        when(collectionRepo.findByUser(userId)).thenReturn(query);
//        when(query.page(any())).thenReturn(query);
//        service.listCollections(userId, 0, 10);
//        verify(query).page(any());
//    }
//
//    @Test
//    void addItem_creates_whenMissing() {
//        UUID collectionId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        UUID movieId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setId(collectionId);
//        collection.setUserId(userId);
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        when(itemRepo.findByCollectionAndMovie(collection, movieId)).thenReturn(null);
//
//        CollectionItem item = service.addItem(userId, collectionId, movieId, WatchStatus.WATCHING, "note");
//        assertEquals(WatchStatus.WATCHING, item.getWatchStatus());
//        verify(itemRepo).persist(any(CollectionItem.class));
//    }
//
//    @Test
//    void addItem_returnsExisting_whenDuplicate() {
//        UUID collectionId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        UUID movieId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setId(collectionId);
//        collection.setUserId(userId);
//        CollectionItem existing = new CollectionItem();
//        existing.setMovieId(movieId);
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        when(itemRepo.findByCollectionAndMovie(collection, movieId)).thenReturn(existing);
//
//        CollectionItem item = service.addItem(userId, collectionId, movieId, WatchStatus.PLANNING, null);
//        assertSame(existing, item);
//        verify(itemRepo, never()).persist(any(CollectionItem.class));
//    }
//
//    @Test
//    void updateItem_updatesStatusAndNotes() {
//        UUID collectionId = UUID.randomUUID();
//        UUID movieId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        CollectionItem existing = new CollectionItem();
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        when(itemRepo.findByCollectionAndMovie(collection, movieId)).thenReturn(existing);
//
//        CollectionItem updated = service.updateItem(collectionId, movieId, WatchStatus.COMPLETED, "done");
//        assertEquals(WatchStatus.COMPLETED, updated.getWatchStatus());
//        assertEquals("done", updated.getNotes());
//    }
//
//    @Test
//    void updateItem_missing_throws() {
//        UUID collectionId = UUID.randomUUID();
//        UUID movieId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        when(itemRepo.findByCollectionAndMovie(collection, movieId)).thenReturn(null);
//        assertThrows(IllegalArgumentException.class,
//                () -> service.updateItem(collectionId, movieId, null, null));
//    }
//
//    @Test
//    void removeItem_checksOwnership() {
//        UUID collectionId = UUID.randomUUID();
//        UUID owner = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setUserId(owner);
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//        service.removeItem(owner, collectionId, UUID.randomUUID());
//        verify(itemRepo).deleteByCollectionAndMovie(eq(collection), any());
//    }
//
//    @Test
//    void removeItem_wrongOwner_throws() {
//        UUID collectionId = UUID.randomUUID();
//        WatchlistCollection collection = new WatchlistCollection();
//        collection.setUserId(UUID.randomUUID());
//        when(collectionRepo.findByIdOptional(collectionId)).thenReturn(Optional.of(collection));
//
//
//        Executable action = () -> service.removeItem(UUID.randomUUID(), collectionId, UUID.randomUUID());
//        assertThrows(SecurityException.class, action);
//    }
//}


