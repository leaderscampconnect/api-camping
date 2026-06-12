package com.esprit.microservice.apicamping.service;

import com.esprit.microservice.apicamping.dto.*;
import com.esprit.microservice.apicamping.entity.InscriptionSite;
import com.esprit.microservice.apicamping.entity.SiteCamping;
import com.esprit.microservice.apicamping.entity.StatutDispo;
import com.esprit.microservice.apicamping.entity.StatutInscription;
import com.esprit.microservice.apicamping.client.UtilisateurClient;
import com.esprit.microservice.apicamping.messaging.NotificationPublisher;
import com.esprit.microservice.apicamping.repository.InscriptionSiteRepository;
import com.esprit.microservice.apicamping.repository.SiteCampingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class InscriptionSiteServiceImp implements IInscriptionSiteService {

    private final InscriptionStripeService inscriptionStripeService;
    private final TicketPdfService ticketPdfService;
    private final InscriptionSiteRepository inscriptionSiteRepository;
    private final SiteCampingRepository siteCampingRepository;
    private final UtilisateurClient utilisateurClient;
    private final NotificationPublisher notificationPublisher;
    private final com.esprit.microservice.apicamping.client.NotificationClient notificationClient;

    private InscriptionSiteResponse mapToResponse(InscriptionSite inscription) {
        InscriptionSiteResponse response = new InscriptionSiteResponse();

        response.setIdInscription(inscription.getIdInscription());
        response.setDateDebut(inscription.getDateDebut());
        response.setDateFin(inscription.getDateFin());
        response.setNumberOfGuests(inscription.getNumberOfGuests());
        response.setStatut(inscription.getStatut());

        if (inscription.getSiteCamping() != null) {
            InscriptionSiteCampingSummary siteSummary = new InscriptionSiteCampingSummary();
            siteSummary.setIdSite(inscription.getSiteCamping().getIdSite());
            siteSummary.setNom(inscription.getSiteCamping().getNom());
            siteSummary.setLocalisation(inscription.getSiteCamping().getLocalisation());
            siteSummary.setPrixParNuit(inscription.getSiteCamping().getPrixParNuit());
            siteSummary.setImageUrl(inscription.getSiteCamping().getImageUrl());
            siteSummary.setStatutDispo(inscription.getSiteCamping().getStatutDispo());

            response.setSiteCamping(siteSummary);
        }

        response.setUtilisateurId(inscription.getUtilisateurId());
        response.setUtilisateurEmail(inscription.getUtilisateurEmail());

        return response;
    }

    private void updateSiteStatus(SiteCamping site) {
        Integer reservedGuests = inscriptionSiteRepository
                .sumGuestsBySiteAndStatut(site.getIdSite(), StatutInscription.CONFIRMED);

        if (reservedGuests == null) {
            reservedGuests = 0;
        }

        int remainingCapacity = site.getCapacite() - reservedGuests;

        if (site.getStatutDispo() == StatutDispo.CLOSED) {
            return;
        }

        if (remainingCapacity <= 0) {
            site.setStatutDispo(StatutDispo.FULL);
        } else {
            site.setStatutDispo(StatutDispo.AVAILABLE);
        }

        siteCampingRepository.save(site);
    }

    @Override
    public InscriptionCheckoutResponse addInscriptionSite(InscriptionSiteCreateRequest request) {
        if (request.getDateDebut() == null || request.getDateFin() == null) {
            throw new RuntimeException("dateDebut and dateFin are required");
        }

        if (!request.getDateFin().isAfter(request.getDateDebut())) {
            throw new RuntimeException("dateFin must be after dateDebut");
        }

        if (request.getNumberOfGuests() == null || request.getNumberOfGuests() <= 0) {
            throw new RuntimeException("numberOfGuests must be greater than 0");
        }

        if (request.getUtilisateurId() == null) {
            throw new RuntimeException("utilisateurId is required");
        }

        SiteCamping site = siteCampingRepository.findById(request.getSiteId())
                .orElseThrow(() -> new RuntimeException("Site not found"));

        if (site.getStatutDispo() == StatutDispo.FULL || site.getStatutDispo() == StatutDispo.CLOSED) {
            throw new RuntimeException("This site is not available for booking");
        }

        Integer reservedGuests = inscriptionSiteRepository
                .sumGuestsBySiteAndStatutAndDateOverlap(
                        site.getIdSite(),
                        StatutInscription.CONFIRMED,
                        request.getDateDebut(),
                        request.getDateFin()
                );

        if (reservedGuests == null) {
            reservedGuests = 0;
        }

        int remainingCapacity = site.getCapacite() - reservedGuests;

        if (request.getNumberOfGuests() > remainingCapacity) {
            throw new RuntimeException("numberOfGuests exceeds remaining capacity");
        }

        InscriptionSite inscriptionSite = new InscriptionSite();
        inscriptionSite.setDateDebut(request.getDateDebut());
        inscriptionSite.setDateFin(request.getDateFin());
        inscriptionSite.setNumberOfGuests(request.getNumberOfGuests());
        inscriptionSite.setStatut(StatutInscription.PENDING);
        inscriptionSite.setSiteCamping(site);
        inscriptionSite.setUtilisateurId(request.getUtilisateurId());
        inscriptionSite.setUtilisateurEmail(request.getUtilisateurEmail());

        InscriptionSite saved = inscriptionSiteRepository.save(inscriptionSite);

        var session = inscriptionStripeService.createCheckoutSession(saved);

        updateSiteStatus(site);

        InscriptionCheckoutResponse response = new InscriptionCheckoutResponse();
        response.setInscription(mapToResponse(saved));
        response.setCheckoutUrl(session.getUrl());
        response.setSessionId(session.getId());

        return response;
    }

    @Override
    public InscriptionSiteResponse confirmPayment(Long idInscription) {
        InscriptionSite inscription = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new RuntimeException("Inscription not found"));

        if (inscription.getStatut() == StatutInscription.CONFIRMED) {
            return mapToResponse(inscription);
        }

        inscription.setStatut(StatutInscription.CONFIRMED);
        InscriptionSite updated = inscriptionSiteRepository.save(inscription);

        updateSiteStatus(updated.getSiteCamping());

        try {
            UtilisateurDto user = utilisateurClient.getUserById(updated.getUtilisateurId());
            byte[] ticketPdf = ticketPdfService.generateTicketPdf(updated);
            
            notificationPublisher.publishBookingConfirmed(updated, user, ticketPdf);
            notificationPublisher.publishBookingOwnerAlert(updated, user);

            try {
                // Camper Notification via Feign
                NotificationDto camperNotif = new NotificationDto();
                camperNotif.setRecipientId(user.getId());
                camperNotif.setEventId(updated.getIdInscription().toString());
                camperNotif.setType("BOOKING_CONFIRMED");
                camperNotif.setTitle("Booking Confirmed: " + updated.getSiteCamping().getNom());
                camperNotif.setMessage("Your booking for " + updated.getSiteCamping().getNom() + " from " + updated.getDateDebut() + " to " + updated.getDateFin() + " has been confirmed.");
                notificationClient.createNotification(camperNotif);

                // Owner Notification via Feign
                if (updated.getSiteCamping().getOwnerId() != null) {
                    NotificationDto ownerNotif = new NotificationDto();
                    ownerNotif.setRecipientId(updated.getSiteCamping().getOwnerId().toString());
                    ownerNotif.setEventId(updated.getIdInscription().toString());
                    ownerNotif.setType("BOOKING_RECEIVED");
                    ownerNotif.setTitle("New Booking: " + updated.getSiteCamping().getNom());
                    ownerNotif.setMessage("You have a new booking for " + updated.getSiteCamping().getNom() + " from " + user.getFirstName() + " " + user.getLastName() + ".");
                    notificationClient.createNotification(ownerNotif);
                }
            } catch (Exception feignEx) {
                System.err.println("Failed to create OpenFeign notifications: " + feignEx.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Failed to publish RabbitMQ events or generate PDF: " + e.getMessage());
        }

        return mapToResponse(updated);
    }

    @Override
    public InscriptionSiteResponse patchInscriptionSite(Long idInscription, InscriptionSiteUpdateRequest request) {
        InscriptionSite existing = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new IllegalArgumentException(
                        "InscriptionSite not found with id: " + idInscription));

        if (request.getDateDebut() != null) {
            existing.setDateDebut(request.getDateDebut());
        }

        if (request.getDateFin() != null) {
            existing.setDateFin(request.getDateFin());
        }

        if (existing.getDateDebut() != null && existing.getDateFin() != null) {
            if (!existing.getDateFin().isAfter(existing.getDateDebut())) {
                throw new RuntimeException("dateFin must be after dateDebut");
            }
        }

        if (request.getNumberOfGuests() != null) {
            if (request.getNumberOfGuests() <= 0) {
                throw new RuntimeException("numberOfGuests must be greater than 0");
            }

            existing.setNumberOfGuests(request.getNumberOfGuests());
        }

        InscriptionSite saved = inscriptionSiteRepository.save(existing);
        updateSiteStatus(saved.getSiteCamping());

        return mapToResponse(saved);
    }

    @Override
    public InscriptionSiteResponse getInscriptionSiteById(Long idInscription) {
        InscriptionSite inscription = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new IllegalArgumentException(
                        "InscriptionSite not found with id: " + idInscription));

        return mapToResponse(inscription);
    }

    @Override
    public List<InscriptionSiteResponse> getAllInscriptionSites() {
        return inscriptionSiteRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteInscriptionSite(Long idInscription) {
        InscriptionSite inscription = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new IllegalArgumentException(
                        "InscriptionSite not found with id: " + idInscription));

        SiteCamping site = inscription.getSiteCamping();

        inscriptionSiteRepository.delete(inscription);
        updateSiteStatus(site);
    }

    @Override
    public List<InscriptionSiteResponse> getBySiteCamping(Long idSite) {
        return inscriptionSiteRepository.findBySiteCamping_IdSite(idSite)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public InscriptionSiteResponse cancelInscriptionSite(Long idInscription) {
        InscriptionSite inscription = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new IllegalArgumentException(
                        "InscriptionSite not found with id: " + idInscription));

        if (inscription.getStatut() == StatutInscription.CANCELLED) {
            return mapToResponse(inscription);
        }

        inscription.setStatut(StatutInscription.CANCELLED);
        InscriptionSite saved = inscriptionSiteRepository.save(inscription);

        updateSiteStatus(saved.getSiteCamping());

        return mapToResponse(saved);
    }

    @Override
    public List<InscriptionSiteResponse> getMyInscriptions(Long utilisateurId) {
        return inscriptionSiteRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public byte[] generateTicket(Long idInscription) {
        InscriptionSite inscription = inscriptionSiteRepository.findById(idInscription)
                .orElseThrow(() -> new RuntimeException("Inscription not found"));

        if (inscription.getStatut() != StatutInscription.CONFIRMED) {
            throw new RuntimeException("Ticket can only be generated for confirmed bookings");
        }

        return ticketPdfService.generateTicketPdf(inscription);
    }

    @Override
    public List<InscriptionSiteResponse> getMyCampBookingList(Long ownerId) {
        return inscriptionSiteRepository.findBySiteCamping_OwnerId(ownerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}
