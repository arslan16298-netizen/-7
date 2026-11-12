package ru.edu.pr04;

/** Позиция заказа: товар и количество. Диапазон количества задан константами в одном месте (R4, вариант 2). */
public class OrderItem {

    /** Вариант 2, часть А: нижняя граница количества 5 (было 1). */
    public static final int MIN_QUANTITY = 5;
    public static final int MAX_QUANTITY = 100;

    private final Product product;   // final: товар позиции не меняется после создания
    private int quantity;            // меняется только через setQuantity / increase

    public OrderItem(Product product, int quantity) {
        // R5: product не null
        if (product == null) {
            throw new IllegalArgumentException("product is null");
        }
        this.product = product;
        setQuantity(quantity);
    }

    /** R4: MIN_QUANTITY ≤ quantity ≤ MAX_QUANTITY включительно. Единственное место проверки диапазона (DRY). */
    private void setQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw outOfRange(quantity);
        }
        this.quantity = quantity;
    }

    private static IllegalArgumentException outOfRange(long got) {
        return new IllegalArgumentException(
                "quantity must be in [" + MIN_QUANTITY + ", " + MAX_QUANTITY + "], got " + got);
    }

    /** Увеличивает количество на delta; итоговое количество проверяет setQuantity, при ошибке позиция не меняется. */
    public void increase(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be > 0");
        }
        // long исключает переполнение int при очень большом delta
        long sum = (long) this.quantity + delta;
        if (sum > MAX_QUANTITY) {
            throw outOfRange(sum);
        }
        setQuantity((int) sum);
    }

    /** Стоимость позиции: цена × количество. Не хранится, считается при каждом вызове. */
    public double getLineTotal() {
        return product.getPrice() * quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
