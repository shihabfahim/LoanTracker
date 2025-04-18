package com.loantracker.controllers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import com.loantracker.models.Loan;
import com.loantracker.utils.DatabaseManager;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddLoanController {
    @FXML private TextField borrowerNameField;
    @FXML private TextField amountField;
    @FXML private TextField interestRateField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label messageLabel;

    private int userId;
    private MainController mainController;

    @FXML
    public void initialize() {
        // Initialize status options
        statusComboBox.getItems().addAll("Active", "Pending", "Completed", "Defaulted");
        statusComboBox.setValue("Active");
        
        // Set today's date as default for start date
        startDatePicker.setValue(LocalDate.now());
        
        // Add listeners for validation
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });
        
        interestRateField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                interestRateField.setText(oldVal);
            }
        });
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Loan loan = new Loan();
            loan.setUserId(userId);
            loan.setBorrowerName(borrowerNameField.getText().trim());
            loan.setAmount(new BigDecimal(amountField.getText().trim()));
            loan.setInterestRate(new BigDecimal(interestRateField.getText().trim()));
            loan.setStartDate(startDatePicker.getValue());
            loan.setEndDate(endDatePicker.getValue());
            loan.setStatus(statusComboBox.getValue());
            loan.setNotes(notesArea.getText().trim());

            saveLoanToDatabase(loan);
            
            // Close the dialog
            ((Stage) borrowerNameField.getScene().getWindow()).close();
            
            // Refresh the main view
            mainController.refreshData();
        } catch (Exception e) {
            messageLabel.setText("Error saving loan: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (borrowerNameField.getText().trim().isEmpty()) {
            messageLabel.setText("Please enter borrower name");
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                messageLabel.setText("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            messageLabel.setText("Please enter a valid amount");
            return false;
        }

        try {
            if (!interestRateField.getText().trim().isEmpty()) {
                BigDecimal rate = new BigDecimal(interestRateField.getText().trim());
                if (rate.compareTo(BigDecimal.ZERO) < 0) {
                    messageLabel.setText("Interest rate cannot be negative");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            messageLabel.setText("Please enter a valid interest rate");
            return false;
        }

        if (startDatePicker.getValue() == null) {
            messageLabel.setText("Please select a start date");
            return false;
        }

        if (endDatePicker.getValue() != null && 
            endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            messageLabel.setText("End date cannot be before start date");
            return false;
        }

        return true;
    }

    private void saveLoanToDatabase(Loan loan) throws Exception {
        String sql = """
            INSERT INTO loans (user_id, borrower_name, amount, interest_rate, 
                             start_date, end_date, status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, loan.getUserId());
            pstmt.setString(2, loan.getBorrowerName());
            pstmt.setBigDecimal(3, loan.getAmount());
            pstmt.setBigDecimal(4, loan.getInterestRate());
            pstmt.setDate(5, Date.valueOf(loan.getStartDate()));
            pstmt.setDate(6, loan.getEndDate() != null ? Date.valueOf(loan.getEndDate()) : null);
            pstmt.setString(7, loan.getStatus());
            pstmt.setString(8, loan.getNotes());
            
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) borrowerNameField.getScene().getWindow()).close();
    }
} 