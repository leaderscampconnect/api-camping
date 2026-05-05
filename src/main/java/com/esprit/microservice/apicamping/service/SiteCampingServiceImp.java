package com.esprit.microservice.apicamping.service;

import com.esprit.microservice.apicamping.client.UtilisateurClient;
import com.esprit.microservice.apicamping.common.ICloudinaryService;
import com.esprit.microservice.apicamping.dto.*;
import com.esprit.microservice.apicamping.entity.SiteCamping;
import com.esprit.microservice.apicamping.entity.StatutDispo;
import com.esprit.microservice.apicamping.entity.StatutInscription;
import com.esprit.microservice.apicamping.repository.InscriptionSiteRepository;
import com.esprit.microservice.apicamping.repository.SiteCampingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class SiteCampingServiceImp implements ISiteCampingService {

    private final SiteCampingRepository siteCampingRepository;
    private final ICloudinaryService cloudinaryService;
    private final InscriptionSiteRepository inscriptionSiteRepository;
    private final UtilisateurClient utilisateurClient;

    @Override
    public SiteCampingResponse patchSiteCamping(Long idSite, SiteCampingUpdateRequest updatedData) {
        SiteCamping existing = siteCampingRepository.findById(idSite)
                .orElseThrow(() -> new IllegalArgumentException("SiteCamping not found with id: " + idSite));

        if (updatedData.getNom() != null) existing.setNom(updatedData.getNom());
        if (updatedData.getLocalisation() != null) existing.setLocalisation(updatedData.getLocalisation());
        if (updatedData.getCapacite() != null) existing.setCapacite(updatedData.getCapacite());
        if (updatedData.getPrixParNuit() != null) existing.setPrixParNuit(updatedData.getPrixParNuit());
        if (updatedData.getDescription() != null) existing.setDescription(updatedData.getDescription());
        if (updatedData.getStatutDispo() != null) existing.setStatutDispo(updatedData.getStatutDispo());

        if (updatedData.getImage() != null && !updatedData.getImage().isEmpty()) {
            if (existing.getImagePublicId() != null && !existing.getImagePublicId().isBlank()) {
                cloudinaryService.deleteImage(existing.getImagePublicId());
            }

            Map<String, String> uploadResult = cloudinaryService.uploadImage(updatedData.getImage());
            existing.setImageUrl(uploadResult.get("imageUrl"));
            existing.setImagePublicId(uploadResult.get("imagePublicId"));
        }

        return mapToResponse(siteCampingRepository.save(existing));
    }

    @Override
    public SiteCampingResponse getSiteCampingById(Long idSite) {
        SiteCamping site = siteCampingRepository.findById(idSite)
                .orElseThrow(() -> new IllegalArgumentException("SiteCamping not found with id: " + idSite));

        return mapToResponse(site);
    }

    @Override
    public List<SiteCampingResponse> getAllSiteCampings() {
        return siteCampingRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SiteCampingResponse addSiteCamping(SiteCampingCreateRequest request) {
        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }

        if (request.getImage() == null || request.getImage().isEmpty()) {
            throw new IllegalArgumentException("image is required");
        }

        Map<String, String> uploadResult = cloudinaryService.uploadImage(request.getImage());

        SiteCamping siteCamping = new SiteCamping();
        siteCamping.setNom(request.getNom());
        siteCamping.setLocalisation(request.getLocalisation());
        siteCamping.setCapacite(request.getCapacite());
        siteCamping.setPrixParNuit(request.getPrixParNuit());
        siteCamping.setDescription(request.getDescription());
        siteCamping.setStatutDispo(request.getStatutDispo());

        siteCamping.setImageUrl(uploadResult.get("imageUrl"));
        siteCamping.setImagePublicId(uploadResult.get("imagePublicId"));

        siteCamping.setOwnerId(request.getOwnerId());
        siteCamping.setOwnerEmail(request.getOwnerEmail());

        return mapToResponse(siteCampingRepository.save(siteCamping));
    }

    @Override
    public SiteCampingResponse closeSiteCamping(Long idSite) {
        SiteCamping site = siteCampingRepository.findById(idSite)
                .orElseThrow(() -> new IllegalArgumentException("SiteCamping not found with id: " + idSite));

        if (site.getStatutDispo() == StatutDispo.CLOSED) {
            throw new IllegalArgumentException("Site is already closed");
        }

        site.setStatutDispo(StatutDispo.CLOSED);

        return mapToResponse(siteCampingRepository.save(site));
    }

    @Override
    public List<SiteCampingResponse> getMySites(Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }

        return siteCampingRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SiteAvailabilityResponse getAvailability(Long idSite, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("dateDebut and dateFin are required");
        }

        SiteCamping site = siteCampingRepository.findById(idSite)
                .orElseThrow(() -> new IllegalArgumentException("SiteCamping not found with id: " + idSite));

        if (!dateFin.isAfter(dateDebut)) {
            throw new IllegalArgumentException("dateFin must be after dateDebut");
        }

        Integer reservedGuests = inscriptionSiteRepository
                .sumGuestsBySiteAndStatutAndDateOverlap(
                        idSite,
                        StatutInscription.CONFIRMED,
                        dateDebut,
                        dateFin
                );

        if (reservedGuests == null) {
            reservedGuests = 0;
        }

        SiteAvailabilityResponse response = new SiteAvailabilityResponse();
        response.setSiteId(site.getIdSite());
        response.setCapacite(site.getCapacite());
        response.setReservedGuests(reservedGuests);
        response.setRemainingCapacity(site.getCapacite() - reservedGuests);

        return response;
    }

    private int calculateRemainingCapacity(SiteCamping site) {
        if (site.getCapacite() == null) {
            return 0;
        }

        Integer confirmedGuests = inscriptionSiteRepository
                .sumGuestsBySiteAndStatut(site.getIdSite(), StatutInscription.CONFIRMED);

        if (confirmedGuests == null) {
            confirmedGuests = 0;
        }

        return site.getCapacite() - confirmedGuests;
    }

    private SiteCampingResponse mapToResponse(SiteCamping site) {
        SiteCampingResponse response = new SiteCampingResponse();

        response.setIdSite(site.getIdSite());
        response.setNom(site.getNom());
        response.setLocalisation(site.getLocalisation());
        response.setCapacite(site.getCapacite());
        response.setRemainingCapacity(calculateRemainingCapacity(site));
        response.setPrixParNuit(site.getPrixParNuit());
        response.setDescription(site.getDescription());
        response.setImageUrl(site.getImageUrl());
        response.setImagePublicId(site.getImagePublicId());
        response.setStatutDispo(site.getStatutDispo());
        response.setOwnerId(site.getOwnerId());
        response.setOwnerEmail(site.getOwnerEmail());

        return response;
    }

    public List<UtilisateurDto> getAllUsersFromUsersService() {
        return utilisateurClient.getAllUsers();
    }

    public UtilisateurDto getUserFromUsersService(Long id) {
        return utilisateurClient.getUserById(id);
    }

    public UtilisateurDto createUserFromCamping(UtilisateurDto dto) {
        return utilisateurClient.addUser(dto);
    }
}