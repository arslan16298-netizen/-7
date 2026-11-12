package ru.edu.pr04;

/** Сотрудник, на которого оформляется заказ. record: поля final, setter нет, объект неизменяем. */
public record Employee(String id, String fullName) {

    /** Компактный конструктор record: проверка выполняется до присваивания полей. */
    public Employee {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("employee id is null or blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("employee fullName is null or blank");
        }
    }
}
