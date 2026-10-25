
package com.stockwise.stockwise.config;

import com.stockwise.stockwise.auth.User;
import com.stockwise.stockwise.auth.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmailAlertService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public EmailAlertService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            JavaMailSender mailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public void sendLowStockAlerts() {

        String sql = """
            SELECT
                p.product_name AS productName,
                i.quantity AS quantity,
                i.reorder_level AS reorderLevel
            FROM products p
            INNER JOIN inventory i
                ON p.product_id = i.product_id
            WHERE i.quantity <= i.reorder_level
            ORDER BY i.quantity ASC
            """;

        List<Map<String, Object>> products =
                jdbcTemplate.queryForList(sql);

        if (products.isEmpty()) {
            System.out.println("No low-stock products found.");
            return;
        }

        List<User> users = userRepository.findAll();

        for (User user : users) {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(user.getEmail());
            message.setSubject("StockWise - Daily Low Stock Alert");

            StringBuilder body = new StringBuilder();
            body.append("Hello ")
                .append(user.getFullName())
                .append(",\n\nThe following products need restocking:\n\n");

            for (Map<String, Object> product : products) {
                body.append("Product: ")
                    .append(product.get("productName"))
                    .append("\nQuantity: ")
                    .append(product.get("quantity"))
                    .append("\nReorder level: ")
                    .append(product.get("reorderLevel"))
                    .append("\n\n");
            }

            body.append("Please check your StockWise dashboard.\n");

            message.setText(body.toString());
            mailSender.send(message);
        }

        System.out.println("Low-stock emails sent.");
    }

    
public void sendExpiryAlerts() {

    String sql = """
        SELECT
            p.product_name AS productName,
            pb.batch_number AS batchNumber,
            pb.expiry_date AS expiryDate,
            pb.quantity AS quantity
        FROM product_batches pb
        INNER JOIN products p
            ON p.product_id = pb.product_id
        WHERE pb.expiry_date >= CURDATE()
          AND pb.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
          AND pb.quantity > 0
        ORDER BY pb.expiry_date ASC
        """;

    List<Map<String, Object>> batches =
            jdbcTemplate.queryForList(sql);

    if (batches.isEmpty()) {
        System.out.println("No batches expiring in the next 30 days.");
        return;
    }

    List<User> users = userRepository.findAll();

    for (User user : users) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            continue;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("StockWise - Expiry Alert");

        StringBuilder body = new StringBuilder();

        body.append("Hello ")
            .append(user.getFullName())
            .append(",\n\nThe following batches are expiring within 30 days:\n\n");

        for (Map<String, Object> batch : batches) {
            body.append("Product: ")
                .append(batch.get("productName"))
                .append("\nBatch Number: ")
                .append(batch.get("batchNumber"))
                .append("\nExpiry Date: ")
                .append(batch.get("expiryDate"))
                .append("\nQuantity: ")
                .append(batch.get("quantity"))
                .append("\n\n");
        }

        body.append("Please check your StockWise dashboard.\n");

        message.setText(body.toString());
        mailSender.send(message);
    }

    System.out.println("Expiry alert emails sent.");
}

}
