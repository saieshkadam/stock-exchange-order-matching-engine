package exchange;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class OrderBook {
    private final NavigableMap<BigDecimal, ArrayDeque<Order>> buyOrders =
            new TreeMap<>(Comparator.reverseOrder());

    private final NavigableMap<BigDecimal, ArrayDeque<Order>> sellOrders =
            new TreeMap<>();

    private final Map<Long, Order> activeOrders = new HashMap<>();

    public void addOrder(Order order) {
        if (order==null) {throw new IllegalArgumentException("Order cannot be null");}
        if (order.isFilled()) {throw new IllegalArgumentException("Cannot add a fully filled order");}
        if (activeOrders.containsKey(order.getOrderId())) {throw new IllegalArgumentException("Duplicate active order ID");}

        NavigableMap<BigDecimal, ArrayDeque<Order>> side=getSide(order.getSide());
        side.computeIfAbsent(
                order.getPrice(),
                price -> new ArrayDeque<>()
        ).addLast(order);
        activeOrders.put(order.getOrderId(), order);
    }

    public Order getBestBuyOrder() {
        return getBestOrder(buyOrders);
    }
    public Order getBestSellOrder() {
        return getBestOrder(sellOrders);
    }
    private Order getBestOrder(NavigableMap<BigDecimal, ArrayDeque<Order>> side) {
        if (side.isEmpty()) {return null;}
        return side.firstEntry().getValue().peekFirst();
    }

    public void removeOrder(long orderId) {
        Order order=activeOrders.get(orderId);
        if (order==null) {return;}

        NavigableMap<BigDecimal, ArrayDeque<Order>> side = getSide(order.getSide());
        ArrayDeque<Order> ordersAtPrice = side.get(order.getPrice());
        ordersAtPrice.remove(order);

        if (ordersAtPrice.isEmpty()) {side.remove(order.getPrice());}
        activeOrders.remove(orderId);
    }
    public Order getOrder(long orderId) {
        return activeOrders.get(orderId);
    }
    public List<Order> getOpenOrders() {
        return new ArrayList<>(activeOrders.values());
    }
    private NavigableMap<BigDecimal, ArrayDeque<Order>> getSide(OrderSide side) {
        return side==OrderSide.BUY ? buyOrders : sellOrders;
    }
}
