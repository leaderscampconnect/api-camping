package com.esprit.microservice.apicamping.client;

import com.esprit.microservice.apicamping.dto.UtilisateurDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ap-users")
public interface UtilisateurClient {

    @GetMapping("/api/users/getAll")
    List<UtilisateurDto> getAllUsers();

    @GetMapping("/api/users/{id}")
    UtilisateurDto getUserById(@PathVariable Long id);

    @PostMapping("/api/users/add")
    UtilisateurDto addUser(@RequestBody UtilisateurDto utilisateur);
}
