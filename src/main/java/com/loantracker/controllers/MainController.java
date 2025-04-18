package com.loantracker.controllers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

import com.loantracker.models.Loan;
import com.loantracker.models.Payment;
import com.loantracker.models.User;
import com.loantracker.utils.DatabaseManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {
    private User currentUser;
    
    @FXML private Label welcomeLabel;
    
    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, Integer> idColumn;
    @FXML private TableColumn<Loan, String> borrowerColumn;
    @FXML private TableColumn<Loan, BigDecimal> amountColumn;
    @FXML private TableColumn<Loan, BigDecimal> interestRateColumn;
    @FXML private TableColumn<Loan, LocalDate> startDateColumn;
    @FXML private TableColumn<Loan, LocalDate> endDateColumn;
    @FXML private TableColumn<Loan, String> statusColumn;
    
    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, Integer> paymentIdColumn;
    @FXML private TableColumn<Payment, Integer> loanIdColumn;
    @FXML private TableColumn<Payment, BigDecimal> paymentAmountColumn;
    @FXML private TableColumn<Payment, LocalDate> paymentDateColumn;
    @FXML private TableColumn<Payment, String> paymentTypeColumn;
    @FXML private TableColumn<Payment, String> notesColumn;

    private ObservableList<Loan> loans = FXCollections.observableArrayList();
    private ObservableList<Payment> payments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        borrowerColumn.setCellValueFactory(new PropertyValueFactory<>("borrowerName"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        interestRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("loanId"));
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        paymentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        
        // Set table data
        loansTable.setItems(loans);
        paymentsTable.setItems(payments);
        
        // Add context menu for loans
        ContextMenu loansContextMenu = new ContextMenu();
        MenuItem editLoan = new MenuItem("Edit");
        MenuItem deleteLoan = new MenuItem("Delete");
        
        editLoan.setOnAction(e -> editSelectedLoan());
        deleteLoan.setOnAction(e -> deleteSelectedLoan());
        
        loansContextMenu.getItems().addAll(editLoan, deleteLoan);
        loansTable.setContextMenu(loansContextMenu);
    }

    public void initData(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        refreshData();
    }

    public void refreshData() {
        loadLoans();
        loadPayments();
    }

    private void loadLoans() {
        loans.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            String query = "SELECT * FROM loans WHERE user_id = ? ORDER BY created_at DESC";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, currentUser.getId());
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Loan loan = new Loan();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBorrowerName(rs.getString("borrower_name"));
                loan.setAmount(rs.getBigDecimal("amount"));
                loan.setInterestRate(rs.getBigDecimal("interest_rate"));
                loan.setStartDate(rs.getDate("start_date").toLocalDate());
                if (rs.getDate("end_date") != null) {
                    loan.setEndDate(rs.getDate("end_date").toLocalDate());
                }
                loan.setStatus(rs.getString("status"));
                loan.setNotes(rs.getString("notes"));
                loans.add(loan);
            }
        } catch (Exception e) {
            showError("Error loading loans: " + e.getMessage());
        }
    }

    private void loadPayments() {
        payments.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            String query = """
                SELECT p.* FROM payments p
                JOIN loans l ON p.loan_id = l.id
                WHERE l.user_id = ?
                ORDER BY p.payment_date DESC
            """;
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, currentUser.getId());
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("id"));
                payment.setLoanId(rs.getInt("loan_id"));
                payment.setAmount(rs.getBigDecimal("amount"));
                payment.setPaymentDate(rs.getDate("payment_date").toLocalDate());
                payment.setPaymentType(rs.getString("payment_type"));
                payment.setNotes(rs.getString("notes"));
                payments.add(payment);
            }
        } catch (Exception e) {
            showError("Error loading payments: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showError("Error during logout: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerateReport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/generate_report_dialog.fxml"));
            Parent root = loader.load();
            
            GenerateReportController controller = loader.getController();
            controller.setMainController(this);
            controller.setCurrentUser(currentUser);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Generate Report");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Error showing generate report dialog: " + e.getMessage());
        }
    }

    @FXML
    private void showAddLoanDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/add_loan_dialog.fxml"));
            Parent root = loader.load();
            
            AddLoanController controller = loader.getController();
            controller.setUserId(currentUser.getId());
            controller.setMainController(this);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Loan");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Error showing add loan dialog: " + e.getMessage());
        }
    }

    @FXML
    private void showAddPaymentDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/add_payment_dialog.fxml"));
            Parent root = loader.load();
            
            AddPaymentController controller = loader.getController();
            controller.setMainController(this);
            controller.setLoans(loans);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Record Payment");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Error showing add payment dialog: " + e.getMessage());
        }
    }

    private void editSelectedLoan() {
        Loan selectedLoan = loansTable.getSelectionModel().getSelectedItem();
        if (selectedLoan == null) {
            showError("Please select a loan to edit");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/edit_loan_dialog.fxml"));
            Parent root = loader.load();
            
            EditLoanController controller = loader.getController();
            controller.setLoan(selectedLoan);
            controller.setMainController(this);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Loan");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Error showing edit loan dialog: " + e.getMessage());
        }
    }

    private void deleteSelectedLoan() {
        Loan selectedLoan = loansTable.getSelectionModel().getSelectedItem();
        if (selectedLoan == null) {
            showError("Please select a loan to delete");
            return;
        }

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this loan? All associated payments will also be deleted.",
                ButtonType.YES, ButtonType.NO).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try (Connection conn = DatabaseManager.getConnection()) {
                // First delete associated payments
                String deletePayments = "DELETE FROM payments WHERE loan_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(deletePayments);
                pstmt.setInt(1, selectedLoan.getId());
                pstmt.executeUpdate();

                // Then delete the loan
                String deleteLoan = "DELETE FROM loans WHERE id = ? AND user_id = ?";
                pstmt = conn.prepareStatement(deleteLoan);
                pstmt.setInt(1, selectedLoan.getId());
                pstmt.setInt(2, currentUser.getId());
                pstmt.executeUpdate();

                refreshData();
                showInfo("Loan deleted successfully");
            } catch (Exception e) {
                showError("Error deleting loan: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).show();
    }
} 