package exchange;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingEngine {

    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();
    private final Map<Long, Order> knownOrders = new HashMap<>();

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final boolean persistenceEnabled;

    private long nextTradeId = 1;
    public MatchingEngine() {
        this(false);
    }
    public MatchingEngine(boolean persistenceEnabled) {
        this.orderRepository = new OrderRepository();
        this.tradeRepository = new TradeRepository();
        this.persistenceEnabled = persistenceEnabled;
    }

    public List<Trade> submitOrder(Order incomingOrder) {
        if (incomingOrder == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (knownOrders.containsKey(incomingOrder.getOrderId())) {
            throw new IllegalArgumentException("Duplicate order ID");
        }

        Connection connection = null;

        try {
            if (persistenceEnabled) {
                connection = DatabaseConnection.getConnection();
                connection.setAutoCommit(false);
                orderRepository.save(incomingOrder, connection);
            }
            knownOrders.put(incomingOrder.getOrderId(), incomingOrder);

            OrderBook orderBook = orderBooks.computeIfAbsent(
                    incomingOrder.getSymbol(),
                    symbol -> new OrderBook()
            );

            List<Trade> newTrades = new ArrayList<>();

            while (incomingOrder.getRemainingQuantity() > 0) {
                Order restingOrder;
                if (incomingOrder.getSide() == OrderSide.BUY) {
                    restingOrder = orderBook.getBestSellOrder();
                } else {
                    restingOrder = orderBook.getBestBuyOrder();
                }

                if (restingOrder == null) {break;}
                if (!incomingOrder.getSymbol().equals(restingOrder.getSymbol())) {
                    break;
                }

                boolean pricesMatch;

                if (incomingOrder.getSide() == OrderSide.BUY) {
                    pricesMatch = incomingOrder.getPrice().compareTo(restingOrder.getPrice()) >= 0;
                } else {
                    pricesMatch = incomingOrder.getPrice().compareTo(restingOrder.getPrice()) <= 0;
                }

                if (!pricesMatch) {
                    break;
                }


                int executedQuantity = Math.min(incomingOrder.getRemainingQuantity(), restingOrder.getRemainingQuantity());

                BigDecimal executionPrice = restingOrder.getPrice();

                incomingOrder.fill(executedQuantity);
                restingOrder.fill(executedQuantity);

                Order buyOrder;
                Order sellOrder;

                if (incomingOrder.getSide() == OrderSide.BUY) {
                    buyOrder = incomingOrder;
                    sellOrder = restingOrder;

                } else {
                    buyOrder = restingOrder;
                    sellOrder = incomingOrder;
                }

                Instant executedAt = Instant.now();
                Trade trade;

                if (persistenceEnabled) {
                    orderRepository.update(buyOrder, connection);
                    orderRepository.update(sellOrder, connection);
                    long generatedTradeId =
                            tradeRepository.saveAndReturnGeneratedId(buyOrder.getOrderId(), sellOrder.getOrderId(), incomingOrder.getSymbol(), executionPrice, executedQuantity, executedAt, connection);

                    trade = new Trade(generatedTradeId, buyOrder.getOrderId(), sellOrder.getOrderId(), incomingOrder.getSymbol(), executionPrice, executedQuantity, executedAt);
                } else {
                    trade = new Trade(nextTradeId++, buyOrder.getOrderId(), sellOrder.getOrderId(), incomingOrder.getSymbol(), executionPrice, executedQuantity, executedAt);
                }

                trades.add(trade);
                newTrades.add(trade);

                if (restingOrder.isFilled()) {orderBook.removeOrder(restingOrder.getOrderId());}
            }

            if (!incomingOrder.isFilled()) {orderBook.addOrder(incomingOrder);}
            if (persistenceEnabled) {orderRepository.update(incomingOrder, connection);}
            if (persistenceEnabled) {connection.commit();}

            return newTrades;

        } catch (SQLException e) {

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
            }
            throw new IllegalStateException("Database transaction failed", e);

        } finally {
            if (connection != null) {
                try{
                    connection.setAutoCommit(true);
                    connection.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean cancelOrder(String symbol, long orderId) {
        if (symbol == null) {return false;}
        OrderBook book = orderBooks.get(symbol.toUpperCase());
        if (book == null) {return false;}
        Order order = book.getOrder(orderId);
        if (order == null) {return false;}

        book.removeOrder(orderId);
        order.markCancelled();

        if (persistenceEnabled) {
            try {
                Connection connection = DatabaseConnection.getConnection();
                try {
                    connection.setAutoCommit(false);
                    orderRepository.update(order, connection);
                    connection.commit();

                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                    connection.close();
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Could not persist order cancellation", e);
            }
        }
        return true;
    }

    public OrderBook getOrderBook(String symbol) {
        if (symbol == null) {return null;}
        return orderBooks.get(symbol.toUpperCase());}

    public List<Trade> getTrades() {return List.copyOf(trades);}
}

