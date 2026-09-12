# Этап 1. База данных: схема и первые три класса

## Зачем своя БД, а не системная

Задание прямо запрещает использовать системную таблицу контактов Android (`ContactsContract`) — нужно реализовать собственное хранилище. Выбор пал на **SQLite** — компактную файловую реляционную СУБД, встроенную в Android "из коробки" (не требует отдельного сервера, всё хранится в одном файле `.db` на диске устройства).

Аналогия из C: это как структура + бинарный файл для персистентности, только с полноценным SQL — таблицами, запросами, типами данных — вместо ручной сериализации байт.

## Проектирование схемы

По заданию нужно минимум 5 полей на контакт, плюс возможность хранить историю переписки. Решено сделать две таблицы.

### Таблица `contacts`

| Поле | Тип | Назначение |
|---|---|---|
| `_id` | INTEGER PRIMARY KEY AUTOINCREMENT | уникальный ID (имя `_id` — соглашение Android для работы с курсорами и адаптерами) |
| `name` | TEXT NOT NULL | имя |
| `surname` | TEXT | фамилия |
| `phone` | TEXT NOT NULL | номер телефона |
| `email` | TEXT | почта |
| `birthday` | TEXT | дата рождения |

**Почему `birthday` — TEXT, а не отдельный тип "дата":** у SQLite на низком уровне всего 5 типов хранения (`TEXT`, `INTEGER`, `REAL`, `BLOB`, `NULL`) — отдельного типа даты нет. Есть три общепринятых способа представить дату: TEXT в формате ISO-8601 (`"1990-05-14"`), INTEGER как unix-timestamp, REAL как julian day number. Выбран первый вариант: он человекочитаем при отладке базы напрямую, не требует думать о часовых поясах (в отличие от timestamp) и at the same time сортируется лексикографически так же, как хронологически — то есть `ORDER BY birthday` в SQL сразу даёт правильный хронологический порядок без дополнительных преобразований.

### Таблица `messages`

| Поле | Тип | Назначение |
|---|---|---|
| `_id` | INTEGER PRIMARY KEY AUTOINCREMENT | ID сообщения |
| `contact_id` | INTEGER NOT NULL | внешний ключ на `contacts._id` |
| `body` | TEXT NOT NULL | текст сообщения |
| `timestamp` | INTEGER NOT NULL | unix-время в миллисекундах |
| `direction` | INTEGER NOT NULL | 0 = входящее, 1 = исходящее |

**Почему `direction` — число, а не строка:** простое числовое перечисление компактнее и быстрее в сравнении/индексации, чем строки `"incoming"`/`"outgoing"`. Смысл значений задокументирован здесь и в комментариях кода, поскольку сам SQLite не поддерживает enum-типы нативно.

Связь между таблицами задаётся через `FOREIGN KEY(contact_id) REFERENCES contacts(_id)` — это защищает от появления сообщений, ссылающихся на несуществующий контакт (referential integrity).

Имя файла базы данных: **`ft_hangouts.db`**, версия схемы: **1**.

---

## Файл 1: `DatabaseContract.java`

### Что это и зачем

Класс-контейнер строковых констант — имена таблиц и колонок. Нигде в коде не пишем строки `"contacts"` или `"name"` напрямую — вместо этого обращаемся через `ContactEntry.TABLE_NAME`, `ContactEntry.COLUMN_NAME` и т.д.

**Практическая причина:** если название колонки понадобится изменить, а на неё в коде ссылались как на голую строку в десятке мест (создание таблицы, вставка, чтение, обновление...) — придётся руками искать все места и не ошибиться ни в одном, при этом SQL как текст никак не проверяется компилятором. Если же везде используется константа — правка происходит в одном месте, а опечатка в имени константы (в отличие от опечатки в голой строке) сразу даёт ошибку компиляции, а не тихий баг в рантайме.

### Код

```java
package com.fortytwo.hangouts;

public final class DatabaseContract {

    private DatabaseContract() {}

    public static final String DATABASE_NAME = "ft_hangouts.db";
    public static final int DATABASE_VERSION = 1;

    public static final class ContactEntry {
        public static final String TABLE_NAME = "contacts";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SURNAME = "surname";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_BIRTHDAY = "birthday";
    }

    public static final class MessageEntry {
        public static final String TABLE_NAME = "messages";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_CONTACT_ID = "contact_id";
        public static final String COLUMN_BODY = "body";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_DIRECTION = "direction";
    }
}
```

### Разбор построчно

**Вложенные классы `ContactEntry` / `MessageEntry`.** Обе таблицы имеют колонку `COLUMN_ID` — без вложенности возникла бы коллизия имён на одном уровне. Вложенный класс работает как пространство имён: `DatabaseContract.ContactEntry.COLUMN_ID` и `DatabaseContract.MessageEntry.COLUMN_ID` — два разных символа. Аналог в C++ — `namespace ContactEntry { ... }`, только в Java для этого используются вложенные классы, отдельной конструкции `namespace` нет.

**`static`.** Означает, что поле принадлежит **самому классу**, а не отдельному объекту. Без `static` пришлось бы сначала создать объект (`new DatabaseContract()`), чтобы обратиться к полю через него. Со `static` — обращение идёт напрямую через имя класса, без единого `new`. Осмысленно здесь, потому что значение константы одинаково для всей программы и не зависит от какого-либо "экземпляра" — экземпляры этого класса нам вообще не нужны. Аналог в C — глобальная константа файла (`static const char *...`), только с явной привязкой к классу-неймспейсу вместо файла.

**`final`.** На поле — значение присваивается один раз и не может измениться (`TABLE_NAME = "..."` где-то ещё в коде вызовет ошибку компиляции). Прямой аналог — `const` в C. `static final` вместе — то же самое, что "глобальная неизменяемая константа" из C-опыта.

`final` **на классе** (`public final class DatabaseContract`) — другое значение того же слова: запрещает наследование от этого класса. Уместно, поскольку класс — чистый контейнер констант, наследоваться от него незачем.

**`private DatabaseContract() {}`.** Приватный конструктор без тела. Смысл — запретить создание объектов этого класса вообще: `new DatabaseContract()` не будет работать нигде за пределами самого класса, а он сам себя не вызывает. Если бы конструктор не был явно объявлен, Java автоматически подставила бы дефолтный `public`-конструктор без аргументов, что позволило бы бессмысленно создавать пустые объекты класса, у которого нет полей экземпляра — только `static` поля. Явный приватный конструктор — сигнал и компилятору, и любому читающему код, что класс — чистый неймспейс, не предназначенный для инстанцирования. Аналог в C — `.h`-файл с `#define`-константами: там в принципе нет понятия "создать экземпляр файла".

---

## Файл 2: `Contact.java`

### Что это и зачем

Модель данных — класс, представляющий один контакт в памяти программы. Когда контакт читается из базы, строка превращается в объект `Contact`; когда список контактов отображается на экране — работа идёт с объектами `Contact`, а не с сырыми SQL-курсорами напрямую. Аналог в C — `struct Contact { long id; char *name; ... };`, но с контролируемым доступом к полям и без ручного управления памятью.

### Код

```java
package com.fortytwo.hangouts;

public class Contact {
    private long id;
    private String name;
    private String surname;
    private String phone;
    private String email;
    private String birthday;

    public Contact(String name, String surname, String phone, String email, String birthday) {
        this.name = name;
        this.surname = surname;
        this.phone = phone;
        this.email = email;
        this.birthday = birthday;
    }

    public Contact(long id, String name, String surname, String phone, String email, String birthday) {
        this(name, surname, phone, email, birthday);
        this.id = id;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getBirthday() { return birthday; }

    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
}
```

### Разбор построчно

**Поля `private`, доступ через `get`/`set`.** Это инкапсуляция — один из столпов ООП. Напрямую `contact.name = "..."` извне класса не скомпилируется. Практический смысл: если позже понадобится, например, чтобы `setPhone()` автоматически убирал пробелы и дефисы из введённого номера —

```java
public void setPhone(String phone) {
    this.phone = phone.replaceAll("[\\s-]", "");
}
```

— логика меняется **в одном месте**, а не в каждом из мест по всему проекту, где раньше писали `contact.phone = "..."` напрямую. Такое именование (`getX`/`setX`) — стандартное соглашение Java (JavaBean convention), которое распознают IDE и многие библиотеки.

**Тип `id` — `long`, а не `int`.** SQLite и Android API договорились использовать `long` для автоинкрементных первичных ключей: `INTEGER PRIMARY KEY AUTOINCREMENT` в SQLite может занимать до 8 байт (64 бита), и методы вставки в Android SDK (`SQLiteDatabase.insert()`) возвращают именно `long`. Использование `long` в модели избавляет от лишних преобразований типов при работе с этим API.

**Два конструктора — перегрузка (overloading).** Один без `id` — для нового контакта, которому ID ещё не присвоен (это сделает база при вставке через `AUTOINCREMENT`). Второй с `id` — для контакта, уже прочитанного из базы. Java различает их по числу и типам параметров при вызове.

**`this(name, surname, phone, email, birthday);` внутри второго конструктора** — вызов другого конструктора того же класса (constructor chaining). Должен стоять первой строкой тела конструктора — таково правило языка. Смысл — не дублировать пятикратное присваивание полей в обоих конструкторах (принцип DRY, Don't Repeat Yourself).

---

## Файл 3: `DatabaseHelper.java`

### Что это и зачем

Класс, который реально создаёт и версионирует файл базы данных на диске устройства. Решает конкретную проблему: при первом запуске приложения файла `.db` ещё не существует — кто-то должен выполнить `CREATE TABLE`; при последующих запусках файл уже есть, и пересоздавать его с нуля нельзя (это уничтожило бы данные пользователя). `SQLiteOpenHelper` из Android SDK берёт этот жизненный цикл на себя.

### Код

```java
package com.fortytwo.hangouts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.fortytwo.hangouts.DatabaseContract.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createContactsTable = "CREATE TABLE " + ContactEntry.TABLE_NAME + " (" +
                ContactEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ContactEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                ContactEntry.COLUMN_SURNAME + " TEXT, " +
                ContactEntry.COLUMN_PHONE + " TEXT NOT NULL, " +
                ContactEntry.COLUMN_EMAIL + " TEXT, " +
                ContactEntry.COLUMN_BIRTHDAY + " TEXT)";

        String createMessagesTable = "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                MessageEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageEntry.COLUMN_CONTACT_ID + " INTEGER NOT NULL, " +
                MessageEntry.COLUMN_BODY + " TEXT NOT NULL, " +
                MessageEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                MessageEntry.COLUMN_DIRECTION + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + MessageEntry.COLUMN_CONTACT_ID + ") REFERENCES " +
                ContactEntry.TABLE_NAME + "(" + ContactEntry.COLUMN_ID + "))";

        db.execSQL(createContactsTable);
        db.execSQL(createMessagesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ContactEntry.TABLE_NAME);
        onCreate(db);
    }
}
```

### Разбор построчно

**`extends SQLiteOpenHelper`.** Наследование: `DatabaseHelper` становится подклассом уже существующего в Android SDK класса, автоматически получает его функциональность (управление файлом БД, версионирование) и обязан реализовать методы, которые родитель объявил абстрактными.

**`@Override`.** Аннотация, сообщающая компилятору: "этот метод переопределяет метод родителя, а не создаёт новый". Защита от опечаток — без `@Override` опечатка в имени метода (`onCreat` вместо `onCreate`) молча создала бы никогда не вызываемый новый метод; с `@Override` компилятор сразу выдаст ошибку, если имя не совпадает ни с одним методом родителя.

**`super(context, DATABASE_NAME, null, DATABASE_VERSION);`** — вызов конструктора родителя (`SQLiteOpenHelper`). Аргументы:
- `context` — ссылка на "окружение" приложения; нужен, поскольку путь к файлу базы (`/data/data/com.fortytwo.hangouts/databases/`) вычисляется именно через него;
- `DATABASE_NAME` — имя файла, константа из `DatabaseContract`, доступна напрямую благодаря `import static ...DatabaseContract.*;`;
- `null` — `CursorFactory`, кастомизация построения объектов `Cursor`; стандартное поведение по умолчанию устраивает, поэтому `null`;
- `DATABASE_VERSION` — номер версии схемы; при его увеличении в будущем система сама вызовет `onUpgrade()`.

**`onCreate(SQLiteDatabase db)`.** Вызывается системой автоматически, ровно один раз — в момент первого реального обращения к ещё не существующей базе (через `getWritableDatabase()`/`getReadableDatabase()`). `db.execSQL(...)` выполняет SQL-команды, не возвращающие результат (CREATE, DROP — структурные операции, в отличие от SELECT).

**Сборка SQL через конкатенацию строк (`+`).** Оправдана здесь, поскольку все склеиваемые куски — константы из собственного кода, а не пользовательский ввод (SQL-инъекции тут неприменимы). Для операций с данными (INSERT/UPDATE/SELECT), которые будут добавлены на следующем этапе, вместо конкатенации значений будут использоваться `ContentValues` и параметризованные запросы (`selectionArgs`) — именно там инъекции становятся реальным риском, если данные приходят от пользователя.

**`FOREIGN KEY(contact_id) REFERENCES contacts(_id)`.** Referential integrity — SQLite проверяет, что значение в `contact_id` соответствует существующему `_id` в `contacts`. По умолчанию SQLite не проверяет это строго без явного `PRAGMA foreign_keys=ON` (будет включено на этапе работы с соединением), но связь уже задокументирована в схеме.

**`onUpgrade()`.** Обязателен к реализации, так как `SQLiteOpenHelper` — абстрактный класс. Вызывается при увеличении `DATABASE_VERSION`, получает старую и новую версию как аргументы. Текущая реализация — самая грубая стратегия (снести всё и создать заново, потеряв данные) — допустима для учебного проекта, где версия схемы не меняется после сдачи; в продакшене обычно используют `ALTER TABLE`, чтобы не терять пользовательские данные при обновлении.

---

## Итог этапа

Созданы три файла, формирующие основу слоя данных: `DatabaseContract` (константы), `Contact` (модель), `DatabaseHelper` (создание/версионирование БД). Таблицы ещё не наполняются и не читаются — это следующий этап (CRUD-операции).
