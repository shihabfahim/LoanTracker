package com.loantracker.controllers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;

import com.loantracker.models.Loan;
import com.loantracker.utils.DatabaseManager;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditLoanController {
    @FXML private TextField borrowerNameField;
    @FXML private TextField amountField;
    @FXML private TextField interestRateField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label messageLabel;

    private Loan loan;
    private MainController mainController;

    @FXML
    public void initialize() {
        // Initialize status options
        statusComboBox.getItems().addAll("Active", "Pending", "Completed", "Defaulted");
        
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

    public void setLoan(Loan loan) {
        this.loan = loan;
        populateFields();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void populateFields() {
        borrowerNameField.setText(loan.getBorrowerName());
        amountField.setText(loan.getAmount().toString());
        interestRateField.setText(loan.getInterestRate() != null ? loan.getInterestRate().toString() : "");
        startDatePicker.setValue(loan.getStartDate());
        endDatePicker.setValue(loan.getEndDate());
        statusComboBox.setValue(loan.getStatus());
        notesArea.setText(loan.getNotes());
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            loan.setBorrowerName(borrowerNameField.getText().trim());
            loan.setAmount(new BigDecimal(amountField.getText().trim()));
            loan.setInterestRate(new BigDecimal(interestRateField.getText().trim()));
            loan.setStartDate(startDatePicker.getValue());
            loan.setEndDate(endDatePicker.getValue());
            loan.setStatus(statusComboBox.getValue());
            loan.setNotes(notesArea.getText().trim());

            updateLoanInDatabase();
            
            // Close the dialog
            ((Stage) borrowerNameField.getScene().getWindow()).close();
            
            // Refresh the main view
            mainController.refreshData();
        } catch (Exception e) {
            messageLabel.setText("Error updating loan: " + e.getMessage());
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

    private void updateLoanInDatabase() throws Exception {
        String sql = """
            UPDATE loans 
            SET borrower_name = ?, amount = ?, interest_rate = ?, 
                start_date = ?, end_date = ?, status = ?, notes = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, loan.getBorrowerName());
            pstmt.setBigDecimal(2, loan.getAmount());
            pstmt.setBigDecimal(3, loan.getInterestRate());
            pstmt.setDate(4, Date.valueOf(loan.getStartDate()));
            pstmt.setDate(5, loan.getEndDate() != null ? Date.valueOf(loan.getEndDate()) : null);
            pstmt.setString(6, loan.getStatus());
            pstmt.setString(7, loan.getNotes());
            pstmt.setInt(8, loan.getId());
            
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) borrowerNameField.getScene().getWindow()).close();
    }
} 