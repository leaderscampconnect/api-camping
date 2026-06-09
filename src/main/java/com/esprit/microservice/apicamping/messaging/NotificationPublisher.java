package com.esprit.microservice.apicamping.messaging;

import com.esprit.microservice.apicamping.dto.UtilisateurDto;
import com.esprit.microservice.apicamping.entity.InscriptionSite;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBookingConfirmed(InscriptionSite inscription, UtilisateurDto user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "booking.confirmed");
        payload.put("bookingId", String.valueOf(inscription.getIdInscription()));
        payload.put("userId", String.valueOf(inscription.getUtilisateurId()));
        payload.put("recipientEmail", inscription.getUtilisateurEmail());
        
        String name = "Camper";
        if (user != null && user.getPrenom() != null && user.getNom() != null) {
            name = user.getPrenom() + " " + user.getNom();
        }
        payload.put("recipientName", name);
        
        payload.put("campingName", inscription.getSiteCamping().getNom());
        payload.put("checkInDate", inscription.getDateDebut().toString());
        payload.put("checkOutDate", inscription.getDateFin().toString());
        
        double totalAmount = 0.0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(inscription.getDateDebut(), inscription.getDateFin());
        if (days == 0) days = 1;
        totalAmount = inscription.getSiteCamping().getPrixParNuit() * days;
        
        payload.put("totalAmount", totalAmount);
        payload.put("currency", "USD");
        payload.put("bookingReference", "BK-" + inscription.getIdInscription());
        payload.put("timestamp", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend("camping.events", "booking.confirmed", payload);
        System.out.println("Published booking.confirmed event to RabbitMQ for bookingId: " + inscription.getIdInscription());
    }
}
