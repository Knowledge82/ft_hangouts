# Этап 1. База данных: схема, модель и слой доступа к данным

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

## Архитектурное решение: DAO вместо CRUD внутри `DatabaseHelper`

Изначально CRUD-методы (`addContact`, `getAllContacts`, `updateContact`, `deleteContact`) были написаны прямо внутри `DatabaseHelper`. После обсуждения решено вынести их в отдельный класс — `ContactDao` (Data Access Object). Причина — **разделение ответственности** (separation of concerns): `DatabaseHelper` должен отвечать только за то, *как устроена* база (создание таблиц, версионирование), а не за то, *что с ней делать* (операции над данными). Если оставить всё в одном классе, при появлении новых таблиц (у нас уже запланирована `messages`) файл быстро превращается в "God Object" — раздутый класс, где вперемешку лежит логика для разных сущностей.

Это классический компромисс между простотой (меньше файлов, быстрее видно результат) и поддерживаемостью (проще расширять и не запутаться, когда операций станет больше). Для проекта, который будет расти (ещё предстоит `messages`, а затем UI поверх обоих), выбор сделан в пользу поддерживаемости.

## Файл 4: `ContactDao.java`

### Что это и зачем

DAO — класс, который **использует** `DatabaseHelper`, чтобы получить доступ к открытой базе, и предоставляет наружу только осмысленные операции над контактами (`addContact`, `getAllContacts`, `updateContact`, `deleteContact`). Остальной код приложения (будущие Activity) будет обращаться к базе **только через DAO**, никогда не работая с `DatabaseHelper` напрямую.

### Код

```java
package com.fortytwo.hangouts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.fortytwo.hangouts.DatabaseContract.*;

public class ContactDao {

    private final DatabaseHelper dbHelper;

    public ContactDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long addContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactEntry.COLUMN_NAME, contact.getName());
        values.put(ContactEntry.COLUMN_SURNAME, contact.getSurname());
        values.put(ContactEntry.COLUMN_PHONE, contact.getPhone());
        values.put(ContactEntry.COLUMN_EMAIL, contact.getEmail());
        values.put(ContactEntry.COLUMN_BIRTHDAY, contact.getBirthday());

        long newId = db.insert(ContactEntry.TABLE_NAME, null, values);
        db.close();
        return newId;
    }

    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + ContactEntry.TABLE_NAME +
                " ORDER BY " + ContactEntry.COLUMN_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_SURNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_BIRTHDAY))
                );
                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return contacts;
    }

    public int updateContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactEntry.COLUMN_NAME, contact.getName());
        values.put(ContactEntry.COLUMN_SURNAME, contact.getSurname());
        values.put(ContactEntry.COLUMN_PHONE, contact.getPhone());
        values.put(ContactEntry.COLUMN_EMAIL, contact.getEmail());
        values.put(ContactEntry.COLUMN_BIRTHDAY, contact.getBirthday());

        int rowsAffected = db.update(
                ContactEntry.TABLE_NAME,
                values,
                ContactEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(contact.getId())}
        );

        db.close();
        return rowsAffected;
    }

    public void deleteContact(long contactId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                ContactEntry.TABLE_NAME,
                ContactEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(contactId)}
        );
        db.close();
    }
}
```

### Новые понятия: `ContentValues` и `Cursor`

**`ContentValues`** — обёртка над парами "имя колонки → значение", предназначенная для безопасной вставки/обновления строк. Вместо ручной сборки строки `"INSERT INTO contacts (...) VALUES (...)"` (уязвимой к SQL-инъекциям и неудобной из-за экранирования кавычек), значения кладутся в объект `ContentValues`, а Android сам формирует и выполняет безопасный запрос.

**`Cursor`** — объект-курсор для постраничного прохода по результатам `SELECT`, без загрузки всех строк в память сразу. Аналог чтения файла через `fgets()` в цикле вместо одного `malloc()` на весь файл. У курсора есть `moveToFirst()`, `moveToNext()`, `getString(columnIndex)`, `getLong(columnIndex)` и т.д. **Курсор обязательно нужно закрывать** (`cursor.close()`) — под капотом он держит открытый нативный ресурс, и незакрытые курсоры со временем приводят к `SQLiteException: too many open cursors`. Прямая аналогия — не забывать `fclose()` после `fopen()`.

### Разбор построчно

**Композиция вместо наследования.** `DatabaseHelper extends SQLiteOpenHelper` — наследование ("является" отношением: `DatabaseHelper` — разновидность `SQLiteOpenHelper`). А вот `ContactDao` **не наследует** `DatabaseHelper` — он хранит ссылку на него как поле:
```java
private final DatabaseHelper dbHelper;
```
Это композиция ("содержит" отношение: `ContactDao` пользуется услугами `DatabaseHelper`, но не является им). Фундаментальный выбор в ООП-дизайне: наследование — когда класс действительно есть разновидность родителя; композиция — когда класс просто нуждается в объекте другого класса для своей работы. DAO не является базой данных, он просто использует объект, который умеет её открывать.

**`private final DatabaseHelper dbHelper;`** — `final` здесь применён к **ссылке на объект**, а не к примитивной константе (как было в `DatabaseContract`). Смысл: после присвоения в конструкторе саму ссылку нельзя переприсвоить на другой объект. Это не делает сам объект неизменяемым — просто фиксирует, на какой именно объект указывает поле, раз и навсегда.

**Конструктор `ContactDao(Context context)`** создаёт `DatabaseHelper` внутри себя:
```java
public ContactDao(Context context) {
    this.dbHelper = new DatabaseHelper(context);
}
```
Благодаря этому остальной код приложения будет работать так:
```java
ContactDao dao = new ContactDao(this); // this — Context, если вызывается из Activity
List<Contact> allContacts = dao.getAllContacts();
```
— вообще не зная о существовании `DatabaseHelper`. Это и есть практический смысл разделения ответственности: снаружи виден только удобный интерфейс (`addContact`, `getAllContacts`...), детали реализации (SQLiteOpenHelper, сырой SQL, курсоры) спрятаны внутри DAO.

**`db.insert(TABLE_NAME, null, values)`** — второй параметр (`null`) — это `nullColumnHack`, специфичная для SQLite деталь: что подставить, если `values` окажется полностью пустым (SQL не разрешает `INSERT INTO table () VALUES ()` без единой колонки). У нас `values` всегда заполнен, поэтому `null` — стандартная практика. Метод возвращает `long` — ID, присвоенный новой строке через `AUTOINCREMENT`, либо `-1` при неудаче.

**`getWritableDatabase()` vs `getReadableDatabase()`.** Оба метода унаследованы `DatabaseHelper` от `SQLiteOpenHelper`. Именно их вызов (а не создание объекта `DatabaseHelper`) запускает всю логику первого открытия базы: если файла `.db` ещё нет — вызывается `onCreate()`; если версия в коде выше версии файла на диске — вызывается `onUpgrade()`. `onCreate`/`onUpgrade` никогда не вызываются вручную, а `getWritableDatabase()`/`getReadableDatabase()` — вызываются явно каждый раз, когда нужно поработать с базой. Семантически `getReadableDatabase()` используется для операций чтения, `getWritableDatabase()` — для записи (хотя в современных версиях Android оба метода часто возвращают одно и то же соединение, разница важна на уровне читаемости кода).

**`db.rawQuery(query, null)`** — выполнение SQL, который **возвращает** результат (в отличие от `execSQL`, годного только для CREATE/DROP/INSERT-без-результата). Второй параметр — `selectionArgs`, массив значений для плейсхолдеров `?` (здесь их нет, поэтому `null`).

**Цикл `if (cursor.moveToFirst()) { do { ... } while (cursor.moveToNext()); }`.** `moveToFirst()` перемещает курсор на первую строку и возвращает `false`, если результат пуст — отсюда внешний `if`. Конструкция `do-while` подходит, поскольку внутри `if` уже точно известно, что курсор указывает на валидную первую строку — дальше просто двигаемся вперёд, пока `moveToNext()` не вернёт `false`.

**`cursor.getColumnIndexOrThrow(COLUMN_NAME)`.** Курсор хранит данные по числовым индексам колонок, а не по именам напрямую — сначала нужно узнать индекс нужной колонки по имени. Вариант `...OrThrow` выбрасывает исключение, если колонки с таким именем нет, что удобно для отладки (сразу видна ошибка при опечатке, а не тихий `-1` от обычного `getColumnIndex`). Метод для извлечения значения (`getLong`, `getString`) должен соответствовать реальному типу колонки, заданному в `CREATE TABLE`.

**Параметризация в `update`/`delete` — `WHERE ... = ?` + `whereArgs`.** Вместо прямой конкатенации значения в SQL-строку (`"COLUMN_ID = " + contact.getId()`), используется плейсхолдер `?` и отдельный массив аргументов:
```java
db.update(TABLE_NAME, values, ContactEntry.COLUMN_ID + " = ?", new String[]{String.valueOf(contact.getId())});
```
Это и есть защита от SQL-инъекций на уровне API: если бы значение подставлялось прямой конкатенацией, а оно приходило от пользователя — это была бы классическая уязвимость. Android API поощряет безопасный способ через `?` + `whereArgs`, где библиотека сама экранирует значение. `whereArgs` принимает только `String[]`, поэтому `long id` явно преобразуется через `String.valueOf(...)`.

**`updateContact` возвращает `int`** — количество затронутых строк (обычно `1`, если контакт с таким ID найден и обновлён; `0` — если такого ID не существует, что удобно проверять в вызывающем коде).

### Открытый вопрос: удаление контакта и связанные сообщения

Поскольку `messages` ссылается на `contacts` через `FOREIGN KEY` без `ON DELETE CASCADE`, при удалении контакта его сообщения **не удаляются автоматически** — SQLite по умолчанию не каскадирует удаление. Решение отложено до этапа реализации UI-логики удаления: либо удалять связанные сообщения отдельным запросом внутри `deleteContact`, либо добавить `ON DELETE CASCADE` в схему.

---

## Итог этапа

Созданы четыре файла, формирующие слой данных:
- `DatabaseContract` — константы (имена таблиц/колонок);
- `Contact` — модель одного контакта;
- `DatabaseHelper` — только создание и версионирование БД;
- `ContactDao` — CRUD-операции над контактами, единственная точка входа для работы с таблицей `contacts` для остального кода приложения.

Методы для таблицы `messages` (`MessageDao`) будут добавлены по тому же принципу на этапе работы с перепиской, чтобы не перегружать текущий этап. Таблицы пока не задействованы в UI — это следующий шаг.
