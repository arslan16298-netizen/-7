# Практическая работа №7. Первое полноценное ООП-приложение. Корпоративный заказ оборудования

Студент: Ахметгалиев Арслан Альбертович, группа ЭФБО-17-25. Вариант 2.

Консольное приложение формирует заказ оборудования сотрудника: `Product` → `OrderItem` → `Order`, сервис `OrderService`.
По варианту 2 количество позиции допустимо в диапазоне [5, 100] (часть А); добавлен метод
`OrderService.addMinQuantityCheck(Order)` (часть Б).

Java SE 21, без Maven и сторонних библиотек, без Stream API и лямбд.

## Структура

```
src/ru/edu/pr04/Product.java        товар, проверки R1–R3
src/ru/edu/pr04/Employee.java       record сотрудника
src/ru/edu/pr04/OrderItem.java      позиция, диапазон количества R4 (5–100)
src/ru/edu/pr04/Order.java          заказ: R6–R11
src/ru/edu/pr04/OrderStatus.java    enum DRAFT / CONFIRMED
src/ru/edu/pr04/OrderService.java   сервисный слой (stateless)
src/ru/edu/pr04/SelfCheck.java      main, unit-проверки, data-driven и интерактивный режимы
data/sample.csv                     productId;name;price;quantity;expected
diagrams/                           activity и class (.dot, .png)
DECISIONS.md                        решения, ответы на вопросы, анализ фрагмента
```

## Компиляция

```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

## Запуск

```
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr04.SelfCheck               # unit-проверки + data/sample.csv
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr04.SelfCheck --csv         # только data/sample.csv
java -Dstdout.encoding=UTF-8 -cp out ru.edu.pr04.SelfCheck --interactive # команды из консоли
```

Запускать из корня проекта (файл `data/sample.csv` читается по относительному пути). Код выхода 1, если есть FAIL.
Windows PowerShell: вместо `$(find ...)` используйте `javac -encoding UTF-8 -d out src\ru\edu\pr04\*.java`.

Команды интерактивного режима: `add M01;Monitor 27;32990;5` (или `add M01 Monitor 32990 5`), `remove M01`, `total`, `list`,
`confirm`, `new`, `exit` (пустая строка тоже завершает).

## Формат data/sample.csv

`productId;name;price;quantity;expected`. Ожидания: `OK:qty=N`, `OK:merged,qty=N`, `SKIP:<причина>` (ошибка данных,
`IllegalArgumentException`), `ERROR:IllegalStateException`. Служебные строки: `!confirm`, `!remove;<id>`,
`TOTAL;positions=N;sum=X`. Строки с `#` — комментарии. Все строки применяются к одному заказу.

## Принятые допущения

- Товар с тем же id объединяется с существующей позицией независимо от name и price (остаётся первый `Product`).
- Правило [5, 100] относится к итоговому количеству позиции, поэтому добавка к существующей позиции допустима, если итог в диапазоне.
- `removeProduct` для отсутствующего id — `IllegalArgumentException`; `confirm` повторно — `IllegalStateException`.
- Пробелы вокруг значений в CSV не допускаются (контролируемая ошибка).

## Граничные проверки (результаты прогона)

| Проверка | Вход | Результат |
|---|---|---|
| Нижняя граница количества | quantity = 5 | принято (`OrderItem` создан) |
| Ниже нижней границы | quantity = 4 | `IllegalArgumentException: quantity must be in [5, 100], got 4` |
| Верхняя граница количества | quantity = 100 | принято |
| Выше верхней границы | quantity = 101; объединение 100 + 1 | `IllegalArgumentException`, количество прежнее |
| Цена | price = 0 / −100 / NaN / 0.01 | первые три отклонены, 0.01 принята |
| Пустое значение | id = `""`, name = `null` | `IllegalArgumentException: … is null or blank` |
| Неверный формат | price = `abc`, quantity = `5.5` | `IllegalArgumentException` (SKIP в CSV) |
| Повтор товара | M01 ×5, затем M01 ×3 | одна позиция, quantity = 8 |
| Пустой заказ | `confirm()` | `IllegalStateException: order is empty` |
| Подтверждённый заказ | `addProduct` / `removeProduct` / `confirm` | `IllegalStateException: order is confirmed` |
| Неизменяемость | `getItems().add(...)` | `UnsupportedOperationException` |

Итог прогона: `PASS 85, FAIL 0` (56 unit-проверок, из них 18 базовых и 12 по варианту, и 29 строк CSV).
