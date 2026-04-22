import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Product class
class Product {
    String name;
    double price;
    int stock;

    public Product(String name, double price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}

// CartItem class
class CartItem {
    Product product;
    int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return product.price * quantity;
    }
}

// DatabaseConnection class
class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/perfume_store";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static Connection connection = null;
    
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected!");
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found!");
            } catch (SQLException e) {
                System.err.println("Database connection failed!");
            }
        }
        return connection;
    }
    
    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products ORDER BY name";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return products;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                Product product = new Product(
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                );
                products.add(product);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
    
    public static boolean updateStock(String productName, int newStock) {
        String query = "UPDATE products SET stock = ? WHERE name = ?";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return false;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, newStock);
            pstmt.setString(2, productName);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            return result > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static boolean addProduct(String name, double price, int stock) {
        String query = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return false;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, stock);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            return result > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static boolean deleteProduct(String productName) {
        String query = "DELETE FROM products WHERE name = ?";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return false;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, productName);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            return result > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
public static boolean saveOrder(List<CartItem> cart, double totalAmount) {
    Connection conn = getConnection();
    if (conn == null) return false;
    
    try {
        conn.setAutoCommit(false);
        
        // Insert into orders table
        String orderQuery = "INSERT INTO orders (total_amount) VALUES (?)";
        PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
        orderStmt.setDouble(1, totalAmount);
        orderStmt.executeUpdate();
        
        // Get the generated order ID
        ResultSet generatedKeys = orderStmt.getGeneratedKeys();
        int orderId = -1;
        if (generatedKeys.next()) {
            orderId = generatedKeys.getInt(1);
        }
        orderStmt.close();
        
        // Insert into order_items table
        String itemQuery = "INSERT INTO order_items (order_id, product_name, quantity, price, subtotal) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement itemStmt = conn.prepareStatement(itemQuery);
        
        for (CartItem item : cart) {
            itemStmt.setInt(1, orderId);
            itemStmt.setString(2, item.product.name);
            itemStmt.setInt(3, item.quantity);
            itemStmt.setDouble(4, item.product.price);
            itemStmt.setDouble(5, item.getSubtotal());
            itemStmt.addBatch();
        }
        
        itemStmt.executeBatch();
        itemStmt.close();
        
        conn.commit();
        System.out.println("Order saved successfully! Order ID: " + orderId);
        return true;
        
    } catch (SQLException e) {
        try {
            conn.rollback();
            System.err.println("Order save failed: " + e.getMessage());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
    
    public static List<String> getAllOrders() {
        List<String> orders = new ArrayList<>();
        String query = "SELECT id, order_date, total_amount FROM orders ORDER BY order_date DESC";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return orders;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String order = String.format("Order #%d | %s | Total: ₱%.2f",
                    rs.getInt("id"),
                    rs.getString("order_date"),
                    rs.getDouble("total_amount")
                );
                orders.add(order);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
    
    public static List<String> getOrderItems(int orderId) {
        List<String> items = new ArrayList<>();
        String query = "SELECT product_name, quantity, price, subtotal FROM order_items WHERE order_id = ?";
        
        try {
            Connection conn = getConnection();
            if (conn == null) return items;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String item = String.format("%s x%d - ₱%.2f each (Subtotal: ₱%.2f)",
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getDouble("subtotal")
                );
                items.add(item);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

// Main GUI class - YOUR ORIGINAL GUI UNCHANGED
public class Admin extends JFrame {
    private List<Product> products = new ArrayList<>();
    private List<CartItem> cart = new ArrayList<>();
    private DefaultTableModel cartTableModel;
    private JTable cartTable;
    private JLabel totalLabel;
    private JLabel statusLabel;
    private JPanel productsPanel;
    private Font buttonFont;

    public Admin() {
        setTitle("ScentMark Perfume Store");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(250, 248, 245));

        buttonFont = new Font("Arial", Font.BOLD, 14);

        // Load products from database (instead of hardcoded)
        products = DatabaseConnection.getAllProducts();
        
        // If database is empty, add default products
        if (products.isEmpty()) {
            products.add(new Product("Chanel No. 5", 150.00, 10));
            products.add(new Product("Dior Sauvage", 120.00, 10));
            products.add(new Product("Gucci Bloom", 100.00, 10));
            products.add(new Product("Versace Eros", 90.00, 10));
            products.add(new Product("Yves Saint Laurent Black Opium", 110.00, 10));
            products.add(new Product("Calvin Klein CK One", 70.00, 10));
            products.add(new Product("Tom Ford Oud Wood", 200.00, 10));
            products.add(new Product("Jo Malone English Pear & Freesia", 180.00, 10));
            
            for (Product p : products) {
                DatabaseConnection.addProduct(p.name, p.price, p.stock);
            }
        }

        // Header with gradient-like effect
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, new Color(139, 69, 19), getWidth(), 0, new Color(160, 82, 45)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setLayout(new BorderLayout());
        JLabel headerLabel = new JLabel("Welcome to Our Perfume Collection", JLabel.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Search panel - Stylish search UI
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        searchPanel.setBackground(new Color(139, 69, 19));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel searchLabel = new JLabel("Search Perfumes:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        searchLabel.setForeground(Color.WHITE);
        JTextField searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 150, 100), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        JButton searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setPreferredSize(new Dimension(110, 35));
        searchButton.setBackground(new Color(218, 165, 32));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createRaisedBevelBorder());
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        searchButton.addActionListener(e -> updateProductTable(searchField.getText()));

        // Product panel
        productsPanel = new JPanel();
        productsPanel.setLayout(new GridLayout(0, 3, 10, 10));
        productsPanel.setBackground(new Color(250, 248, 245));
        updateProductTable("");
        JScrollPane productScroll = new JScrollPane(productsPanel);
        productScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(139, 69, 19), 2), "Available Perfumes", 0, 0, new Font("Arial", Font.BOLD, 18), new Color(139, 69, 19)));

        // Cart table - Stylish table
        String[] cartColumns = {"Name", "Price", "Quantity", "Subtotal"};
        cartTableModel = new DefaultTableModel(cartColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartTableModel);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 13));
        cartTable.setRowHeight(28);
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        cartTable.getTableHeader().setBackground(new Color(139, 69, 19));
        cartTable.getTableHeader().setForeground(Color.WHITE);
        cartTable.getTableHeader().setPreferredSize(new Dimension(0, 35));
        cartTable.setSelectionBackground(new Color(255, 182, 193));
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
            "Your Cart",
            0, 0,
            new Font("Arial", Font.BOLD, 16),
            new Color(139, 69, 19)
        ));

        // Buttons
        JButton removeFromCartButton = new JButton("Remove from Cart");
        JButton checkoutButton = new JButton("Checkout");
        JButton clearCartButton = new JButton("Clear Cart");
        JButton adminButton = new JButton("Admin Panel");
        JButton viewOrdersButton = new JButton("View Orders"); // Added new button

        // Style buttons with consistent modern styling
        Font buttonFontLarge = new Font("Arial", Font.BOLD, 13);
        removeFromCartButton.setFont(buttonFontLarge);
        removeFromCartButton.setBackground(new Color(220, 20, 60));
        removeFromCartButton.setForeground(Color.WHITE);
        removeFromCartButton.setFocusPainted(false);
        removeFromCartButton.setBorder(BorderFactory.createRaisedBevelBorder());
        removeFromCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeFromCartButton.setPreferredSize(new Dimension(140, 40));
        
        checkoutButton.setFont(buttonFontLarge);
        checkoutButton.setBackground(new Color(34, 139, 34));
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setFocusPainted(false);
        checkoutButton.setBorder(BorderFactory.createRaisedBevelBorder());
        checkoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkoutButton.setPreferredSize(new Dimension(100, 40));
        
        clearCartButton.setFont(buttonFontLarge);
        clearCartButton.setBackground(new Color(255, 140, 0));
        clearCartButton.setForeground(Color.WHITE);
        clearCartButton.setFocusPainted(false);
        clearCartButton.setBorder(BorderFactory.createRaisedBevelBorder());
        clearCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearCartButton.setPreferredSize(new Dimension(120, 40));
        
        adminButton.setFont(buttonFontLarge);
        adminButton.setBackground(new Color(139, 69, 19));
        adminButton.setForeground(Color.WHITE);
        adminButton.setFocusPainted(false);
        adminButton.setBorder(BorderFactory.createRaisedBevelBorder());
        adminButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        adminButton.setPreferredSize(new Dimension(120, 40));
        
        viewOrdersButton.setFont(buttonFontLarge);
        viewOrdersButton.setBackground(new Color(70, 130, 180));
        viewOrdersButton.setForeground(Color.WHITE);
        viewOrdersButton.setFocusPainted(false);
        viewOrdersButton.setBorder(BorderFactory.createRaisedBevelBorder());
        viewOrdersButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewOrdersButton.setPreferredSize(new Dimension(120, 40));

        // Tooltips
        removeFromCartButton.setToolTipText("Select an item from your cart to remove it");
        clearCartButton.setToolTipText("Remove all items from your cart");
        checkoutButton.setToolTipText("Complete your purchase");
        adminButton.setToolTipText("Open admin panel to manage products");
        viewOrdersButton.setToolTipText("View order history");
        searchButton.setToolTipText("Search for perfumes by name");

        removeFromCartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = cartTable.getSelectedRow();
                if (selectedRow != -1) {
                    CartItem item = cart.get(selectedRow);
                    item.product.stock += item.quantity;
                    DatabaseConnection.updateStock(item.product.name, item.product.stock);
                    cart.remove(selectedRow);
                    updateCartTable();
                    updateTotal();
                    updateProductTable("");
                    JOptionPane.showMessageDialog(Admin.this, item.product.name + " removed from cart.");
                } else {
                    JOptionPane.showMessageDialog(Admin.this, "Please select an item to remove.");
                }
            }
        });

        clearCartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (CartItem item : cart) {
                    item.product.stock += item.quantity;
                    DatabaseConnection.updateStock(item.product.name, item.product.stock);
                }
                cart.clear();
                updateCartTable();
                updateTotal();
                updateProductTable("");
                JOptionPane.showMessageDialog(Admin.this, "Cart cleared.");
            }
        });

        checkoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cart.isEmpty()) {
                    JOptionPane.showMessageDialog(Admin.this, "Your cart is empty. Add some perfumes first!");
                } else {
                    double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
                    if (DatabaseConnection.saveOrder(cart, total)) {
                        JOptionPane.showMessageDialog(Admin.this, "Total: ₱" + String.format("%.2f", total) + "\nThank you for your purchase! Your perfumes will be delivered soon.");
                        cart.clear();
                        updateCartTable();
                        updateTotal();
                        products = DatabaseConnection.getAllProducts();
                        updateProductTable("");
                    } else {
                        JOptionPane.showMessageDialog(Admin.this, "Error saving order. Please try again.");
                    }
                }
            }
        });

        adminButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAdminPanel();
            }
        });
        
        viewOrdersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOrderHistory();
            }
        });

        // Total label - Stylish total display
        totalLabel = new JLabel("Total: ₱0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 22));
        totalLabel.setForeground(new Color(220, 20, 60));
        totalLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 20, 60), 3),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        totalLabel.setBackground(new Color(255, 240, 245));
        totalLabel.setOpaque(true);

        statusLabel = new JLabel("Items in cart: 0");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 13));
        statusLabel.setForeground(new Color(139, 69, 19));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        // Layout - JSplitPane with 70% for products and 30% for cart
        JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, productScroll, cartScroll);
        centerPanel.setDividerLocation(0.7);
        centerPanel.setResizeWeight(0.7);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        centerPanel.setBackground(new Color(250, 248, 245));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(removeFromCartButton);
        buttonPanel.add(clearCartButton);
        buttonPanel.add(checkoutButton);
        buttonPanel.add(adminButton);
        buttonPanel.add(viewOrdersButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(200, 150, 100)),
            BorderFactory.createEmptyBorder(10, 15, 15, 15)
        ));
        bottomPanel.setBackground(new Color(250, 248, 245));
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(totalLabel, BorderLayout.EAST);
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.closeConnection();
        }));
    }

    private void updateTotal() {
        double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        totalLabel.setText("Total: ₱" + String.format("%.2f", total));
        statusLabel.setText("Items in cart: " + cart.stream().mapToInt(item -> item.quantity).sum());
    }

    private void updateProductTable(String filter) {
        productsPanel.removeAll();
        for (Product p : products) {
            if (filter.isEmpty() || p.name.toLowerCase().contains(filter.toLowerCase())) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                card.setBackground(Color.WHITE);
                card.setPreferredSize(new Dimension(320, 280));

                // Image placeholder
                JLabel imageLabel = new JLabel("Perfume Image", JLabel.CENTER);
                imageLabel.setFont(new Font("Arial", Font.BOLD, 14));
                imageLabel.setPreferredSize(new Dimension(300, 150));
                imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                imageLabel.setBackground(new Color(240, 240, 240));
                imageLabel.setOpaque(true);
                card.add(imageLabel, BorderLayout.NORTH);

                // Info
                JPanel infoPanel = new JPanel(new GridLayout(3, 1));
                infoPanel.setBackground(Color.WHITE);
                JLabel nameLabel = new JLabel(p.name, JLabel.CENTER);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
                infoPanel.add(nameLabel);
                JLabel priceLabel = new JLabel("₱" + String.format("%.2f", p.price), JLabel.CENTER);
                priceLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                infoPanel.add(priceLabel);
                JLabel stockLabel = new JLabel("Stock: " + p.stock, JLabel.CENTER);
                stockLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                infoPanel.add(stockLabel);
                card.add(infoPanel, BorderLayout.CENTER);

                // Button
                JButton addBtn = new JButton("Add to Cart");
                addBtn.setFont(buttonFont);
                addBtn.setBackground(new Color(34, 139, 34));
                addBtn.setForeground(Color.WHITE);
                addBtn.setToolTipText("Add this perfume to your cart");
                addBtn.addActionListener(e -> {
                    if (p.stock > 0) {
                        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, p.stock, 1));
                        int result = JOptionPane.showConfirmDialog(Admin.this, quantitySpinner, "Select Quantity", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            int quantity = (Integer) quantitySpinner.getValue();
                            if (quantity > p.stock) {
                                JOptionPane.showMessageDialog(Admin.this, "Not enough stock.");
                                return;
                            }
                            p.stock -= quantity;
                            DatabaseConnection.updateStock(p.name, p.stock);
                            boolean found = false;
                            for (CartItem item : cart) {
                                if (item.product.name.equals(p.name)) {
                                    item.quantity += quantity;
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                cart.add(new CartItem(p, quantity));
                            }
                            updateCartTable();
                            updateTotal();
                            updateProductTable("");
                            JOptionPane.showMessageDialog(Admin.this, quantity + " x " + p.name + " added to cart!\nStock left: " + p.stock);
                        }
                    } else {
                        JOptionPane.showMessageDialog(Admin.this, "Sorry, " + p.name + " is out of stock.");
                    }
                });
                card.add(addBtn, BorderLayout.SOUTH);

                productsPanel.add(card);
            }
        }
        productsPanel.revalidate();
        productsPanel.repaint();
    }

    private void updateCartTable() {
        cartTableModel.setRowCount(0);
        for (CartItem item : cart) {
            cartTableModel.addRow(new Object[]{item.product.name, "₱" + String.format("%.2f", item.product.price), item.quantity, "₱" + String.format("%.2f", item.getSubtotal())});
        }
    }

    private void showOrderHistory() {
        JDialog orderDialog = new JDialog(this, "Order History", true);
        orderDialog.setSize(600, 400);
        orderDialog.setLocationRelativeTo(this);
        orderDialog.setLayout(new BorderLayout());
        
        List<String> orders = DatabaseConnection.getAllOrders();
        
        if (orders.isEmpty()) {
            JLabel emptyLabel = new JLabel("No orders found.", JLabel.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 16));
            orderDialog.add(emptyLabel, BorderLayout.CENTER);
        } else {
            JList<String> orderList = new JList<>(orders.toArray(new String[0]));
            orderList.setFont(new Font("Arial", Font.PLAIN, 14));
            orderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            JTextArea detailsArea = new JTextArea();
            detailsArea.setFont(new Font("Arial", Font.PLAIN, 12));
            detailsArea.setEditable(false);
            
            orderList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selected = orderList.getSelectedValue();
                    if (selected != null) {
                        String[] parts = selected.split("#");
                        if (parts.length > 1) {
                            String[] idParts = parts[1].split(" ");
                            int orderId = Integer.parseInt(idParts[0]);
                            
                            List<String> items = DatabaseConnection.getOrderItems(orderId);
                            detailsArea.setText("");
                            detailsArea.append("Order Items:\n");
                            detailsArea.append("------------\n");
                            for (String item : items) {
                                detailsArea.append(item + "\n");
                            }
                        }
                    }
                }
            });
            
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                new JScrollPane(orderList), new JScrollPane(detailsArea));
            splitPane.setDividerLocation(300);
            
            orderDialog.add(splitPane, BorderLayout.CENTER);
        }
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> orderDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        orderDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        orderDialog.setVisible(true);
    }

    private void openAdminPanel() {
        JDialog adminDialog = new JDialog(this, "Admin Panel - Add Product", true);
        adminDialog.setSize(450, 350);
        adminDialog.setLayout(new GridBagLayout());
        adminDialog.setLocationRelativeTo(this);
        adminDialog.getContentPane().setBackground(new Color(245, 245, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0;
        adminDialog.add(nameLabel, gbc);

        JTextField nameField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        adminDialog.add(nameField, gbc);

        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 1;
        adminDialog.add(priceLabel, gbc);

        JTextField priceField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        adminDialog.add(priceField, gbc);

        JLabel stockLabel = new JLabel("Stock:");
        stockLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2;
        adminDialog.add(stockLabel, gbc);

        JTextField stockField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 2;
        adminDialog.add(stockField, gbc);

        JLabel removeLabel = new JLabel("Remove Product:");
        removeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 3;
        adminDialog.add(removeLabel, gbc);

        JComboBox<String> productCombo = new JComboBox<>();
        for (Product p : products) {
            productCombo.addItem(p.name);
        }
        gbc.gridx = 1; gbc.gridy = 3;
        adminDialog.add(productCombo, gbc);

        JButton addButton = new JButton("Add Product");
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setBackground(new Color(34, 139, 34));
        addButton.setForeground(Color.WHITE);
        gbc.gridx = 1; gbc.gridy = 4;
        adminDialog.add(addButton, gbc);

        JButton removeButton = new JButton("Remove Product");
        removeButton.setFont(new Font("Arial", Font.BOLD, 14));
        removeButton.setBackground(Color.RED);
        removeButton.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 4;
        adminDialog.add(removeButton, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setBackground(Color.RED);
        cancelButton.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 5;
        adminDialog.add(cancelButton, gbc);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = nameField.getText().trim();
                    double price = Double.parseDouble(priceField.getText().trim());
                    int stock = Integer.parseInt(stockField.getText().trim());
                    if (!name.isEmpty() && price > 0 && stock >= 0) {
                        if (DatabaseConnection.addProduct(name, price, stock)) {
                            products = DatabaseConnection.getAllProducts();
                            updateProductTable("");
                            productCombo.addItem(name);
                            JOptionPane.showMessageDialog(adminDialog, "Product added successfully!");
                            nameField.setText("");
                            priceField.setText("");
                            stockField.setText("");
                        } else {
                            JOptionPane.showMessageDialog(adminDialog, "Failed to add product. It might already exist.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(adminDialog, "Please enter valid details.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(adminDialog, "Invalid price or stock. Please enter numbers.");
                }
            }
        });

        cancelButton.addActionListener(e -> adminDialog.dispose());

        removeButton.addActionListener(e -> {
            String selected = (String) productCombo.getSelectedItem();
            if (selected != null) {
                if (DatabaseConnection.deleteProduct(selected)) {
                    products = DatabaseConnection.getAllProducts();
                    updateProductTable("");
                    JOptionPane.showMessageDialog(adminDialog, "Product removed successfully!");
                    productCombo.removeAllItems();
                    for (Product p : products) productCombo.addItem(p.name);
                } else {
                    JOptionPane.showMessageDialog(adminDialog, "Failed to remove product.");
                }
            } else {
                JOptionPane.showMessageDialog(adminDialog, "No product selected.");
            }
        });

        adminDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Admin().setVisible(true));
    }
}