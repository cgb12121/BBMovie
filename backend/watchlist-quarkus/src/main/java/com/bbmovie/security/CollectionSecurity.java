package com.bbmovie.security;

import com.bbmovie.entity.enums.MemberRole;
import com.bbmovie.repo.CollectionMemberRepository;
import com.bbmovie.repo.WatchlistCollectionRepository;
import io.quarkus.security.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CollectionSecurity {

    private final WatchlistCollectionRepository collectionRepo;
    private final CollectionMemberRepository memberRepo;

    @Inject
    public CollectionSecurity(WatchlistCollectionRepository collectionRepo, CollectionMemberRepository memberRepo) {
        this.collectionRepo = collectionRepo;
        this.memberRepo = memberRepo;
    }

    public void requireOwner(UUID userId, UUID collectionId) {
        if (!isOwner(userId, collectionId)) throw new ForbiddenException();
    }

    public void requireView(UUID userId, UUID collectionId) {
        if (!canView(userId, collectionId)) throw new ForbiddenException();
    }

    public void requireEdit(UUID userId, UUID collectionId) {
        if (!canEdit(userId, collectionId)) throw new ForbiddenException();
    }

    public boolean isOwner(UUID userId, UUID collectionId) {
        var col = collectionRepo.findById(collectionId);
        return col != null && userId.equals(col.getUserId());
    }

    public boolean canView(UUID userId, UUID collectionId) {
        var col = collectionRepo.findById(collectionId);
        if (col == null) return false;
        return userId.equals(col.getUserId()) || col.isPublic() || memberRepo.findByCollectionAndUser(col, userId).isPresent();
    }

    public boolean canEdit(UUID userId, UUID collectionId) {
        var col = collectionRepo.findById(collectionId);
        if (col == null) return false;
        return userId.equals(col.getUserId()) ||
                memberRepo.findByCollectionAndUser(col, userId)
                        .map(m -> m.getRole() == MemberRole.EDITOR)
                        .orElse(false);
    }
}