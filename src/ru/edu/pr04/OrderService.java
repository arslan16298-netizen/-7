package ru.edu.pr04;

import java.util.List;

/** Сервисный слой без полей (stateless) и без фреймворков: создание заказа и анализ заказа. */
public class OrderService {

    /** Создаёт пустой заказ в статусе DRAFT для сотрудника. */
    public Order createOrder(Employee employee) {
        return new Order(employee);
    }

    /**
     * Вариант 2, часть Б: true, если у всех позиций заказа количество не меньше MIN_QUANTITY (5).
     * Для пустого заказа результат true (нет позиций, нарушающих правило).
     */
    public boolean addMinQuantityCheck(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order is null");
        }
        List<OrderItem> items = order.getItems();
        for (OrderItem item : items) {
            if (item.getQuantity() < OrderItem.MIN_QUANTITY) {
                return false;
            }
        }
        return true;
    }
}
