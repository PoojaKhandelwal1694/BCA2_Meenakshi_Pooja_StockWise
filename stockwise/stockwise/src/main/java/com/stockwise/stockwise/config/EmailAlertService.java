
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
}
