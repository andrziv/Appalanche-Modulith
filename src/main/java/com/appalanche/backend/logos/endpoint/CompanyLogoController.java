package com.appalanche.backend.logos.endpoint;

import com.appalanche.backend.logos.business.CompanyLogoService;
import com.appalanche.backend.logos.persistence.CompanyLogo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.parseMediaType;

@RequestMapping("/logo")
@RestController
public class CompanyLogoController {
    private final CompanyLogoService logoService;

    public CompanyLogoController(CompanyLogoService logoService) {
        this.logoService = logoService;
    }

    @GetMapping("/{brand}/{tld}")
    public ResponseEntity<byte[]> getLogo(@PathVariable String brand, @PathVariable String tld) {
        CompanyLogo logo = logoService.getCompanyLogo(brand, tld);

        return ResponseEntity.ok()
                             .contentType(parseMediaType(logo.getContentType()))
                             .cacheControl(maxAge(30, DAYS))
                             .body(logo.getImageBytes());
    }

    @GetMapping("/name/{brand}")
    public ResponseEntity<byte[]> getLogo(@PathVariable String brand) {
        CompanyLogo logo = logoService.getCompanyLogo(brand);

        return ResponseEntity.ok()
                             .contentType(parseMediaType(logo.getContentType()))
                             .cacheControl(maxAge(30, DAYS))
                             .body(logo.getImageBytes());
    }
}
