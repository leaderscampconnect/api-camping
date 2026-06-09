package com.esprit.microservice.apicamping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UtilisateurDto {
    private Long id;
    
    @JsonProperty("lastName")
    private String nom;
    
    @JsonProperty("firstName")
    private String prenom;
    
    private String email;
    
    @JsonProperty("phone")
    private String telephone;
}