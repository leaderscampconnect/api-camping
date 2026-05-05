package com.esprit.microservice.apicamping.dto;

import com.esprit.microservice.apicamping.entity.StatutDispo;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteCampingUpdateRequest {

    String nom;
    String localisation;
    Integer capacite;
    Double prixParNuit;
    String description;
    StatutDispo statutDispo;
    MultipartFile image;
}
