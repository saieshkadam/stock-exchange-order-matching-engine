package exchange;

import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        MatchingEngine engine=new MatchingEngine(true);

        Order buyOrder = new Order(
                5007,
                "RELIANCE",
                OrderSide.BUY,
                new BigDecimal("1000.00"),
                10,
                1
        );

        Order sellOrder = new Order(
                5006,
                "RELIANCE",
                OrderSide.SELL,
                new BigDecimal("990.00"),
                6,
                2
        );

        try {
            engine.submitOrder(buyOrder);
            List<Trade> executedTrades = engine.submitOrder(sellOrder);
            System.out.println("Executed trades: "+executedTrades.size());
            for (Trade trade : executedTrades) {
                System.out.println(trade);
            }
            System.out.println(
                    "Buy remaining quantity: "
                            + buyOrder.getRemainingQuantity()
            );
            System.out.println(
                    "Sell status: " + sellOrder.getStatus()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}