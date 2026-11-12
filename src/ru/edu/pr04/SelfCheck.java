package ru.edu.pr04;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Единственная точка входа (main) и тестовый раннер.
 * Без аргументов: unit-проверки + data/sample.csv; csv (или --csv): только data/sample.csv;
 * interactive (или --interactive): команды из System.in.
 */
public class SelfCheck {

    private static final String CSV_PATH = "data/sample.csv";
    private static final double EPS = 1e-9;
    private static final OrderService SERVICE = new OrderService();
    private static final Employee EMP = new Employee("E-01", "Ахметгалиев Арслан");

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "all";
        if (mode.equals("interactive") || mode.equals("--interactive")) {
            interactive();
            return;
        }
        if (!mode.equals("csv") && !mode.equals("--csv")) {
            unitChecks();
        }
        dataDriven();
        System.out.println("Итого: PASS " + passed + ", FAIL " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ================= Режим 1: unit-проверки =================

    private static void unitChecks() {
        baseChecks();          // 18 обязательных проверок (1–18)
        extraChecks();         // дополнительные граничные проверки
        variantPartA();        // вариант 2, часть А: количество [5, 100]
        variantPartB();        // вариант 2, часть Б: addMinQuantityCheck
        System.out.println("Unit-проверок выполнено: " + (passed + failed));
    }

    private static Product product(String id, double price) {
        return new Product(id, "Товар " + id, price);
    }

    private static void baseChecks() {
        // --- Product (1–6) ---
        try {
            Product p = new Product("M01", "Monitor", 32990);
            check("1 Product: корректные параметры", p.getId().equals("M01") && p.getPrice() == 32990, "поля не совпали");
        } catch (RuntimeException e) {
            check("1 Product: корректные параметры", false, e.toString());
        }
        expectProductError("2 Product: id=null", null, "Monitor", 100, "id is null or blank");
        expectProductError("3 Product: id=\"\"", "", "Monitor", 100, "id is null or blank");
        expectProductError("4 Product: name=null", "M01", null, 100, "name is null or blank");
        expectProductError("5 Product: price=0", "M01", "Monitor", 0, "price must be > 0");
        expectProductError("6 Product: price=-100", "M01", "Monitor", -100, "price must be > 0");

        // --- OrderItem (7–12), диапазон количества по варианту 2: [5, 100] ---
        Product p1 = product("P1", 1000);
        expectItemOk("7 OrderItem: корректные product, quantity=5", p1, 5);
        expectItemError("8 OrderItem: quantity=0", p1, 0, "quantity must be in [5, 100]");
        expectItemError("9 OrderItem: quantity=101", p1, 101, "quantity must be in [5, 100]");
        expectItemOk("10 OrderItem: quantity=5 (нижняя граница)", p1, 5);
        expectItemOk("11 OrderItem: quantity=100 (верхняя граница)", p1, 100);
        try {
            OrderItem item = new OrderItem(p1, 5);
            item.increase(3);
            check("12 OrderItem: increase(3) при quantity=5", item.getQuantity() == 8, "quantity=" + item.getQuantity());
        } catch (RuntimeException e) {
            check("12 OrderItem: increase(3) при quantity=5", false, e.toString());
        }

        // --- Order (13–18) ---
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.addProduct(product("B", 200), 6);
            check("13 Order: addProduct двух разных товаров", o.getItems().size() == 2, "size=" + o.getItems().size());
        } catch (RuntimeException e) {
            check("13 Order: addProduct двух разных товаров", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.addProduct(product("A", 100), 3);
            boolean ok = o.getItems().size() == 1 && o.getItems().get(0).getQuantity() == 8;
            check("14 Order: addProduct того же id (R7) объединяет", ok, "size=" + o.getItems().size());
        } catch (RuntimeException e) {
            check("14 Order: addProduct того же id (R7) объединяет", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);     // 500
            o.addProduct(product("B", 250.5), 6);   // 1503
            check("15 Order: total двух позиций", near(o.total(), 2003.0), "total=" + o.total());
        } catch (RuntimeException e) {
            check("15 Order: total двух позиций", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.confirm();
            check("16 Order: confirm() непустого", o.getStatus() == OrderStatus.CONFIRMED, "status=" + o.getStatus());
        } catch (RuntimeException e) {
            check("16 Order: confirm() непустого", false, e.toString());
        }
        try {
            new Order(EMP).confirm();
            check("17 Order: confirm() пустого (R10)", false, "исключение не выброшено");
        } catch (IllegalStateException e) {
            check("17 Order: confirm() пустого (R10)", e.getMessage().contains("order is empty"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.confirm();
            o.addProduct(product("B", 100), 5);
            check("18 Order: addProduct после confirm (R8)", false, "исключение не выброшено");
        } catch (IllegalStateException e) {
            check("18 Order: addProduct после confirm (R8)", e.getMessage().contains("order is confirmed"), e.getMessage());
        }
    }

    private static void extraChecks() {
        // Product: дополнительные случаи R2, R3
        expectProductError("+ Product: name=\"\"", "M01", "", 100, "name is null or blank");
        expectProductError("+ Product: id из пробелов", "   ", "Monitor", 100, "id is null or blank");
        expectProductError("+ Product: price=NaN", "M01", "Monitor", Double.NaN, "price must be > 0");
        expectProductError("+ Product: price=Infinity", "M01", "Monitor", Double.POSITIVE_INFINITY, "price must be finite");
        try {
            Product p = new Product("M01", "Monitor", 0.01);
            check("+ Product: price=0.01 (минимально положительная)", p.getPrice() == 0.01, "price=" + p.getPrice());
        } catch (RuntimeException e) {
            check("+ Product: price=0.01 (минимально положительная)", false, e.toString());
        }

        // OrderItem: R4, R5, increase
        expectItemError("+ OrderItem: product=null (R5)", null, 5, "product is null");
        expectItemError("+ OrderItem: quantity=-1", product("P", 10), -1, "quantity must be in [5, 100]");
        try {
            OrderItem item = new OrderItem(product("P", 10), 5);
            item.increase(0);
            check("+ OrderItem: increase(0)", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ OrderItem: increase(0)", e.getMessage().contains("delta must be > 0"), e.getMessage());
        }
        try {
            OrderItem item = new OrderItem(product("P", 10), 95);
            item.increase(5);
            check("+ OrderItem: increase до ровно 100", item.getQuantity() == 100, "quantity=" + item.getQuantity());
        } catch (RuntimeException e) {
            check("+ OrderItem: increase до ровно 100", false, e.toString());
        }
        try {
            OrderItem item = new OrderItem(product("P", 10), 95);
            item.increase(6);
            check("+ OrderItem: increase до 101", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ OrderItem: increase до 101", e.getMessage().contains("quantity must be in [5, 100]"), e.getMessage());
        }
        try {
            OrderItem item = new OrderItem(product("P", 10), 95);
            try {
                item.increase(Integer.MAX_VALUE);
            } catch (IllegalArgumentException ignored) {
                // ожидаем отказ без переполнения int
            }
            check("+ OrderItem: после отказа quantity не изменилось", item.getQuantity() == 95, "quantity=" + item.getQuantity());
        } catch (RuntimeException e) {
            check("+ OrderItem: после отказа quantity не изменилось", false, e.toString());
        }
        try {
            OrderItem item = new OrderItem(product("P", 12.5), 8);
            check("+ OrderItem: getLineTotal = price × quantity", near(item.getLineTotal(), 100.0), "total=" + item.getLineTotal());
        } catch (RuntimeException e) {
            check("+ OrderItem: getLineTotal = price × quantity", false, e.toString());
        }

        // Employee (record)
        try {
            new Employee(" ", "Иванов");
            check("+ Employee: пустой id", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ Employee: пустой id", true, "");
        }
        check("+ Employee: record сравнивается по значению",
                new Employee("E-01", "А Б").equals(new Employee("E-01", "А Б")), "equals вернул false");

        // Order: R6, R7, R8, R9, R10, R11
        try {
            new Order(null);
            check("+ Order: employee=null (R6)", false, "исключение не выброшено");
        } catch (NullPointerException e) {
            check("+ Order: employee=null (R6)", e.getMessage().contains("employee is null"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.getItems().add(new OrderItem(product("B", 1), 5));
            check("+ Order: getItems().add() должен быть запрещён (R11)", false, "исключение не выброшено");
        } catch (UnsupportedOperationException e) {
            check("+ Order: getItems().add() должен быть запрещён (R11)", true, "");
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            List<OrderItem> snapshot = o.getItems();
            o.addProduct(product("B", 100), 5);
            check("+ Order: getItems() возвращает копию на момент вызова", snapshot.size() == 1 && o.getItems().size() == 2,
                    "size=" + snapshot.size());
        } catch (RuntimeException e) {
            check("+ Order: getItems() возвращает копию на момент вызова", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.addProduct(product("B", 10), 10);
            double before = o.total();
            o.removeProduct("B");
            check("+ Order: total() пересчитывается после removeProduct (R9)",
                    near(before, 600.0) && near(o.total(), 500.0) && o.getItems().size() == 1, "before=" + before + ", after=" + o.total());
        } catch (RuntimeException e) {
            check("+ Order: total() пересчитывается после removeProduct (R9)", false, e.toString());
        }
        check("+ Order: total() пустого заказа = 0", near(new Order(EMP).total(), 0.0), "total != 0");
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.removeProduct("ZZZ");
            check("+ Order: removeProduct несуществующего id", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ Order: removeProduct несуществующего id", e.getMessage().contains("product not found"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.confirm();
            o.removeProduct("A");
            check("+ Order: removeProduct после confirm (R8)", false, "исключение не выброшено");
        } catch (IllegalStateException e) {
            check("+ Order: removeProduct после confirm (R8)", e.getMessage().contains("order is confirmed"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 5);
            o.confirm();
            o.confirm();
            check("+ Order: повторный confirm (R8)", false, "исключение не выброшено");
        } catch (IllegalStateException e) {
            check("+ Order: повторный confirm (R8)", e.getMessage().contains("order is confirmed"), e.getMessage());
        }
        try {
            new Order(EMP).addProduct(null, 5);
            check("+ Order: addProduct(null, 5)", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ Order: addProduct(null, 5)", e.getMessage().contains("product is null"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 60);
            o.addProduct(product("A", 100), 41);
            check("+ Order: объединение сверх 100", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("+ Order: объединение сверх 100", e.getMessage().contains("quantity must be in [5, 100]"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 60);
            try {
                o.addProduct(product("A", 100), 41);
            } catch (IllegalArgumentException ignored) {
                // ожидаем отказ
            }
            check("+ Order: после отказа объединения количество прежнее", o.getItems().get(0).getQuantity() == 60,
                    "quantity=" + o.getItems().get(0).getQuantity());
        } catch (RuntimeException e) {
            check("+ Order: после отказа объединения количество прежнее", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("A", 100), 60);
            o.addProduct(product("A", 100), 40);
            check("+ Order: объединение ровно до 100", o.getItems().get(0).getQuantity() == 100, "ok");
        } catch (RuntimeException e) {
            check("+ Order: объединение ровно до 100", false, e.toString());
        }
    }

    /** Вариант 2, часть А: количество позиции в [5, 100] (минимум 5 вместо 1). */
    private static void variantPartA() {
        expectItemError("A: quantity=4 отклоняется (граница ниже минимума)", product("P", 10), 4, "quantity must be in [5, 100]");
        expectItemError("A: quantity=1 больше недопустимо (старый минимум)", product("P", 10), 1, "got 1");
        expectItemOk("A: quantity=5 допустимо (новый минимум)", product("P", 10), 5);
        try {
            Order o = new Order(EMP);
            o.addProduct(product("P", 10), 4);
            check("A: addProduct нового товара с количеством 4", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("A: addProduct нового товара с количеством 4", e.getMessage().contains("[5, 100]"), e.getMessage());
        }
        try {
            Order o = new Order(EMP);
            try {
                o.addProduct(product("P", 10), 4);
            } catch (IllegalArgumentException ignored) {
                // ожидаем отказ
            }
            check("A: после отказа позиция не добавлена", o.getItems().isEmpty(), "size=" + o.getItems().size());
        } catch (RuntimeException e) {
            check("A: после отказа позиция не добавлена", false, e.toString());
        }
        try {
            Order o = new Order(EMP);
            o.addProduct(product("P", 10), 5);
            o.addProduct(product("P", 10), 1);   // итоговое количество 6 ≥ 5, это допустимо
            check("A: добавление 1 к существующей позиции (итог 6)", o.getItems().get(0).getQuantity() == 6, "ok");
        } catch (RuntimeException e) {
            check("A: добавление 1 к существующей позиции (итог 6)", false, e.toString());
        }
    }

    /** Вариант 2, часть Б: OrderService.addMinQuantityCheck(Order). */
    private static void variantPartB() {
        Order empty = new Order(EMP);
        check("B: addMinQuantityCheck пустого заказа = true", SERVICE.addMinQuantityCheck(empty), "вернул false");
        try {
            Order o = SERVICE.createOrder(EMP);
            o.addProduct(product("A", 100), 5);
            o.addProduct(product("B", 100), 50);
            check("B: addMinQuantityCheck (5 и 50) = true", SERVICE.addMinQuantityCheck(o), "вернул false");
        } catch (RuntimeException e) {
            check("B: addMinQuantityCheck (5 и 50) = true", false, e.toString());
        }
        try {
            Order o = SERVICE.createOrder(EMP);
            o.addProduct(product("A", 100), 5);
            o.addProduct(product("A", 100), 2);
            check("B: addMinQuantityCheck после объединения = true", SERVICE.addMinQuantityCheck(o), "вернул false");
        } catch (RuntimeException e) {
            check("B: addMinQuantityCheck после объединения = true", false, e.toString());
        }
        try {
            Order o = SERVICE.createOrder(EMP);
            o.addProduct(product("A", 100), 5);
            o.confirm();
            check("B: addMinQuantityCheck подтверждённого заказа = true", SERVICE.addMinQuantityCheck(o), "вернул false");
        } catch (RuntimeException e) {
            check("B: addMinQuantityCheck подтверждённого заказа = true", false, e.toString());
        }
        try {
            SERVICE.addMinQuantityCheck(null);
            check("B: addMinQuantityCheck(null)", false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check("B: addMinQuantityCheck(null)", e.getMessage().contains("order is null"), e.getMessage());
        }
        try {
            Order o = SERVICE.createOrder(EMP);
            check("B: OrderService.createOrder создаёт пустой DRAFT",
                    o.getStatus() == OrderStatus.DRAFT && o.getItems().isEmpty() && o.getEmployee().equals(EMP), "состояние не совпало");
        } catch (RuntimeException e) {
            check("B: OrderService.createOrder создаёт пустой DRAFT", false, e.toString());
        }
    }

    // ---------- вспомогательные методы unit-проверок ----------

    private static void expectProductError(String name, String id, String productName, double price, String fragment) {
        try {
            new Product(id, productName, price);
            check(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check(name, e.getMessage().contains(fragment), "сообщение: " + e.getMessage());
        }
    }

    private static void expectItemOk(String name, Product p, int quantity) {
        try {
            OrderItem item = new OrderItem(p, quantity);
            check(name, item.getQuantity() == quantity, "quantity=" + item.getQuantity());
        } catch (RuntimeException e) {
            check(name, false, e.toString());
        }
    }

    private static void expectItemError(String name, Product p, int quantity, String fragment) {
        try {
            new OrderItem(p, quantity);
            check(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            check(name, e.getMessage().contains(fragment), "сообщение: " + e.getMessage());
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + ": " + detail);
        }
    }

    // ================= Режим 2: data-driven (data/sample.csv) =================

    /**
     * Формат: productId;name;price;quantity;expected. Все строки добавляются в один заказ.
     * expected: OK:qty=N | OK:merged,qty=N | merged | SKIP[:причина] | ERROR[:IllegalStateException].
     * Служебные строки: !confirm / !remove;id и итоговая строка TOTAL;positions=N;sum=X.
     * Строки с # и пустые игнорируются, первая значимая строка – заголовок.
     */
    private static void dataDriven() {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(CSV_PATH));
        } catch (IOException e) {
            failed++;
            System.out.println("FAIL data: не удалось прочитать " + CSV_PATH);
            return;
        }
        Order order = SERVICE.createOrder(EMP);
        int n = 0;
        int ok = 0;
        boolean headerSkipped = false;
        for (String row : lines) {
            if (row.isBlank() || row.startsWith("#")) {
                continue;
            }
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            n++;
            String problem;
            try {
                problem = checkRow(row, order);
            } catch (RuntimeException e) {
                problem = "необработанное исключение: " + e;
            }
            if (problem == null) {
                ok++;
                passed++;
                System.out.println("PASS data/" + n);
            } else {
                failed++;
                System.out.println("FAIL data/" + n + ": " + problem);
            }
        }
        System.out.println("Data-driven: " + ok + "/" + n + " passed");
    }

    /** Возвращает null, если строка выполнена как ожидалось, иначе описание расхождения. */
    private static String checkRow(String row, Order order) {
        String[] f = row.split(";", -1);
        if (f[0].equals("TOTAL")) {
            return checkTotal(f, order);
        }
        if (f[0].startsWith("!")) {
            return checkCommand(f, order);
        }
        if (f.length != 5) {
            return "ожидалось 5 колонок, получено " + f.length;
        }
        String expected = f[4];

        // Результат строки: kind = OK | IAE | ISE
        String kind;
        String message = "";
        boolean merged = false;
        int qty = -1;
        try {
            String id = noSurroundingSpaces("productId", f[0]);
            String name = noSurroundingSpaces("name", f[1]);
            double price = parsePrice(f[2]);
            int quantity = parseQuantity(f[3]);
            Product p = new Product(id, name, price);
            merged = findQuantity(order, p.getId()) >= 0;
            order.addProduct(p, quantity);
            qty = findQuantity(order, p.getId());
            kind = "OK";
        } catch (IllegalStateException e) {
            kind = "ISE";
            message = e.getMessage();
        } catch (IllegalArgumentException e) {
            kind = "IAE";
            message = e.getMessage();
        }
        return compare(expected, kind, message, merged, qty);
    }

    private static String compare(String expected, String kind, String message, boolean merged, int qty) {
        String actual = kind.equals("OK") ? "OK:" + (merged ? "merged," : "") + "qty=" + qty : kind + ": " + message;
        if (expected.equals("merged")) {
            return kind.equals("OK") && merged ? null : "ожидалось merged, получено " + actual;
        }
        if (expected.startsWith("OK")) {
            if (!kind.equals("OK")) {
                return "ожидалось " + expected + ", получено " + actual;
            }
            String tail = expected.length() > 3 ? expected.substring(3) : "";
            for (String token : tail.split(",")) {
                if (token.equals("merged") && !merged) {
                    return "ожидалось объединение (merged), но позиция новая: " + actual;
                }
                if (token.startsWith("qty=") && !token.equals("qty=" + qty)) {
                    return "ожидалось " + token + ", получено qty=" + qty;
                }
            }
            return null;
        }
        if (expected.equals("SKIP") || expected.startsWith("SKIP:")) {
            if (!kind.equals("IAE")) {
                return "ожидался SKIP (IllegalArgumentException), получено " + actual;
            }
            String reason = expected.length() > 5 ? expected.substring(5) : "";
            return message.contains(reason) ? null : "ожидалась причина «" + reason + "», получено «" + message + "»";
        }
        if (expected.equals("ERROR") || expected.equals("ERROR:IllegalStateException") || expected.equals("ERROR:ISE")) {
            return kind.equals("ISE") ? null : "ожидался IllegalStateException, получено " + actual;
        }
        if (expected.equals("ERROR:IllegalArgumentException") || expected.equals("ERROR:IAE")) {
            return kind.equals("IAE") ? null : "ожидался IllegalArgumentException, получено " + actual;
        }
        return "неизвестное значение expected: " + expected;
    }

    /** Служебные строки: !confirm;;;;ожидание и !remove;id;;;ожидание. */
    private static String checkCommand(String[] f, Order order) {
        if (f.length != 5) {
            return "ожидалось 5 колонок, получено " + f.length;
        }
        String kind;
        String message = "";
        try {
            if (f[0].equals("!confirm")) {
                order.confirm();
            } else if (f[0].equals("!remove")) {
                order.removeProduct(f[1]);
            } else {
                return "неизвестная команда: " + f[0];
            }
            kind = "OK";
        } catch (IllegalStateException e) {
            kind = "ISE";
            message = e.getMessage();
        } catch (IllegalArgumentException e) {
            kind = "IAE";
            message = e.getMessage();
        }
        String expected = f[4];
        if (expected.startsWith("OK")) {
            if (!kind.equals("OK")) {
                return "ожидалось " + expected + ", получено " + kind + ": " + message;
            }
            if (expected.startsWith("OK:status=") && !expected.equals("OK:status=" + order.getStatus())) {
                return "ожидалось " + expected + ", статус " + order.getStatus();
            }
            return null;
        }
        return compare(expected, kind, message, false, -1);
    }

    /** Итоговая строка TOTAL;positions=N;sum=X сверяется с составом и суммой заказа. */
    private static String checkTotal(String[] f, Order order) {
        if (f.length != 3 || !f[1].startsWith("positions=") || !f[2].startsWith("sum=")) {
            return "формат итоговой строки: TOTAL;positions=N;sum=X";
        }
        int positions;
        double sum;
        try {
            positions = Integer.parseInt(f[1].substring("positions=".length()));
            sum = Double.parseDouble(f[2].substring("sum=".length()));
        } catch (NumberFormatException e) {
            return "числа в итоговой строке разобрать не удалось";
        }
        if (order.getItems().size() != positions) {
            return "positions: ожидалось " + positions + ", получено " + order.getItems().size();
        }
        if (Math.abs(order.total() - sum) > 0.005) {
            return "sum: ожидалось " + f[2].substring(4) + ", получено " + String.format(Locale.ROOT, "%.2f", order.total());
        }
        return null;
    }

    private static int findQuantity(Order order, String productId) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                return item.getQuantity();
            }
        }
        return -1;
    }

    // ---------- разбор полей: ошибки формата – контролируемые IllegalArgumentException ----------

    private static String noSurroundingSpaces(String field, String value) {
        if (!value.isBlank() && !value.equals(value.strip())) {
            throw new IllegalArgumentException(field + " has surrounding spaces: \"" + value + "\"");
        }
        return value;
    }

    private static double parsePrice(String raw) {
        noSurroundingSpaces("price", raw);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("price is not a number: \"" + raw + "\"");
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price is not a number: \"" + raw + "\"");
        }
    }

    private static int parseQuantity(String raw) {
        noSurroundingSpaces("quantity", raw);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("quantity is not an integer: \"" + raw + "\"");
        }
    }

    // ================= Режим 3: интерактивный =================

    private static void interactive() {
        System.out.println("Команды: add <id>;<name>;<price>;<qty> | add <id> <name> <price> <qty> | remove <id> | total | list | confirm | new | exit");
        System.out.println("Пустая строка или exit – выход.");
        Scanner in = new Scanner(System.in, "UTF-8");
        Order order = SERVICE.createOrder(EMP);
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.isBlank() || line.strip().equals("exit")) {
                break;
            }
            try {
                order = handle(line.strip(), order);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    /** Выполняет одну команду; возвращает текущий заказ (команда new создаёт новый). */
    private static Order handle(String line, Order order) {
        int space = line.indexOf(' ');
        String cmd = space < 0 ? line : line.substring(0, space);
        String rest = space < 0 ? "" : line.substring(space + 1).strip();
        switch (cmd) {
            case "add" -> {
                String[] a = rest.contains(";") ? rest.split(";", -1) : rest.split("\\s+");
                if (a.length != 4) {
                    throw new IllegalArgumentException("ожидалось 4 значения: id, name, price, qty");
                }
                Product p = new Product(a[0], a[1], parsePrice(a[2]));
                order.addProduct(p, parseQuantity(a[3]));
                System.out.println("Добавлено: " + a[0] + ", позиций " + order.getItems().size()
                        + ", количество " + findQuantity(order, a[0]));
            }
            case "remove" -> {
                order.removeProduct(rest);
                System.out.println("Удалено: " + rest + ", позиций " + order.getItems().size());
            }
            case "total" -> System.out.println("Итог: " + String.format(Locale.ROOT, "%.2f", order.total())
                    + ", позиций " + order.getItems().size());
            case "list" -> {
                for (OrderItem item : order.getItems()) {
                    System.out.println(item.getProduct().getId() + " " + item.getProduct().getName()
                            + " × " + item.getQuantity() + " = " + String.format(Locale.ROOT, "%.2f", item.getLineTotal()));
                }
                System.out.println("Статус: " + order.getStatus());
            }
            case "confirm" -> {
                order.confirm();
                System.out.println("Заказ подтверждён, статус " + order.getStatus());
            }
            case "new" -> {
                System.out.println("Создан новый заказ (DRAFT)");
                return SERVICE.createOrder(EMP);
            }
            default -> throw new IllegalArgumentException("неизвестная команда: " + cmd);
        }
        return order;
    }
}
