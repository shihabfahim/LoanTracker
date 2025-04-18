# Loan Tracker Application

A JavaFX-based desktop application for managing personal or small-scale loans with SQLite database backend.

## Features

- Secure user authentication with signup and login functionality
- Efficient loan management (Create, Read, Update, Delete)
- Payment tracking and management
- Basic reporting capabilities
- Modern and user-friendly interface
- Lightweight SQLite database for easy deployment

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Maven 3.6 or later

## Setup Instructions

1. Clone the repository:
```bash
git clone [repository-url]
cd loan-tracker
```

2. Build the project using Maven:
```bash
mvn clean package
```

3. Run the application:
```bash
mvn javafx:run
```

## Database Schema

The application uses SQLite with the following table structure:

### Users Table
- id (INTEGER PRIMARY KEY)
- username (TEXT UNIQUE)
- password (TEXT)
- email (TEXT UNIQUE)
- created_at (TIMESTAMP)

### Loans Table
- id (INTEGER PRIMARY KEY)
- user_id (INTEGER)
- borrower_name (TEXT)
- amount (DECIMAL)
- interest_rate (DECIMAL)
- start_date (DATE)
- end_date (DATE)
- status (TEXT)
- notes (TEXT)
- created_at (TIMESTAMP)

### Payments Table
- id (INTEGER PRIMARY KEY)
- loan_id (INTEGER)
- amount (DECIMAL)
- payment_date (DATE)
- payment_type (TEXT)
- notes (TEXT)
- created_at (TIMESTAMP)

## Security Features

- Password hashing using BCrypt
- Prepared statements to prevent SQL injection
- Input validation and sanitization

## Usage

1. Launch the application
2. Create a new account or login with existing credentials
3. Use the main interface to:
   - Add new loans
   - Track payments
   - Generate reports
   - Manage existing loans
   - View payment history

## Contributing

Feel free to submit issues and enhancement requests.

## License

[MIT License](LICENSE) 