package com.loantracker.controllers;

import com.loantracker.models.Loan;
import com.loantracker.models.Payment;
import com.loantracker.utils.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class AddPaymentController {
    @FXML private ComboBox<Loan> loanComboBox;
    @FXML private TextField amountField;
    @FXML private DatePicker paymentDatePicker;
    @FXML private ComboBox<String> paymentTypeComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label messageLabel;

    private MainController mainController;
    private List<Loan> loans;

    @FXML
    public void initialize() {
        // Initialize payment type options
        paymentTypeComboBox.getItems().addAll("Principal", "Interest", "Principal + Interest");
        paymentTypeComboBox.setValue("Principal + Interest");
        
        // Set today's date as default
        paymentDatePicker.setValue(LocalDate.now());
        
        // Add listener for numeric validation
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });

        // Configure loan combo box
        loanComboBox.setConverter(new StringConverter<Loan>() {
            @Override
            public String toString(Loan loan) {
                if (loan == null) return "";
                return String.format("%s - $%s", loan.getBorrowerName(), loan.getAmount());
            }

            @Override
            public Loan fromString(String string) {
                return null; // Not needed for this use case
            }
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans;
        loanComboBox.getItems().addAll(loans);
        if (!loans.isEmpty()) {
            loanComboBox.setValue(loans.get(0));
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Payment payment = new Payment();
            payment.setLoanId(loanComboBox.getValue().getId());
            payment.setAmount(new BigDecimal(amountField.getText().trim()));
            payment.setPaymentDate(paymentDatePicker.getValue());
            payment.setPaymentType(paymentTypeComboBox.getValue());
            payment.setNotes(notesArea.getText().trim());

            savePaymentToDatabase(payment);
            
            // Close the dialog
            ((Stage) amountField.getScene().getWindow()).close();
            
            // Refresh the main view
            mainController.refreshData();
        } catch (Exception e) {
            messageLabel.setText("Error saving payment: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (loanComboBox.getValue() == null) {
            messageLabel.setText("Please select a loan");
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

        if (paymentDatePicker.getValue() == null) {
            messageLabel.setText("Please select a payment date");
            return false;
        }

        if (paymentTypeComboBox.getValue() == null) {
            messageLabel.setText("Please select a payment type");
            return false;
        }

        return true;
    }

    private void savePaymentToDatabase(Payment payment) throws Exception {
        String sql = """
            INSERT INTO payments (loan_id, amount, payment_date, payment_type, notes)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, payment.getLoanId());
            pstmt.setBigDecimal(2, payment.getAmount());
            pstmt.setDate(3, Date.valueOf(payment.getPaymentDate()));
            pstmt.setString(4, payment.getPaymentType());
            pstmt.setString(5, payment.getNotes());
            
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) amountField.getScene().getWindow()).close();
    }
} 