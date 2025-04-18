package com.loantracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.loantracker.utils.DatabaseManager;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database
        DatabaseManager.initializeDatabase();
        
        // Load the login view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loantracker/fxml/login.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Loan Tracker");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 