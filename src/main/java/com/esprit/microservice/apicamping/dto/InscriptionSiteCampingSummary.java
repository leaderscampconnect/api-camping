package com.esprit.microservice.apicamping.dto;

import com.esprit.microservice.apicamping.entity.StatutDispo;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InscriptionSiteCampingSummary {

    Long idSite;
    String nom;
    String localisation;
    double prixParNuit;
    String imageUrl;
    StatutDispo statutDispo;
}