package com.esprit.microservice.apicamping.controller;


import com.esprit.microservice.apicamping.dto.*;
import com.esprit.microservice.apicamping.service.ISiteCampingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Gestion Site Camping")
@RestController
@RequestMapping("/site-camping")
@AllArgsConstructor
public class SiteCampingController {
    private final ISiteCampingService iSiteCampingService;


    @GetMapping("/getsite/{idSite}")
    public SiteCampingResponse retrieveSiteCamping(@PathVariable Long idSite) {
        return iSiteCampingService.getSiteCampingById(idSite);
    }

    @PostMapping(value = "/addSite", consumes = "multipart/form-data")
    public SiteCampingResponse addSiteCamping(@ModelAttribute SiteCampingCreateRequest request) {
        return iSiteCampingService.addSiteCamping(request);
    }

    @GetMapping("/getAll")
    public List<SiteCampingResponse> getAllSiteCampings() {
        return iSiteCampingService.getAllSiteCampings();
    }

    @PatchMapping(value = "/updateSite/{idSite}", consumes = "multipart/form-data")
    public SiteCampingResponse patchSiteCamping(
            @PathVariable Long idSite,
            @ModelAttribute SiteCampingUpdateRequest updatedData) {

        return iSiteCampingService.patchSiteCamping(idSite, updatedData);
    }

    @PatchMapping("/close/{idSite}")
    public SiteCampingResponse closeSiteCamping(@PathVariable Long idSite) {
        return iSiteCampingService.closeSiteCamping(idSite);
    }

    @GetMapping("/my-sites")
    public List<SiteCampingResponse> getMySites(@RequestParam Long ownerId) {
        return iSiteCampingService.getMySites(ownerId);
    }

    @GetMapping("/{idSite}/availability")
    public SiteAvailabilityResponse getAvailability(
            @PathVariable Long idSite,
            @RequestParam LocalDate dateDebut,
            @RequestParam LocalDate dateFin) {
        return iSiteCampingService.getAvailability(idSite, dateDebut, dateFin);
    }

    @GetMapping("/users")
    public List<UtilisateurDto> getUsersFromUsersService() {
        return iSiteCampingService.getAllUsersFromUsersService();
    }

    @GetMapping("/users/{id}")
    public UtilisateurDto getUserFromUsersService(@PathVariable Long id) {
        return iSiteCampingService.getUserFromUsersService(id);
    }

    @PostMapping("/users")
    public UtilisateurDto createUserFromUsersService(@RequestBody UtilisateurDto dto) {
        return iSiteCampingService.createUserFromCamping(dto);
    }
}
