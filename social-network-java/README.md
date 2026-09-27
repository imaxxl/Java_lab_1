# Social Network Lab 1

Проект выполнен на Java 21, Gradle Kotlin DSL, JavaFX и JUnit 5.

Запуск:

`./gradlew run`

Тесты:

`./gradlew test`

Основная сущность — Profile.

Типы:
- Profile — базовая сущность.
- DeletedProfile — read-only сущность.
- Community — редактируемая сущность с дополнительными полями.

CSV использует разделитель `;`.

Поддерживаются типы PROFILE, DELETED и COMMUNITY.

Битые строки не останавливают загрузку всего файла. Для них создаются CsvLoadException с кодами ошибок.

Граф использует собственную структуру EdgeNode для списков смежности. Кратчайшая цепочка знакомств ищется алгоритмом BFS.

## Запуск в Windows без отдельной установки Gradle

В VS Code откройте корень проекта и выполните в терминале:

`gradlew.bat test`

Затем:

`gradlew.bat run`

При первом запуске `gradlew.bat` автоматически скачает Gradle 8.10.2 и сохранит его в папке проекта `.gradle-local`.
