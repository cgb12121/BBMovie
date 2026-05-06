package com.bbmovie.camundaengine.service;

import com.bbmovie.camundaengine.dto.UniversityObject;
import com.bbmovie.camundaengine.entity.University;
import com.bbmovie.camundaengine.repository.UniversityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UniversityRegistryServiceTest {

    @Test
    void findByDomainExtractsDomainFromEmail() {
        UniversityRepository repository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService service = new UniversityRegistryService(repository, entityManager);

        University university = University.builder().name("Test University").build();
        when(repository.findByDomainsContainingIgnoreCase("example.edu")).thenReturn(Optional.of(university));

        Optional<University> result = service.findByDomain("student@example.edu");

        assertTrue(result.isPresent());
        assertEquals("Test University", result.get().getName());
        verify(repository).findByDomainsContainingIgnoreCase("example.edu");
    }

    @Test
    void bestMatchByNameDelegatesToRepository() {
        UniversityRepository repository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService service = new UniversityRegistryService(repository, entityManager);

        University university = University.builder().name("Harvard University").build();
        when(repository.findByNameContainingIgnoreCase("Harvard")).thenReturn(Optional.of(university));

        Optional<University> result = service.bestMatchByName("Harvard");

        assertTrue(result.isPresent());
        assertEquals("Harvard University", result.get().getName());
    }

    @Test
    @SuppressWarnings("null")
    void initSkipsLoadingWhenRepositoryAlreadyHasData() {
        UniversityRepository repository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService service = new UniversityRegistryService(repository, entityManager);
        when(repository.count()).thenReturn(10L);

        service.init();

        verify(repository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void initHandlesMissingJsonGracefully() {
        UniversityRepository repository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService service = new UniversityRegistryService(repository, entityManager);
        when(repository.count()).thenReturn(0L);

        assertDoesNotThrow(service::init);
    }

    @Test
    void privateHelpersConvertAndExtractDomainAsExpected() {
        UniversityRepository repository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService service = new UniversityRegistryService(repository, entityManager);

        String extracted = ReflectionTestUtils.invokeMethod(service, "extractDomain", "Student@Example.EDU");
        assertEquals("example.edu", extracted);
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractDomain", "no-at-symbol"));

        UniversityObject source = new UniversityObject();
        source.setName("Demo Uni");
        source.setDomains(List.of("demo.edu", "example.edu"));
        source.setWeb_pages(List.of("https://demo.edu"));
        source.setCountry("VN");
        source.setAlpha_two_code("VN");
        source.setState_province("HN");

        University converted = ReflectionTestUtils.invokeMethod(service, "convert", source);
        assertEquals("Demo Uni", converted != null ? converted.getName() : null);
        assertEquals("demo.edu,example.edu", converted != null ? converted.getDomains() : null);
        assertEquals("https://demo.edu", converted != null ? converted.getWebPages() : null);
        assertEquals("VN", converted != null ? converted.getCountry() : null);
        assertEquals("VN", converted != null ? converted.getAlphaTwoCode() : null);
        assertEquals("HN", converted != null ? converted.getStateProvince() : null);
    }
}
