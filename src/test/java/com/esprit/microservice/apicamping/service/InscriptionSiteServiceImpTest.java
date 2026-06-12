package com.esprit.microservice.apicamping.service;

import com.esprit.microservice.apicamping.client.NotificationClient;
import com.esprit.microservice.apicamping.dto.*;
import com.esprit.microservice.apicamping.entity.InscriptionSite;
import com.esprit.microservice.apicamping.entity.SiteCamping;
import com.esprit.microservice.apicamping.entity.StatutDispo;
import com.esprit.microservice.apicamping.entity.StatutInscription;
import com.esprit.microservice.apicamping.messaging.NotificationPublisher;
import com.esprit.microservice.apicamping.repository.InscriptionSiteRepository;
import com.esprit.microservice.apicamping.repository.SiteCampingRepository;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscriptionSiteServiceImpTest {

    @Mock
    private InscriptionSiteRepository inscriptionSiteRepository;

    @Mock
    private SiteCampingRepository siteCampingRepository;

    @Mock
    private InscriptionStripeService inscriptionStripeService;

    @Mock
    private TicketPdfService ticketPdfService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private NotificationClient notificationClient;

    private InscriptionSiteServiceImp inscriptionSiteService;

    @BeforeEach
    void setUp() {
        inscriptionSiteService = new InscriptionSiteServiceImp(
                inscriptionStripeService,
                ticketPdfService,
                inscriptionSiteRepository,
                siteCampingRepository,
                notificationPublisher,
                notificationClient
        );
        
        lenient().when(inscriptionSiteRepository.save(any(InscriptionSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(siteCampingRepository.save(any(SiteCamping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addInscriptionSite_Success() throws Exception {
        SiteCamping site = createMockSite(10, StatutDispo.AVAILABLE);
        when(siteCampingRepository.findById(1L)).thenReturn(Optional.of(site));
        
        // 2 reserved guests already, so 8 remaining
        when(inscriptionSiteRepository.sumGuestsBySiteAndStatutAndDateOverlap(
                eq(1L), eq(StatutInscription.CONFIRMED), any(), any()))
                .thenReturn(2);
        
        // Mock Stripe Session
        Session mockSession = mock(Session.class);
        lenient().when(mockSession.getUrl()).thenReturn("http://stripe.com/checkout");
        lenient().when(mockSession.getId()).thenReturn("sess_123");
        when(inscriptionStripeService.createCheckoutSession(any(InscriptionSite.class)))
                .thenReturn(mockSession);

        InscriptionSiteCreateRequest request = createMockRequest(1L, 4);
        InscriptionCheckoutResponse response = inscriptionSiteService.addInscriptionSite(request);

        assertNotNull(response);
        assertEquals("http://stripe.com/checkout", response.getCheckoutUrl());
        assertEquals("sess_123", response.getSessionId());
        assertEquals(StatutInscription.PENDING, response.getInscription().getStatut());
        assertEquals(4, response.getInscription().getNumberOfGuests());
        
        verify(inscriptionSiteRepository).save(any(InscriptionSite.class));
        verify(siteCampingRepository).save(any(SiteCamping.class));
    }

    @Test
    void addInscriptionSite_Fails_WhenCapacityExceeded() {
        SiteCamping site = createMockSite(10, StatutDispo.AVAILABLE);
        when(siteCampingRepository.findById(1L)).thenReturn(Optional.of(site));
        
        // 8 reserved guests, only 2 remaining
        when(inscriptionSiteRepository.sumGuestsBySiteAndStatutAndDateOverlap(
                eq(1L), eq(StatutInscription.CONFIRMED), any(), any()))
                .thenReturn(8);

        // Requesting 4 guests when only 2 remain
        InscriptionSiteCreateRequest request = createMockRequest(1L, 4);

        RuntimeException exception = assertThrows(
                RuntimeException.class, 
                () -> inscriptionSiteService.addInscriptionSite(request)
        );
        assertEquals("numberOfGuests exceeds remaining capacity", exception.getMessage());
    }

    @Test
    void addInscriptionSite_Fails_WhenSiteFullOrClosed() {
        SiteCamping site = createMockSite(10, StatutDispo.CLOSED);
        when(siteCampingRepository.findById(1L)).thenReturn(Optional.of(site));

        InscriptionSiteCreateRequest request = createMockRequest(1L, 2);

        RuntimeException exception = assertThrows(
                RuntimeException.class, 
                () -> inscriptionSiteService.addInscriptionSite(request)
        );
        assertEquals("This site is not available for booking", exception.getMessage());
    }

    @Test
    void confirmPayment_Success() {
        InscriptionSite inscription = createMockInscription(StatutInscription.PENDING);
        when(inscriptionSiteRepository.findById(100L)).thenReturn(Optional.of(inscription));
        when(inscriptionSiteRepository.sumGuestsBySiteAndStatut(eq(1L), eq(StatutInscription.CONFIRMED)))
                .thenReturn(2);
        
        byte[] pdfBytes = "mock-pdf-content".getBytes();
        when(ticketPdfService.generateTicketPdf(any(InscriptionSite.class))).thenReturn(pdfBytes);

        // Mock SecurityContext and JWT for Keycloak data extraction
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        
        when(jwt.getSubject()).thenReturn("keycloak-camper-123");
        when(jwt.getClaimAsString("given_name")).thenReturn("John");
        when(jwt.getClaimAsString("family_name")).thenReturn("Doe");
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        InscriptionSiteResponse response = inscriptionSiteService.confirmPayment(100L);

        assertEquals(StatutInscription.CONFIRMED, response.getStatut());
        
        verify(inscriptionSiteRepository).save(inscription);
        verify(notificationPublisher).publishBookingConfirmed(eq(inscription), any(UtilisateurDto.class), eq(pdfBytes));
        verify(notificationPublisher).publishBookingOwnerAlert(eq(inscription), any(UtilisateurDto.class));
        
        // OpenFeign calls: 1 for camper, 1 for owner
        verify(notificationClient, times(2)).createNotification(any(NotificationDto.class));

        // Clear SecurityContext to avoid polluting other tests
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelInscriptionSite_Success() {
        InscriptionSite inscription = createMockInscription(StatutInscription.CONFIRMED);
        when(inscriptionSiteRepository.findById(100L)).thenReturn(Optional.of(inscription));
        
        when(inscriptionSiteRepository.sumGuestsBySiteAndStatut(eq(1L), eq(StatutInscription.CONFIRMED)))
                .thenReturn(0);

        InscriptionSiteResponse response = inscriptionSiteService.cancelInscriptionSite(100L);

        assertEquals(StatutInscription.CANCELLED, response.getStatut());
        verify(inscriptionSiteRepository).save(inscription);
        verify(siteCampingRepository).save(any(SiteCamping.class));
    }

    @Test
    void patchInscriptionSite_UpdatesDatesAndGuests() {
        InscriptionSite inscription = createMockInscription(StatutInscription.PENDING);
        when(inscriptionSiteRepository.findById(100L)).thenReturn(Optional.of(inscription));
        
        when(inscriptionSiteRepository.sumGuestsBySiteAndStatut(eq(1L), eq(StatutInscription.CONFIRMED)))
                .thenReturn(0);

        InscriptionSiteUpdateRequest request = new InscriptionSiteUpdateRequest();
        request.setNumberOfGuests(8);
        request.setDateDebut(LocalDate.now().plusDays(5));
        request.setDateFin(LocalDate.now().plusDays(8));
        
        InscriptionSiteResponse response = inscriptionSiteService.patchInscriptionSite(100L, request);

        assertEquals(8, response.getNumberOfGuests());
        assertEquals(LocalDate.now().plusDays(5), response.getDateDebut());
        verify(inscriptionSiteRepository).save(inscription);
    }

    // --- Helper Methods to Prevent Duplication ---

    private SiteCamping createMockSite(int capacity, StatutDispo statutDispo) {
        SiteCamping site = new SiteCamping();
        site.setIdSite(1L);
        site.setNom("Mock Camping Site");
        site.setCapacite(capacity);
        site.setStatutDispo(statutDispo);
        site.setPrixParNuit(50.0);
        site.setOwnerId(500L);
        return site;
    }

    private InscriptionSite createMockInscription(StatutInscription statut) {
        InscriptionSite inscription = new InscriptionSite();
        inscription.setIdInscription(100L);
        inscription.setSiteCamping(createMockSite(10, StatutDispo.AVAILABLE));
        inscription.setStatut(statut);
        inscription.setNumberOfGuests(4);
        inscription.setDateDebut(LocalDate.now().plusDays(1));
        inscription.setDateFin(LocalDate.now().plusDays(3));
        inscription.setUtilisateurId(99L);
        inscription.setUtilisateurEmail("camper@test.com");
        return inscription;
    }

    private InscriptionSiteCreateRequest createMockRequest(Long siteId, int guests) {
        InscriptionSiteCreateRequest request = new InscriptionSiteCreateRequest();
        request.setSiteId(siteId);
        request.setNumberOfGuests(guests);
        request.setDateDebut(LocalDate.now().plusDays(1));
        request.setDateFin(LocalDate.now().plusDays(3));
        request.setUtilisateurId(99L);
        request.setUtilisateurEmail("camper@test.com");
        return request;
    }
}
