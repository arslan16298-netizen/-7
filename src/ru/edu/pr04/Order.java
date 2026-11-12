package ru.edu.pr04;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** Заказ сотрудника: корень агрегата. Содержит позиции (композиция) и знает свой статус. */
public class Order {

    private final Employee employee;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(Employee employee) {
        // R6: employee не null
        this.employee = Objects.requireNonNull(employee, "employee is null");
    }

    /**
     * Добавляет товар. R8: после confirm изменять нельзя. R7: товар с тем же id не даёт вторую строку,
     * количество существующей позиции увеличивается (диапазон количества проверяет OrderItem).
     */
    public void addProduct(Product product, int quantity) {
        requireDraft();
        if (product == null) {
            throw new IllegalArgumentException("product is null");
        }
        OrderItem existing = findItem(product.getId());
        if (existing != null) {
            existing.increase(quantity);
        } else {
            items.add(new OrderItem(product, quantity));
        }
    }

    /** Удаляет позицию по id товара. Если такой позиции нет – контролируемая ошибка, заказ не меняется. */
    public void removeProduct(String productId) {
        requireDraft();
        Iterator<OrderItem> it = items.iterator();
        while (it.hasNext()) {
            OrderItem item = it.next();
            if (item.getProduct().getId().equals(productId)) {
                it.remove();
                return;
            }
        }
        throw new IllegalArgumentException("product not found in order: " + productId);
    }

    /** R9: итог пересчитывается из текущего состава при каждом вызове, отдельное поле total не хранится. */
    public double total() {
        double sum = 0;
        for (OrderItem item : items) {
            sum += item.getLineTotal();
        }
        return sum;
    }

    /** R10: пустой заказ подтвердить нельзя. R8: после подтверждения статус CONFIRMED, повторный confirm – ошибка. */
    public void confirm() {
        requireDraft();
        if (items.isEmpty()) {
            throw new IllegalStateException("order is empty");
        }
        status = OrderStatus.CONFIRMED;
    }

    /** R11: наружу отдаётся неизменяемая копия, внешний код не может обойти addProduct. */
    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Employee getEmployee() {
        return employee;
    }

    private void requireDraft() {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("order is confirmed");
        }
    }

    private OrderItem findItem(String productId) {
        for (OrderItem item : items) {
            if (item.getProduct().getId().equals(productId)) {
                return item;
            }
        }
        return null;
    }
}
