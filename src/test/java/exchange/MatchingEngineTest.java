package exchange;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {
    @Test
    void compatibleOrdersShouldExecuteTrade() {
        MatchingEngine engine = new MatchingEngine();

        Order buy = new Order(
                1, "RELIANCE", OrderSide.BUY,
                new BigDecimal("1000.00"), 10, 1
        );

        Order sell = new Order(
                2, "RELIANCE", OrderSide.SELL,
                new BigDecimal("990.00"), 6, 2
        );

        engine.submitOrder(buy);
        List<Trade> trades = engine.submitOrder(sell);

        assertEquals(1, trades.size());
        assertEquals(6, trades.get(0).quantity());
        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(
                        trades.get(0).executionPrice()
                )
        );

        assertEquals(4, buy.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, buy.getStatus());
        assertEquals(OrderStatus.FILLED, sell.getStatus());
    }

    @Test
    void incompatibleOrdersShouldNotExecuteTrade() {
        MatchingEngine engine = new MatchingEngine();

        Order buy = new Order(
                1, "RELIANCE", OrderSide.BUY,
                new BigDecimal("950.00"), 5, 1
        );

        Order sell = new Order(
                2, "RELIANCE", OrderSide.SELL,
                new BigDecimal("990.00"), 5, 2
        );

        engine.submitOrder(buy);
        List<Trade> trades = engine.submitOrder(sell);

        assertTrue(trades.isEmpty());
        assertEquals(5, buy.getRemainingQuantity());
        assertEquals(5, sell.getRemainingQuantity());
    }

    @Test
    void differentSymbolsShouldNeverMatch() {
        MatchingEngine engine = new MatchingEngine();

        Order tcsSell = new Order(
                1, "TCS", OrderSide.SELL,
                new BigDecimal("3500.00"), 5, 1
        );

        Order relianceBuy = new Order(
                2, "RELIANCE", OrderSide.BUY,
                new BigDecimal("4000.00"), 5, 2
        );

        engine.submitOrder(tcsSell);
        List<Trade> trades = engine.submitOrder(relianceBuy);

        assertTrue(trades.isEmpty());
        assertEquals(5, tcsSell.getRemainingQuantity());
        assertEquals(5, relianceBuy.getRemainingQuantity());
    }

    @Test
    void duplicateOrderIdsShouldBeRejected() {
        MatchingEngine engine = new MatchingEngine();

        Order first = new Order(
                1, "RELIANCE", OrderSide.BUY,
                new BigDecimal("1000.00"), 5, 1
        );

        Order duplicate = new Order(
                1, "TCS", OrderSide.BUY,
                new BigDecimal("3500.00"), 5, 2
        );

        engine.submitOrder(first);
        assertThrows(IllegalArgumentException.class, () -> engine.submitOrder(duplicate));
    }

    @Test
    void multiplePriceLevelsShouldMatchBestPriceFirst() {

        MatchingEngine engine = new MatchingEngine();
        Order sell990 = new Order(
                10,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("990.00"),
                5,
                1
        );

        Order sell995 = new Order(
                11,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("995.00"),
                5,
                2
        );

        Order sell1000 = new Order(
                12,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("1000.00"),
                5,
                3
        );

        Order buy = new Order(
                13,
                "RELIANCE",
                OrderSide.BUY,
                new BigDecimal("1000.00"),
                12,
                4
        );

        engine.submitOrder(sell990);
        engine.submitOrder(sell995);
        engine.submitOrder(sell1000);

        List<Trade> trades = engine.submitOrder(buy);

        assertEquals(3, trades.size());
        assertEquals(5, trades.get(0).quantity());
        assertEquals(
                0,
                new BigDecimal("990.00")
                        .compareTo(trades.get(0).executionPrice())
        );

        assertEquals(5, trades.get(1).quantity());
        assertEquals(
                0,
                new BigDecimal("995.00")
                        .compareTo(trades.get(1).executionPrice())
        );

        assertEquals(2, trades.get(2).quantity());
        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(trades.get(2).executionPrice())
        );

        assertEquals(0, buy.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, buy.getStatus());

        assertEquals(OrderStatus.FILLED, sell990.getStatus());
        assertEquals(OrderStatus.FILLED, sell995.getStatus());

        assertEquals(3, sell1000.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sell1000.getStatus());
    }

    @Test
    void samePriceOrdersShouldFollowTimePriority() {
        MatchingEngine engine = new MatchingEngine();
        Order firstSell = new Order(
                20,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("1000.00"),
                5,
                1
        );

        Order secondSell = new Order(
                21,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("1000.00"),
                5,
                2
        );

        Order buy = new Order(
                22,
                "RELIANCE",
                OrderSide.BUY,
                new BigDecimal("1000.00"),
                7,
                3
        );

        engine.submitOrder(firstSell);
        engine.submitOrder(secondSell);

        List<Trade> trades = engine.submitOrder(buy);

        assertEquals(2, trades.size());


        assertEquals(20, trades.get(0).sellOrderId());
        assertEquals(5, trades.get(0).quantity());


        assertEquals(21, trades.get(1).sellOrderId());
        assertEquals(2, trades.get(1).quantity());
        assertEquals(OrderStatus.FILLED, firstSell.getStatus());
        assertEquals(3, secondSell.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, secondSell.getStatus());
        assertEquals(0, buy.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, buy.getStatus());
    }
    @Test
    void cancelledOrderShouldNotMatch() {
        MatchingEngine engine = new MatchingEngine();
        Order buy = new Order(
                30,
                "RELIANCE",
                OrderSide.BUY,
                new BigDecimal("1000.00"),
                10,
                1
        );

        engine.submitOrder(buy);
        boolean cancelled =
                engine.cancelOrder("RELIANCE", 30);
        assertTrue(cancelled);
        assertEquals(
                OrderStatus.CANCELLED,
                buy.getStatus()
        );
        assertEquals(10, buy.getRemainingQuantity());
        Order sell = new Order(
                31,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("990.00"),
                5,
                2
        );
        List<Trade> trades = engine.submitOrder(sell);
        assertTrue(trades.isEmpty());
        assertEquals(5, sell.getRemainingQuantity());
        assertEquals(OrderStatus.OPEN, sell.getStatus());
    }
}
