import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Main {
    private static final String DB_URL = "jdbc:sqlite:bank_system.db";

    // Main frame for the application
    public static void main(String[] args) {
        // Create the SQLite database and tables if they do not exist
        createDatabase();

        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame("Bank System");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setSize(400, 300);

            JButton adminButton = new JButton("Admin");
            JButton atmButton = new JButton("ATM");

            adminButton.addActionListener(e -> new AdminPanel());
            atmButton.addActionListener(e -> new ATMPanel());

            JPanel panel = new JPanel();
            panel.add(adminButton);
            panel.add(atmButton);
            mainFrame.add(panel);
            mainFrame.setVisible(true);
        });
    }

    // Create the database and users table
    private static void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                         "username TEXT PRIMARY KEY," +
                         "balance REAL NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Admin panel for managing users
    // Admin panel for managing users
    static class AdminPanel {
        private JFrame frame;
        private JTextField usernameField;
        private JTextField initialBalanceField;
        private JTextArea userDetailsArea;

        public AdminPanel() {
            frame = new JFrame("Admin Panel");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            // Create components
            usernameField = new JTextField(15);
            initialBalanceField = new JTextField(15);
            userDetailsArea = new JTextArea(10, 30);
            userDetailsArea.setEditable(false);

            // Create buttons
            JButton addButton = new JButton("Add User");
            JButton showUsersButton = new JButton("Show Users");

            // Add action listeners to buttons
            addButton.addActionListener(e -> addUser());
            showUsersButton.addActionListener(e -> showUsers());

            // Create a panel for user input
            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new GridLayout(3, 2)); // 3 rows, 2 columns
            inputPanel.add(new JLabel("Username:"));
            inputPanel.add(usernameField);
            inputPanel.add(new JLabel("Initial Balance:"));
            inputPanel.add(initialBalanceField);
            inputPanel.add(addButton);
            inputPanel.add(showUsersButton);

            // Create a scroll pane for the user details area
            JScrollPane scrollPane = new JScrollPane(userDetailsArea);
            scrollPane.setPreferredSize(new Dimension(380, 150)); // Set a preferred size for the scroll pane

            // Add components to the frame
            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setVisible(true);
        }

        // Method to add a user
        private void addUser() {
            String username = usernameField.getText();
            double initialBalance;

            try {
                initialBalance = Double.parseDouble(initialBalanceField.getText());
                if (initialBalance < 0) throw new NumberFormatException();
                String sql = "INSERT INTO users (username, balance) VALUES (?, ?)";

                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setDouble(2, initialBalance);
                    pstmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(frame, "User added successfully!");
                usernameField.setText("");
                initialBalanceField.setText("");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid balance.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "Error adding user: " + e.getMessage());
            }
        }

        // Method to show users
        private void showUsers() {
            StringBuilder userList = new StringBuilder("Users:\n");
            String sql = "SELECT username, balance FROM users";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    userList.append("Username: ")
                            .append(rs.getString("username"))
                            .append(", Balance: ")
                            .append(rs.getDouble("balance"))
                            .append("\n");
                }
            } catch (SQLException e) {
                userList.append("Error fetching users: ").append(e.getMessage());
            }
            userDetailsArea.setText(userList.toString());
        }
    }


    // ATM panel for user transactions
    static class ATMPanel {
        private JFrame frame;
        private JTextField usernameField;
        private JTextField amountField;

        public ATMPanel() {
            frame = new JFrame("ATM Panel");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            usernameField = new JTextField(15);
            amountField = new JTextField(15);

            JButton depositButton = new JButton("Deposit");
            JButton withdrawButton = new JButton("Withdraw");
            JButton balanceButton = new JButton("Check Balance");

            depositButton.addActionListener(e -> performTransaction(true));
            withdrawButton.addActionListener(e -> performTransaction(false));
            balanceButton.addActionListener(e -> checkBalance());

            JPanel panel = new JPanel();
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Amount:"));
            panel.add(amountField);
            panel.add(depositButton);
            panel.add(withdrawButton);
            panel.add(balanceButton);
            frame.add(panel);
            frame.setVisible(true);
        }

        private void performTransaction(boolean isDeposit) {
            String username = usernameField.getText();
            double amount;

            try {
                amount = Double.parseDouble(amountField.getText());
                if (amount < 0) throw new NumberFormatException();

                String sql = isDeposit ?
                        "UPDATE users SET balance = balance + ? WHERE username = ?" :
                        "UPDATE users SET balance = balance - ? WHERE username = ?";

                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setString(2, username);
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected > 0) {
                        String message = isDeposit ? "Deposited: " + amount : "Withdrawn: " + amount;
                        JOptionPane.showMessageDialog(frame, message);
                    } else {
                        JOptionPane.showMessageDialog(frame, "User not found!");
                    }
                }
                amountField.setText("");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid amount.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "Error during transaction: " + e.getMessage());
            }
        }

        private void checkBalance() {
            String username = usernameField.getText();
            String sql = "SELECT balance FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(frame, "Balance: " + rs.getDouble("balance"));
                    } else {
                        JOptionPane.showMessageDialog(frame, "User not found!");
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "Error checking balance: " + e.getMessage());
            }
        }
    }
}
