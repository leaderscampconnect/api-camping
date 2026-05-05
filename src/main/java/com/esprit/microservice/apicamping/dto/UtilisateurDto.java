package com.esprit.microservice.apicamping.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UtilisateurDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
}