# Этап 5. Редактирование и удаление контакта

## Задача

Реализовать оставшиеся операции CRUD, которых не хватало после добавления контакта: **Update** (редактирование существующего контакта) и **Delete** (удаление). Заодно — разобраться подробно, как устроена связь между экранами в Android (`Activity`, `Intent`), потому что именно на этом этапе она впервые используется не только для запуска, но и для передачи данных между двумя экранами.

Этап получился длинным не из-за самого CRUD (он написался быстро), а из-за серии мелких, но поучительных багов, вылезших при тестировании на реальном устройстве — каждый разобран отдельно ниже, с полным ходом расследования через Logcat.

## `ContactDao.java` — недостающий метод `getContactById`

```java
public Contact getContactById(long contactId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Contact contact = null;

    Cursor cursor = db.query(
            ContactEntry.TABLE_NAME,
            null,
            ContactEntry.COLUMN_ID + " = ?",
            new String[]{String.valueOf(contactId)},
            null, null, null
    );

    if (cursor.moveToFirst()) {
        contact = new Contact(
                cursor.getLong(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_SURNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_PHONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(ContactEntry.COLUMN_BIRTHDAY))
        );
    }

    cursor.close();
    db.close();
    return contact;
}
```

`db.query(...)` вместо `db.rawQuery("SELECT * FROM ...")` — оба способа равнозначны, `query` собирает SQL из параметров, снижая риск синтаксической опечатки в строке. `updateContact()` и `deleteContact()` были написаны верно ещё на этапе создания DAO.

## `activity_main.xml` — финальная версия

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/primary">

        <TextView
            android:id="@+id/toolbarTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:textColor="@android:color/white"
            android:textSize="20sp"
            android:textStyle="bold" />

    </androidx.appcompat.widget.Toolbar>

    <ListView
        android:id="@+id/contactListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:divider="@android:color/transparent"
        android:dividerHeight="12dp"
        android:clipToPadding="false"
        android:paddingHorizontal="12dp"
        android:paddingVertical="8dp"
        android:listSelector="@android:color/transparent" />

</LinearLayout>
```

## `contact_list_item.xml` — финальная версия

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_card"
    android:foreground="@drawable/bg_card_ripple"
    android:elevation="2dp"
    android:padding="16dp">

    <TextView
        android:id="@+id/contactNameTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="17sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary" />

    <TextView
        android:id="@+id/contactPhoneTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:textSize="14sp"
        android:textColor="@color/text_secondary" />

</LinearLayout>
```

Обрати внимание — **никакого `android:clickable`/`android:focusable` тут больше нет** (о том, почему они были ошибкой, — отдельный разбор ниже).

## `MainActivity.java` — финальная версия

```java
package com.fortytwo.ft_hangouts;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.util.TypedValue;
import android.view.ViewGroup;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ContactDao contactDao;
    private ContactAdapter contactAdapter;
    private ListView contactListView;

    private void refreshContactList() {
        List<Contact> contacts = contactDao.getAllContacts();
        contactAdapter.clear();
        contactAdapter.addAll(contacts);
        contactAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        final int actionBarHeightPx = TypedValue.complexToDimensionPixelSize(
                typedValue.data, getResources().getDisplayMetrics());

        toolbar.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarInset = insets.getSystemWindowInsetTop();

            v.setPadding(v.getPaddingLeft(), statusBarInset, v.getPaddingRight(), v.getPaddingBottom());

            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = actionBarHeightPx + statusBarInset;
            v.setLayoutParams(params);

            return insets;
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("ft_hangouts");

        contactDao = new ContactDao(this);
        contactListView = findViewById(R.id.contactListView);

        contactAdapter = new ContactAdapter(this, contactDao.getAllContacts());
        contactListView.setAdapter(contactAdapter);

        contactListView.setOnItemClickListener((parent, view, position, id) -> {
            Contact contact = contactAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, AddEditContactActivity.class);
            intent.putExtra(AddEditContactActivity.EXTRA_CONTACT_ID, contact.getId());
            startActivity(intent);
        });

        contactListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Contact contact = contactAdapter.getItem(position);

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Удалить контакт")
                    .setMessage("Удалить " + contact.getName() + " " + contact.getSurname() + "?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        contactDao.deleteContact(contact.getId());
                        refreshContactList();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        contactAdapter.clear();
        contactAdapter.addAll(contactDao.getAllContacts());
        contactAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_contact) {
            startActivity(new Intent(this, AddEditContactActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
```

## `activity_add_edit_contact.xml` — финальная версия

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/addEditRoot"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/primary"
        app:titleTextColor="@android:color/white">

        <TextView
            android:id="@+id/toolbarTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:textColor="@android:color/white"
            android:textSize="20sp"
            android:textStyle="bold" />

    </androidx.appcompat.widget.Toolbar>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <EditText
            android:id="@+id/nameEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Имя"
            android:inputType="textPersonName"
            android:autofillHints="name" />

        <EditText
            android:id="@+id/surnameEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Фамилия"
            android:inputType="textPersonName" />

        <EditText
            android:id="@+id/phoneEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Телефон"
            android:inputType="phone"
            android:autofillHints="phone" />

        <EditText
            android:id="@+id/emailEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Email"
            android:inputType="textEmailAddress"
            android:autofillHints="emailAddress" />

        <EditText
            android:id="@+id/birthdayEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Дата рождения (ГГГГ-ММ-ДД)"
            android:focusable="false" />

        <Button
            android:id="@+id/saveButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="Сохранить"
            android:backgroundTint="@color/primary" />

    </LinearLayout>

</LinearLayout>
```

## `AddEditContactActivity.java` — финальная версия

```java
package com.fortytwo.ft_hangouts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import android.util.TypedValue;
import android.view.ViewGroup;
import java.util.Calendar;
import java.util.Locale;

public class AddEditContactActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "contact_id";

    private final Calendar birthdayCalendar = Calendar.getInstance();
    private EditText nameEditText, surnameEditText, phoneEditText, emailEditText, birthdayEditText;
    private ContactDao contactDao;

    private long contactId = -1;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
        final int actionBarHeightPx = TypedValue.complexToDimensionPixelSize(
                typedValue.data, getResources().getDisplayMetrics());

        toolbar.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarInset = insets.getSystemWindowInsetTop();

            v.setPadding(v.getPaddingLeft(), statusBarInset, v.getPaddingRight(), v.getPaddingBottom());

            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = actionBarHeightPx + statusBarInset;
            v.setLayoutParams(params);

            return insets;
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);

        contactDao = new ContactDao(this);

        nameEditText = findViewById(R.id.nameEditText);
        surnameEditText = findViewById(R.id.surnameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        birthdayEditText = findViewById(R.id.birthdayEditText);
        birthdayEditText.setOnClickListener(v -> showDatePicker());

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveContact());

        contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, -1);
        isEditMode = contactId != -1;

        if (isEditMode) {
            toolbarTitle.setText("Редактировать контакт");
            loadContact();
        } else {
            toolbarTitle.setText("Новый контакт");
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    birthdayCalendar.set(year, month, dayOfMonth);
                    String formatted = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    birthdayEditText.setText(formatted);
                },
                birthdayCalendar.get(Calendar.YEAR),
                birthdayCalendar.get(Calendar.MONTH),
                birthdayCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadContact() {
        Contact contact = contactDao.getContactById(contactId);
        if (contact == null) return;

        nameEditText.setText(contact.getName());
        surnameEditText.setText(contact.getSurname());
        phoneEditText.setText(contact.getPhone());
        emailEditText.setText(contact.getEmail());
        birthdayEditText.setText(contact.getBirthday());

        if (contact.getBirthday() != null && !contact.getBirthday().isEmpty()) {
            try {
                String[] parts = contact.getBirthday().split("-");
                birthdayCalendar.set(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2])
                );
            } catch (Exception ignored) {
                // некорректный формат в базе — просто откроется сегодняшняя дата
            }
        }
    }

    private void saveContact() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Имя и телефон обязательны", Toast.LENGTH_SHORT).show();
            return;
        }

        String surname = surnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String birthday = birthdayEditText.getText().toString().trim();

        if (isEditMode) {
            Contact contact = new Contact(contactId, name, surname, phone, email, birthday);
            contactDao.updateContact(contact);
        } else {
            Contact contact = new Contact(name, surname, phone, email, birthday);
            contactDao.addContact(contact);
        }

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
```

## Activity, MainActivity, Intent — как это устроено на самом деле

### Контраст с C

В C у программы один вход — `main()`: ОС запускает файл, вызывается `main()`, код идёт линейно, `main()` возвращает значение, процесс завершается. Один старт, один конец, управление целиком в руках программиста.

В Android иначе: приложение — это набор **отдельных экранов**, каждым из которых управляет система, а не сам код — она может создать экран, поставить на паузу, убить и пересоздать в любой момент (пользователь свернул приложение, позвонили, кончилась память).

### Activity — единица экрана

`Activity` — базовый класс SDK, представляющий один экран. В проекте два наследника — `MainActivity` (список) и `AddEditContactActivity` (форма). Каждый — самостоятельный объект, управляемый системой через методы **жизненного цикла**, вызываемые не программистом, а системой в ответ на события:

- **`onCreate(Bundle savedInstanceState)`** — один раз, при первом создании экрана: `setContentView`, `findViewById`, настройка слушателей.
- **`onStart()`** — экран становится видимым.
- **`onResume()`** — экран на переднем плане, доступен для взаимодействия. Используется в `MainActivity` для перечитывания списка из базы — вызывается **каждый раз** при возврате на экран, в отличие от `onCreate()`.
- **`onPause()`** — экран уходит на второй план (например, поверх открылась `AddEditContactActivity`).
- **`onStop()`** — экран полностью не виден.
- **`onDestroy()`** — экран уничтожается.

Ключевое отличие от `main()` в C: программист не решает, когда эти методы вызываются — только что происходит **внутри** них. Это называется **инверсией управления**: framework вызывает код приложения, а не наоборот.

### Intent — как один экран просит систему открыть другой

Activity изолированы друг от друга — нет прямого вызова метода одной из другой, как обычной функции в C. Чтобы попросить систему открыть другой экран, используется **`Intent`** ("намерение") — объект-сообщение, описывающий, что нужно сделать, передаваемый системе, а не напрямую целевой Activity.

**Явный Intent** (когда класс назначения известен точно):
```java
Intent intent = new Intent(MainActivity.this, AddEditContactActivity.class);
intent.putExtra(AddEditContactActivity.EXTRA_CONTACT_ID, contact.getId());
startActivity(intent);
```
`putExtra(ключ, значение)` — единственный официальный способ передать данные между двумя Activity: они не разделяют общую область памяти, как две функции в одном C-файле.

**Неявный Intent** — вместо конкретного класса описывается действие, и система сама подбирает подходящее приложение (пригодится позже для звонка/SMS через системные приложения).

### Симуляция: тап по карточке → редактирование → сохранение

1. `MainActivity` активна, в состоянии после `onResume()`.
2. Тап по карточке "Иван Петров" → `contact.getId()` возвращает `3`.
3. Создаётся `Intent` с `EXTRA_CONTACT_ID = 3`, вызывается `startActivity(intent)`.
4. `startActivity()` — запрос системному `ActivityManagerService`, а не прямой вызов метода целевой Activity.
5. Система создаёт новый объект `AddEditContactActivity`, вызывает `onCreate()`.
6. `getIntent().getLongExtra(EXTRA_CONTACT_ID, -1)` → `3`; `isEditMode = true`; `loadContact()` заполняет форму.
7. `MainActivity` параллельно уходит в `onPause()` → `onStop()` (не уничтожена — держится в памяти на случай нажатия "назад").
8. Пользователь правит поле, жмёт Save → `contactDao.updateContact(...)` → `finish()`.
9. `finish()` → `onPause()` → `onStop()` → `onDestroy()` у `AddEditContactActivity`, объект удаляется из памяти.
10. Система возвращает `MainActivity` на передний план: `onRestart()` → `onStart()` → `onResume()`.
11. Именно в этом (втором по счёту, не путать с `onCreate()`, вызванным только один раз при первом запуске) `onResume()` срабатывает `refreshContactList()`, и пользователь видит обновлённые данные.

### Аналогия с C

Грубо: `Activity` — как если бы вместо функций внутри одной программы использовались отдельные процессы, которые не вызываются напрямую, а запрашиваются у ОС через что-то вроде `fork()`+`exec()`, с данными, переданными через `argv` (это и есть `Intent.putExtra`). Методы жизненного цикла — как если бы ОС сама решала, когда процессу выделить время, а когда "заморозить", присылая сигналы в нужные моменты, вместо линейного выполнения одного `main()` до конца.

## Реальные баги, пойманные на этом этапе — разбор расследования

Ниже — пять проблем подряд, каждая обнаружена через Logcat/визуально на реальном устройстве. Разбор оставлен подробным специально: сам процесс диагностики полезнее готового ответа.

### Баг 1 — Toolbar прижат к статус-бару (edge-to-edge)

На Android 14–15 контент по умолчанию рисуется "во весь экран" (edge-to-edge), из-за чего Toolbar оказывался прижат вплотную к системной области (время, значки). Первая попытка исправления — обычный `padding` на корневом `LinearLayout` — дала эффект, визуально неотличимый от `margin`: сверху появлялась полоса **фона корня** (не цвета Toolbar), а не цельная цветная область.

**Правильное решение** — применить отступ и увеличение высоты **прямо к `Toolbar`**, а не к корню, через чистый framework API `WindowInsets` (доступен с API 20, без AndroidX):

```java
Toolbar toolbar = findViewById(R.id.toolbar);

TypedValue typedValue = new TypedValue();
getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
final int actionBarHeightPx = TypedValue.complexToDimensionPixelSize(
        typedValue.data, getResources().getDisplayMetrics());

toolbar.setOnApplyWindowInsetsListener((v, insets) -> {
    int statusBarInset = insets.getSystemWindowInsetTop();
    v.setPadding(v.getPaddingLeft(), statusBarInset, v.getPaddingRight(), v.getPaddingBottom());

    ViewGroup.LayoutParams params = v.getLayoutParams();
    params.height = actionBarHeightPx + statusBarInset;
    v.setLayoutParams(params);

    return insets;
});
```

Первая попытка (только `padding`, без увеличения `height`) добавляла отступ **внутри** и без того фиксированной высоты `Toolbar` (`?attr/actionBarSize`) — контенту (заголовку, иконке меню) не хватало места, из-за чего текст обрезался, а иконка "Добавить" вовсе пропадала за пределами видимой области. Решение — явно увеличивать `params.height` на величину отступа, а не просто вписывать отступ в прежний размер: тогда содержимому достаётся полная штатная высота, а сверху добавляется ровно нужная цветная полоса под статус-бар.

### Баг 2 — прямоугольный `id` в другом layout-файле

При копировании кода для отступа между `MainActivity` и `AddEditContactActivity` в одном из файлов остался `findViewById(R.id.mainRoot)`, хотя корень `activity_add_edit_contact.xml` называется `addEditRoot`. Ошибка Android Studio: *"`@layout/activity_add_edit_contact` does not contain a declaration with id `mainRoot`"* — `id` как константа существует (объявлен в другом файле), но конкретно в этом layout его нет.

### Баг 3 — дублирование переменной `toolbar`

При последовательных правках в одном `onCreate()` дважды объявилась переменная с одним именем:
```java
View toolbar = findViewById(R.id.toolbar);
...
Toolbar toolbar = findViewById(R.id.toolbar); // конфликт имён
```
Java не разрешает повторное объявление переменной в одной области видимости. Решение — объединить в одно объявление типа `Toolbar` (он и есть `View`, так что оба варианта использования доступны на одном объекте).

**Важное следствие**: пока была эта ошибка компиляции, приложение не пересобиралось — на устройстве продолжала работать **старая** версия `.apk`. Отсюда временная иллюзия "Edit/Delete не работают вообще" — на самом деле работал устаревший код, где этой функциональности ещё не было.

### Баг 4 — клик по карточке не доходит до `ListView`

После исправления сборки клики по карточкам всё равно ничего не делали — только показывался ripple. Причина нашлась в `contact_list_item.xml`:
```xml
android:clickable="true"
android:focusable="true"
```
Когда корневой элемент строки списка явно объявлен `clickable`, он **сам** обрабатывает касание как собственный клик и не передаёт событие наверх, в `ListView.OnItemClickListener` — а поскольку своего `OnClickListener` у строки нет, клик просто "проглатывается" молча, без ошибок. Решение — убрать оба атрибута: `ListView` сам управляет состоянием `pressed` своих дочерних элементов, когда на нём есть `OnItemClickListener`, независимо от того, объявлена ли строка `clickable`.

### Баг 5 — прямоугольный ripple при долгом тапе

После исправления бага 4 обнаружился ещё один слой: при долгом тапе поверх аккуратного скруглённого ripple (из `bg_card_ripple.xml`) проступал **второй**, прямоугольный эффект — встроенный `android:listSelector` самого `ListView`, рисуемый краем в край поверх всей строки, независимо от `foreground` конкретного элемента. При быстром тапе он был почти незаметен из-за мгновенного перехода на другой экран, а при долгом — оставался на виду всё время удержания. Решение:
```xml
android:listSelector="@android:color/transparent"
```

### Баг 6 — `NullPointerException` при открытии формы редактирования

После добавления центрированного заголовка в Toolbar приложение стало падать при открытии `AddEditContactActivity`:
```
NullPointerException: Attempt to invoke virtual method '... ContactDao.getContactById(long)' on a null object reference
at AddEditContactActivity.loadContact(...)
```
Причина — строка `contactDao = new ContactDao(this);` при очередной правке `onCreate()` оказалась **после** места, где вызывается `loadContact()` (или выпала вовсе), хотя поле `contactDao` было объявлено в классе. Урок: объявление поля класса гарантирует только существование переменной (со значением `null` по умолчанию) — реальная инициализация конкретным объектом происходит там, где стоит `= new ContactDao(this)`, и должна располагаться в коде строго **до** первого использования, а не "где-то в файле".

### Побочные правки

**`autofillHints`** — framework-атрибут (API 26+), сообщающий системе автозаполнения тип ожидаемых данных поля (`name`, `phone`, `emailAddress`); не библиотека, добавлен по рекомендации Lint.

**`DatePickerDialog` для даты рождения** — цифровая клавиатура не позволяла ввести разделитель `-`. Поле сделано некликабельным для текстового ввода (`android:focusable="false"`), вместо клавиатуры открывается системный календарь. Формат приведён к ISO 8601 (`ГГГГ-ММ-ДД`). `month + 1` компенсирует нумерацию месяцев с нуля в `Calendar` (январь = `0`).

**Центрированный заголовок Toolbar** — у `android.widget.Toolbar` нет встроенного способа центрировать `title` (это даёт только `MaterialToolbar` из библиотеки Material Components, которую проект не использует). Решение — не пользоваться `setTitle()` вовсе, а добавить внутрь `Toolbar` собственный `TextView` с `android:layout_gravity="center"` (`Toolbar` — обычный `ViewGroup`, допускающий произвольные дочерние View). При этом нужно явно отключить встроенный заголовок: `getSupportActionBar().setDisplayShowTitleEnabled(false)` — иначе слева по-прежнему будет виден дефолтный `title`, автоматически подставляемый `AppCompat` из `android:label` в манифесте.

## Итог этапа

Edit и Delete реализованы поверх единой двухрежимной формы. По пути пойман и исправлен полноценный "букет" типичных для Android-разработки багов — от edge-to-edge вёрстки до перехвата кликов дочерними View — каждый с понятным механизмом, а не просто "заработало после правки". Всё протестировано и подтверждено рабочим на реальном устройстве.
