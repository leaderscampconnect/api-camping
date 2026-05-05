package com.esprit.microservice.apicamping.service;



import com.esprit.microservice.apicamping.dto.*;

import java.util.List;

public interface IInscriptionSiteService {

    InscriptionCheckoutResponse addInscriptionSite(InscriptionSiteCreateRequest request);
    InscriptionSiteResponse confirmPayment(Long idInscription);

    InscriptionSiteResponse patchInscriptionSite(Long idInscription, InscriptionSiteUpdateRequest request);

    InscriptionSiteResponse getInscriptionSiteById(Long idInscription);

    List<InscriptionSiteResponse> getAllInscriptionSites();

    void deleteInscriptionSite(Long idInscription);

    List<InscriptionSiteResponse> getBySiteCamping(Long idSite);


    InscriptionSiteResponse cancelInscriptionSite(Long idInscription);

    List<InscriptionSiteResponse> getMyInscriptions(Long utilisateurId);

    byte[] generateTicket(Long idInscription);

    List<InscriptionSiteResponse> getMyCampBookingList(Long ownerId);


}
