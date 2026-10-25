
package com.stockwise.stockwise.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LowStockEmailScheduler {

    private final EmailAlertService emailAlertService;

    public LowStockEmailScheduler(EmailAlertService emailAlertService) {
        this.emailAlertService = emailAlertService;
    }

@Scheduled(cron = "0 0 21 * * *", zone = "Asia/Kolkata")
    public void sendDailyLowStockEmails() {
        emailAlertService.sendLowStockAlerts();
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Kolkata")
public void sendDailyExpiryAlerts() {
    System.out.println("Expiry scheduler triggered: " + java.time.LocalDateTime.now());

    emailAlertService.sendExpiryAlerts();
}
}
