package com.bbmovie.repo;

import com.bbmovie.entity.CollectionMember;
import com.bbmovie.entity.WatchlistCollection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CollectionMemberRepository implements PanacheRepositoryBase<CollectionMember, UUID> {
    public Optional<CollectionMember> findByCollectionAndUser(WatchlistCollection c, UUID userId) {
        return find("collection = ?1 and memberUserId = ?2", c, userId).firstResultOptional();
    }
    public boolean exists(WatchlistCollection c, UUID userId) {
        return find("collection = ?1 and memberUserId = ?2", c, userId).firstResultOptional().isPresent();
    }
}


