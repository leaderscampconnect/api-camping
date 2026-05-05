package com.esprit.microservice.apicamping.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteCamping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idSite;

    String nom;
    String localisation;
    Integer capacite;
    double prixParNuit;
    String imageUrl;
    String imagePublicId;
    String description;

    @Enumerated(EnumType.STRING)
    StatutDispo statutDispo;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "siteCamping")
    Set<InscriptionSite> inscriptions = new HashSet<>();

    @Column(name = "owner_id")
    Long ownerId;

    @Column(name = "owner_email")
    String ownerEmail;
}