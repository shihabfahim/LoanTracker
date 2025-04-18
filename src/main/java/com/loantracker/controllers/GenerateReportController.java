package com.loantracker.controllers;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.loantracker.models.Loan;
import com.loantracker.models.Payment;
import com.loantracker.models.User;
import com.loantracker.utils.DatabaseManager;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class GenerateReportController {
    private User currentUser;
    private MainController mainController;
    
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label messageLabel;
    
    @FXML
    public void initialize() {
        reportTypeComboBox.getItems().addAll(
            "All Loans Summary",
            "Payment History"
        );
        reportTypeComboBox.getSelectionModel().selectFirst();
        
        // Set default date range to current month
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now);
    }
    
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    @FXML
    private void handleGenerate() {
        if (!validateInput()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName("loan_report_" + LocalDate.now() + ".txt");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
        );

        File file = fileChooser.showSaveDialog(messageLabel.getScene().getWindow());
        if (file != null) {
            try {
                generateReport(file);
                messageLabel.setText("Report generated successfully!");
            } catch (Exception e) {
                messageLabel.setText("Error generating report: " + e.getMessage());
            }
        }
    }
    
    private void generateReport(File file) throws Exception {
        List<Loan> loans = fetchLoans();
        List<Payment> payments = fetchPayments();
        
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("LOAN TRACKER REPORT");
            writer.println("==================");
            writer.println("Report Type: " + reportTypeComboBox.getValue());
            writer.println("Date Range: " + startDatePicker.getValue() + " to " + endDatePicker.getValue());
            writer.println("Generated on: " + LocalDate.now());
            writer.println();
            
            if (reportTypeComboBox.getValue().equals("All Loans Summary")) {
                writeLoansSummary(writer, loans);
            } else {
                writePaymentHistory(writer, payments);
            }
        }
    }
    
    private void writeLoansSummary(PrintWriter writer, List<Loan> loans) {
        writer.println("LOANS SUMMARY");
        writer.println("============");
        writer.println();
        
        for (Loan loan : loans) {
            writer.println("Loan ID: " + loan.getId());
            writer.println("Borrower: " + loan.getBorrowerName());
            writer.printf("Amount: $%.2f%n", loan.getAmount());
            writer.printf("Interest Rate: %.2f%%%n", loan.getInterestRate());
            writer.println("Start Date: " + loan.getStartDate());
            writer.println("Status: " + loan.getStatus());
            writer.println("--------------------");
        }
    }
    
    private void writePaymentHistory(PrintWriter writer, List<Payment> payments) {
        writer.println("PAYMENT HISTORY");
        writer.println("===============");
        writer.println();
        
        for (Payment payment : payments) {
            writer.println("Payment ID: " + payment.getId());
            writer.println("Loan ID: " + payment.getLoanId());
            writer.printf("Amount: $%.2f%n", payment.getAmount());
            writer.println("Date: " + payment.getPaymentDate());
            writer.println("Type: " + payment.getPaymentType());
            writer.println("--------------------");
        }
    }
    
    private List<Loan> fetchLoans() throws Exception {
        List<Loan> loans = new ArrayList<>();
        String query = "SELECT * FROM loans WHERE user_id = ? AND start_date BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, currentUser.getId());
            pstmt.setDate(2, java.sql.Date.valueOf(startDatePicker.getValue()));
            pstmt.setDate(3, java.sql.Date.valueOf(endDatePicker.getValue()));
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Loan loan = new Loan();
                loan.setId(rs.getInt("id"));
                loan.setBorrowerName(rs.getString("borrower_name"));
                loan.setAmount(rs.getBigDecimal("amount"));
                loan.setInterestRate(rs.getBigDecimal("interest_rate"));
                loan.setStartDate(rs.getDate("start_date").toLocalDate());
                loan.setStatus(rs.getString("status"));
                loans.add(loan);
            }
        }
        return loans;
    }
    
    private List<Payment> fetchPayments() throws Exception {
        List<Payment> payments = new ArrayList<>();
        String query = "SELECT p.* FROM payments p " +
                      "JOIN loans l ON p.loan_id = l.id " +
                      "WHERE l.user_id = ? AND p.payment_date BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, currentUser.getId());
            pstmt.setDate(2, java.sql.Date.valueOf(startDatePicker.getValue()));
            pstmt.setDate(3, java.sql.Date.valueOf(endDatePicker.getValue()));
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("id"));
                payment.setLoanId(rs.getInt("loan_id"));
                payment.setAmount(rs.getBigDecimal("amount"));
                payment.setPaymentDate(rs.getDate("payment_date").toLocalDate());
                payment.setPaymentType(rs.getString("payment_type"));
                payments.add(payment);
            }
        }
        return payments;
    }
    
    private boolean validateInput() {
        if (reportTypeComboBox.getValue() == null) {
            messageLabel.setText("Please select a report type");
            return false;
        }
        
        if (startDatePicker.getValue() == null) {
            messageLabel.setText("Please select a start date");
            return false;
        }
        
        if (endDatePicker.getValue() == null) {
            messageLabel.setText("Please select an end date");
            return false;
        }
        
        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            messageLabel.setText("End date cannot be before start date");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void handleCancel() {
        ((Stage) messageLabel.getScene().getWindow()).close();
    }
} 