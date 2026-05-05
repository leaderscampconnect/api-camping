package com.esprit.microservice.apicamping.repository;

import com.esprit.microservice.apicamping.entity.InscriptionSite;
import com.esprit.microservice.apicamping.entity.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InscriptionSiteRepository extends JpaRepository<InscriptionSite, Long> {

    List<InscriptionSite> findByUtilisateurId(Long utilisateurId);

    List<InscriptionSite> findBySiteCamping_OwnerId(Long ownerId);

    List<InscriptionSite> findBySiteCamping_IdSite(Long idSite);

    @Query("""
           SELECT COALESCE(SUM(i.numberOfGuests), 0)
           FROM InscriptionSite i
           WHERE i.siteCamping.idSite = :siteId
           AND i.statut = :statut
           """)
    Integer sumGuestsBySiteAndStatut(
            @Param("siteId") Long siteId,
            @Param("statut") StatutInscription statut
    );

    @Query("""
           SELECT COALESCE(SUM(i.numberOfGuests), 0)
           FROM InscriptionSite i
           WHERE i.siteCamping.idSite = :siteId
           AND i.statut = :statut
           AND i.dateDebut < :dateFin
           AND i.dateFin > :dateDebut
           """)
    Integer sumGuestsBySiteAndStatutAndDateOverlap(
            @Param("siteId") Long siteId,
            @Param("statut") StatutInscription statut,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("""
           SELECT COUNT(i)
           FROM InscriptionSite i
           WHERE i.siteCamping.idSite = :siteId
           AND i.statut = com.esprit.microservice.apicamping.entity.StatutInscription.CONFIRMED
           """)
    Long countConfirmedBookingsBySiteId(@Param("siteId") Long siteId);
}