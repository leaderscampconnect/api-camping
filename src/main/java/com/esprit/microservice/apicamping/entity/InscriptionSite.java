package com.esprit.microservice.apicamping.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;


import jakarta.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "siteCamping")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InscriptionSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idInscription;

    LocalDate dateDebut;
    LocalDate dateFin;
    Integer numberOfGuests;

    @Enumerated(EnumType.STRING)
    StatutInscription statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @JsonIgnore
    SiteCamping siteCamping;

    Long utilisateurId;
    String utilisateurEmail;
}