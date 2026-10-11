package exchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Order {

    private final long orderId;
    private final String symbol;
    private final OrderSide side;
    private final BigDecimal price;
    private final int originalQuantity;
    private final long sequenceNumber;

    private int remainingQuantity;
    private exchange.OrderStatus status=exchange.OrderStatus.OPEN;

    public Order(long orderId, String symbol, OrderSide side, BigDecimal price, int quantity, long sequenceNumber) {
        if (orderId<=0||sequenceNumber<=0) {
            throw new IllegalArgumentException("Order ID and sequence must be positive");
        }
        if (symbol==null||symbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol cannot be empty");
        }
        if (side==null||price==null) {
            throw new IllegalArgumentException("Side and price are required");
        }
        if (price.scale()>2) {
            throw new IllegalArgumentException("Price must have at most two decimal places");
        }
        if (price.compareTo(BigDecimal.ZERO)<=0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (quantity<=0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.orderId=orderId;
        this.symbol=symbol.trim().toUpperCase();
        this.side=side;
        this.price=price.setScale(2, RoundingMode.UNNECESSARY);
        this.originalQuantity=quantity;
        this.remainingQuantity=quantity;
        this.sequenceNumber=sequenceNumber;
    }

    public long getOrderId() {return orderId;}
    public String getSymbol() {return symbol;}
    public OrderSide getSide() {return side;}
    public BigDecimal getPrice() {return price;}
    public int getOriginalQuantity() {return originalQuantity;};
    public int getRemainingQuantity() {return remainingQuantity;}
    public long getSequenceNumber() {return sequenceNumber;}
    public boolean isFilled() {return remainingQuantity==0;}
    public OrderStatus getStatus() {return status;}

    void markCancelled() {status = OrderStatus.CANCELLED;}

    public void fill(int quantity) {
        if (quantity<= 0||quantity>remainingQuantity) {
            throw new IllegalArgumentException("Invalid fill quantity");
        }

        remainingQuantity-=quantity;

        if (remainingQuantity==0) {
            status = exchange.OrderStatus.FILLED;
        } else {
            status = exchange.OrderStatus.PARTIALLY_FILLED;
        }
    }
    @Override
    public String toString() {
        return "Order{"+"id="+orderId+",symbol='"+symbol+'\''+",side="+side+",price="+price+",remaining="+remainingQuantity+'}';
    }
}