package com.bbmovie.repo;

import com.bbmovie.entity.CollectionInvite;
import com.bbmovie.entity.WatchlistCollection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CollectionInviteRepository implements PanacheRepositoryBase<CollectionInvite, UUID> {
    public Optional<CollectionInvite> findByCollectionAndInvitee(WatchlistCollection c, UUID invitee) {
        return find("collection = ?1 and inviteeUserId = ?2", c, invitee).firstResultOptional();
    }
}


