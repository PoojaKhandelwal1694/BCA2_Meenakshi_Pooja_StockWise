
package com.stockwise.stockwise.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-email")
public class ExpiryEmailTestController {

    private final EmailAlertService emailAlertService;

    public ExpiryEmailTestController(EmailAlertService emailAlertService) {
        this.emailAlertService = emailAlertService;
    }

    @PostMapping("/expiry")
    public ResponseEntity<String> testExpiryEmail() {
        emailAlertService.sendExpiryAlerts();
        return ResponseEntity.ok("Expiry email test triggered. Check server console and inbox.");
    }
}
