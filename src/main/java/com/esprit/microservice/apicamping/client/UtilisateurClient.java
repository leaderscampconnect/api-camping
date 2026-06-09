package com.esprit.microservice.apicamping.client;

import com.esprit.microservice.apicamping.dto.UtilisateurDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service")
public interface UtilisateurClient {

    @GetMapping("/api/users")
    List<UtilisateurDto> getAllUsers();

    @GetMapping("/api/users/{id}")
    UtilisateurDto getUserById(@PathVariable("id") Long id);
}
