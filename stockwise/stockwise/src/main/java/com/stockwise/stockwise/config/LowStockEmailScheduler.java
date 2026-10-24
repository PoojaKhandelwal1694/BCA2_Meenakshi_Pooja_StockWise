
package com.stockwise.stockwise.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LowStockEmailScheduler {

    private final EmailAlertService emailAlertService;

    public LowStockEmailScheduler(EmailAlertService emailAlertService) {
        this.emailAlertService = emailAlertService;
    }

@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendDailyLowStockEmails() {
        emailAlertService.sendLowStockAlerts();
    }
}
