package com.esprit.microservice.apicamping.service;



import com.esprit.microservice.apicamping.dto.SiteAvailabilityResponse;
import com.esprit.microservice.apicamping.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ISiteCampingService {

    SiteCampingResponse patchSiteCamping(Long idSite, SiteCampingUpdateRequest updatedData);

    SiteCampingResponse getSiteCampingById(Long idSite);

    List<SiteCampingResponse> getAllSiteCampings();

    SiteCampingResponse addSiteCamping(SiteCampingCreateRequest request);

    SiteCampingResponse closeSiteCamping(Long idSite);

    List<SiteCampingResponse> getMySites(Long ownerId);

    SiteAvailabilityResponse getAvailability(Long idSite, LocalDate dateDebut, LocalDate dateFin);
    List<UtilisateurDto> getAllUsersFromUsersService();

    UtilisateurDto getUserFromUsersService(Long id);

    UtilisateurDto createUserFromCamping(UtilisateurDto dto);
}
