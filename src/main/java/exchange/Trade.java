package exchange;

import java.math.BigDecimal;
import java.time.Instant;

public record Trade(long tradeId, long buyOrderId, long sellOrderId, String symbol, BigDecimal executionPrice, int quantity, Instant executedAt) {
    public Trade {
        if (tradeId<=0||buyOrderId<=0||sellOrderId<=0||buyOrderId==sellOrderId) {
            throw new IllegalArgumentException("Invalid trade IDs");
        }
        if (symbol==null||symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (executionPrice == null || executionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Execution price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Trade quantity must be positive");
        }
        if (executedAt == null) {
            throw new IllegalArgumentException("Execution time is required");
        }
    }
    public BigDecimal getTradeValue() {
        return executionPrice.multiply(BigDecimal.valueOf(quantity));
    }
}