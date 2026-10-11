package exchange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OrderRepository {

    public void save(Order order) throws SQLException {
        String sql = """
                INSERT INTO orders (
                    order_id,
                    symbol,
                    side,
                    price,
                    original_quantity,
                    remaining_quantity,
                    status,
                    sequence_number
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (
                Connection connection=DatabaseConnection.getConnection();
                PreparedStatement statement=connection.prepareStatement(sql)
        ) {
            save(order,connection,statement);
        }
    }

    public void save(Order order, Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO orders (
                    order_id,
                    symbol,
                    side,
                    price,
                    original_quantity,
                    remaining_quantity,
                    status,
                    sequence_number
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            save(order, connection, statement);
        }
    }

    private void save(Order order, Connection connection, PreparedStatement statement
    ) throws SQLException {

        statement.setLong(1, order.getOrderId());
        statement.setString(2, order.getSymbol());
        statement.setString(3, order.getSide().name());
        statement.setBigDecimal(4, order.getPrice());
        statement.setInt(5, order.getOriginalQuantity());
        statement.setInt(6, order.getRemainingQuantity());
        statement.setString(7, order.getStatus().name());
        statement.setLong(8, order.getSequenceNumber());

        statement.executeUpdate();
    }

    public void update(Order order) throws SQLException {
        String sql = """
                UPDATE orders
                SET remaining_quantity = ?,
                    status = ?
                WHERE order_id = ?
                """;
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            update(order, connection, statement);
        }
    }

    public void update(Order order, Connection connection
    ) throws SQLException {

        String sql = """
                UPDATE orders
                SET remaining_quantity = ?,
                    status = ?
                WHERE order_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            update(order, connection, statement);
        }
    }

    private void update(Order order, Connection connection, PreparedStatement statement
    ) throws SQLException {

        statement.setInt(1, order.getRemainingQuantity());
        statement.setString(2, order.getStatus().name());
        statement.setLong(3, order.getOrderId());

        int rowsUpdated = statement.executeUpdate();

        if (rowsUpdated != 1) {
            throw new SQLException("Expected to update one order, but updated " + rowsUpdated);
        }
    }
}

