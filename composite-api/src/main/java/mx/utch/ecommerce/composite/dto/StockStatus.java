package mx.utch.ecommerce.composite.dto;

public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    UNKNOWN;

    private static final int LOW_STOCK_THRESHOLD = 5;

    public static StockStatus fromUnits(int units) {
        if (units <= 0) {
            return OUT_OF_STOCK;
        }
        return units <= LOW_STOCK_THRESHOLD ? LOW_STOCK : IN_STOCK;
    }
}
