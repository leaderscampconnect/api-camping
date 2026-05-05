package com.esprit.microservice.apicamping.repository;

import com.esprit.microservice.apicamping.entity.SiteCamping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SiteCampingRepository extends JpaRepository<SiteCamping, Long> {

    List<SiteCamping> findByOwnerId(Long ownerId);

    @Query("""
           SELECT s
           FROM SiteCamping s
           WHERE s.statutDispo <> com.esprit.microservice.apicamping.entity.StatutDispo.CLOSED
           ORDER BY s.idSite DESC
           """)
    List<SiteCamping> findAllVisibleSites();
}