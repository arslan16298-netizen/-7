package ru.edu.pr04;

/** Товар каталога: неизменяемый объект с проверкой инвариантов R1–R3 в конструкторе. */
public class Product {

    private final String id;
    private final String name;
    private final double price;

    /**
     * Fail-fast: все проверки выполняются до присваивания полей,
     * поэтому некорректный Product невозможно создать.
     */
    public Product(String id, String name, double price) {
        // R1: id не null и не blank
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is null or blank");
        }
        // R2: name не null и не blank
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is null or blank");
        }
        // R3: price строго больше 0 (NaN даёт false в сравнении, поэтому тоже отклоняется)
        if (!(price > 0)) {
            throw new IllegalArgumentException("price must be > 0, got " + price);
        }
        if (Double.isInfinite(price)) {
            throw new IllegalArgumentException("price must be finite, got " + price);
        }
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
