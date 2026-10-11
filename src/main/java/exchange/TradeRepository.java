package exchange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TradeRepository {

    public void save(Trade trade) throws SQLException {

        String sql = """
                INSERT INTO trades (
                    trade_id,
                    buy_order_id,
                    sell_order_id,
                    symbol,
                    execution_price,
                    quantity,
                    executed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            save(trade, connection, statement);
        }
    }

    public void save(Trade trade, Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO trades (
                    trade_id,
                    buy_order_id,
                    sell_order_id,
                    symbol,
                    execution_price,
                    quantity,
                    executed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            save(trade, connection, statement);
        }
    }

    private void save(Trade trade, Connection connection, PreparedStatement statement
    ) throws SQLException {
        statement.setLong(1, trade.tradeId());
        statement.setLong(2, trade.buyOrderId());
        statement.setLong(3, trade.sellOrderId());
        statement.setString(4, trade.symbol());
        statement.setBigDecimal(5, trade.executionPrice());
        statement.setInt(6, trade.quantity());
        statement.setTimestamp(
                7,
                java.sql.Timestamp.from(trade.executedAt())
        );
        statement.executeUpdate();
    }

    // Used when MySQL should generate the trade ID.
    public long saveAndReturnGeneratedId(
            long buyOrderId,
            long sellOrderId,
            String symbol,
            java.math.BigDecimal executionPrice,
            int quantity,
            java.time.Instant executedAt,
            Connection connection
    ) throws SQLException {

        String sql = """
                INSERT INTO trades (
                    buy_order_id,
                    sell_order_id,
                    symbol,
                    execution_price,
                    quantity,
                    executed_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             java.sql.Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setLong(1, buyOrderId);
            statement.setLong(2, sellOrderId);
            statement.setString(3, symbol);
            statement.setBigDecimal(4, executionPrice);
            statement.setInt(5, quantity);

            statement.setTimestamp(
                    6,
                    java.sql.Timestamp.from(executedAt)
            );

            statement.executeUpdate();

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Database did not generate a trade ID"
                    );
                }

                return generatedKeys.getLong(1);
            }
        }
    }
}
