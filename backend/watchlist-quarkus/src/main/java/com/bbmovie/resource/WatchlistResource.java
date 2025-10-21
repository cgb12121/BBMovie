package com.bbmovie.resource;

import com.bbmovie.dto.ApiResponse;
import com.bbmovie.dto.PageResponse;
import com.bbmovie.dto.request.CreateCollectionRequest;
import com.bbmovie.dto.request.UpdateCollectionRequest;
import com.bbmovie.dto.request.UpsertItemRequest;
import com.bbmovie.security.CollectionSecurity;
import com.bbmovie.service.WatchlistService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/watchlist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WatchlistResource {

    private final WatchlistService watchlistService;
    private final CollectionSecurity security;
    private final UUID userId;

    @Inject
    public WatchlistResource(WatchlistService watchlistService, CollectionSecurity security, UUID userId) {
        this.watchlistService = watchlistService;
        this.security = security;
        this.userId = userId;
    }

    @GET
    @Path("/collections")
    public Response listCollections(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size) {
        return Response.ok(ApiResponse.success(
                PageResponse.from(watchlistService.listCollections(userId, page, size))
        )).build();
    }

    @POST
    @Path("/collections")
    public Response createCollection(CreateCollectionRequest req) {
        return Response.ok(ApiResponse.success(
                watchlistService.createCollection(userId, req.name(), req.description(), req.isPublic())
        )).build();
    }

    @PUT
    @Path("/collections/{id}")
    public Response updateCollection(@PathParam("id") UUID id, UpdateCollectionRequest req) {
        security.requireOwner(userId, id);
        return Response.ok(ApiResponse.success(
                watchlistService.renameCollection(userId, id, req.name(), req.description(), req.isPublic())
        )).build();
    }

    @DELETE
    @Path("/collections/{id}")
    public Response deleteCollection(@PathParam("id") UUID id) {
        security.requireOwner(userId, id);
        watchlistService.deleteCollection(userId, id);
        return Response.ok(ApiResponse.success("Deleted")).build();
    }

    @GET
    @Path("/collections/{id}/items")
    public Response listItems(@PathParam("id") UUID id,
                              @QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("20") int size) {
        security.requireView(userId, id);
        return Response.ok(ApiResponse.success(
                PageResponse.from(watchlistService.listItems(userId, id, page, size))
        )).build();
    }

    @POST
    @Path("/collections/{id}/items")
    public Response addItem(@PathParam("id") UUID collectionId, UpsertItemRequest req) {
        security.requireEdit(userId, collectionId);
        return Response.ok(ApiResponse.success(
                watchlistService.addItem(userId, collectionId, req.movieId(), req.status(), req.notes())
        )).build();
    }

    @PUT
    @Path("/collections/{id}/items/{movieId}")
    public Response updateItem(@PathParam("id") UUID collectionId,
                               @PathParam("movieId") UUID movieId,
                               UpsertItemRequest req) {
        security.requireEdit(userId, collectionId);
        return Response.ok(ApiResponse.success(
                watchlistService.updateItem(collectionId, movieId, req.status(), req.notes())
        )).build();
    }

    @DELETE
    @Path("/collections/{id}/items/{movieId}")
    public Response deleteItem(@PathParam("id") UUID collectionId, @PathParam("movieId") UUID movieId) {
        security.requireEdit(userId, collectionId);
        watchlistService.removeItem(userId, collectionId, movieId);
        return Response.ok(ApiResponse.success("Deleted")).build();
    }
}


