package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.exception.AccessDeniedException;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyMediaStreamServiceEntitlementTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private StreamingAccessControlService accessControlService;
    @Mock
    private EntitlementClient entitlementClient;

    @InjectMocks
    private ProxyMediaStreamService service;

    @Test
    void should_deny_when_entitlement_client_denies() {
        ReflectionTestUtils.setField(service, "hlsBucket", "hls");
        ReflectionTestUtils.setField(service, "secureBucket", "secure");
        UUID movieId = UUID.randomUUID();

        when(entitlementClient.resolveTierOrDeny(eq("user-1"), eq(movieId), eq("STREAM")))
                .thenThrow(new AccessDeniedException("Entitlement denied"));

        assertThrows(AccessDeniedException.class, () -> service.getFilteredMasterPlaylist(movieId, "user-1"));
        assertThrows(AccessDeniedException.class, () -> service.getHlsFile(movieId, "720p", "user-1"));
    }
}
