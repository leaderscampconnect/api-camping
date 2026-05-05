package com.esprit.microservice.apicamping.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InscriptionCheckoutResponse {
    InscriptionSiteResponse inscription;
    String checkoutUrl;
    String sessionId;
}