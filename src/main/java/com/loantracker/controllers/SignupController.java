package com.loantracker.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.mindrot.jbcrypt.BCrypt;

import com.loantracker.utils.DatabaseManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please fill in all fields");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Please enter a valid email address");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters long");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            
            if (checkStmt.executeQuery().getInt(1) > 0) {
                messageLabel.setText("Username or email already exists");
                return;
            }

            // Insert new user
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            String insertQuery = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, email);
            insertStmt.setString(3, hashedPassword);
            
            insertStmt.executeUpdate();
            
            // Show success message and return to login
            messageLabel.setText("Account created successfully!");
            messageLabel.getStyleClass().add("success-message");
            
            // Wait a moment before switching to login screen
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::showLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            messageLabel.setText("Error creating account: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            messageLabel.setText("Error loading login view: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 