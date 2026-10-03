# Этап 7. Локализация (русский/английский + реакция на смену системного языка)

## Что требовалось по заданию

- Поддержка минимум двух языков интерфейса.
- Приложение должно реагировать на смену **системного** языка устройства — то есть пользователь не выбирает язык внутри самого приложения (это отдельная, более сложная техника — см. раздел "Альтернативы" в конце), а меняет язык в системных настройках телефона, и приложение подхватывает это само.

## Как Android вообще понимает, на каком языке показывать текст

### Сравнение с тем, как это делалось бы в C

В C/C++ ты, скорее всего, делал бы что-то вроде:

```c
#ifdef LANG_RU
    #define MSG_SAVE "Сохранить"
#else
    #define MSG_SAVE "Save"
#endif
```

Или рантайм-вариант: один массив строк с индексом по языку, который ты сам выбираешь при старте программы и сам подставляешь везде, где нужен текст. В обоих случаях **ты** пишешь логику выбора.

Android устроен принципиально иначе: один и тот же `.apk` содержит ресурсы **для всех языков сразу**, разложенные по папкам с суффиксами-квалификаторами (ты уже видел этот механизм на `values-night` для тёмной темы — здесь то же самое, только квалификатор не "тип интерфейса", а "язык"):

```
res/
├── values/
│   └── strings.xml       ← ресурсы по умолчанию (fallback)
└── values-ru/
    └── strings.xml       ← ресурсы для локали "ru" (русский)
```

Систему, которая решает, какую папку открывать, называют **resource resolution** — и работает она не на этапе компиляции, а **на этапе запуска/выполнения**, каждый раз, когда код делает `getString(R.string.xxx)` или когда layout-инфлейтер встречает `@string/xxx`. Класс `Resources` смотрит на текущий `Configuration.locale` устройства и ищет наиболее подходящую папку. Если под текущий язык папки нет — откатывается (fallback) на `values/` без суффикса.

Ты в коде **никогда** не пишешь `if (язык == "ru")`. Вся логика выбора зашита в самой ОС.

### Наш случай конкретно

Дефолтный `values/strings.xml` я сделала **английским** — так как ты сам в самом начале говорил, что хочешь дефолтные названия на английском, а не на русском. Русский вынесен в `values-ru/strings.xml`.

**`res/values/strings.xml`** (используется, если системный язык устройства — не русский; то есть это одновременно и "английская версия", и "аварийный fallback на все остальные языки"):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <string name="app_name">ft_hangouts</string>

    <string name="menu_change_color">Header color</string>
    <string name="dialog_delete_title">Delete contact</string>
    <string name="dialog_delete_message">Delete %1$s %2$s?</string>
    <string name="dialog_delete_confirm">Delete</string>
    <string name="dialog_delete_cancel">Cancel</string>
    <string name="dialog_color_title">Header color</string>

    <string-array name="toolbar_color_names">
        <item>Peach (default)</item>
        <item>Blue</item>
        <item>Green</item>
        <item>Purple</item>
        <item>Red</item>
    </string-array>

    <string name="title_new_contact">New contact</string>
    <string name="title_edit_contact">Edit contact</string>

    <string name="hint_name">First name</string>
    <string name="hint_surname">Last name</string>
    <string name="hint_phone">Phone</string>
    <string name="hint_email">Email</string>
    <string name="hint_birthday">Date of birth (YYYY-MM-DD)</string>
    <string name="btn_save">Save</string>

    <string name="error_name_phone_required">Name and phone are required</string>

    <string name="toast_background_info">Backgrounded at %1$s (%2$d min %3$d sec ago)</string>

</resources>
```

**`res/values-ru/strings.xml`** (используется, если системный язык устройства — русский):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <string name="app_name">ft_hangouts</string>

    <string name="menu_change_color">Цвет шапки</string>
    <string name="dialog_delete_title">Удалить контакт</string>
    <string name="dialog_delete_message">Удалить %1$s %2$s?</string>
    <string name="dialog_delete_confirm">Удалить</string>
    <string name="dialog_delete_cancel">Отмена</string>
    <string name="dialog_color_title">Цвет шапки</string>

    <string-array name="toolbar_color_names">
        <item>Персиковый (по умолчанию)</item>
        <item>Синий</item>
        <item>Зелёный</item>
        <item>Фиолетовый</item>
        <item>Красный</item>
    </string-array>

    <string name="title_new_contact">Новый контакт</string>
    <string name="title_edit_contact">Редактировать контакт</string>

    <string name="hint_name">Имя</string>
    <string name="hint_surname">Фамилия</string>
    <string name="hint_phone">Телефон</string>
    <string name="hint_email">Email</string>
    <string name="hint_birthday">Дата рождения (ГГГГ-ММ-ДД)</string>
    <string name="btn_save">Сохранить</string>

    <string name="error_name_phone_required">Имя и телефон обязательны</string>

    <string name="toast_background_info">Свёрнуто в %1$s (%2$d мин %3$d сек назад)</string>

</resources>
```

Папка `values-ru/` создаётся через **New → Android Resource Directory**, Resource type `values`, квалификатор `Locale` → Language `ru: Russian` — Android Studio сама подставляет суффикс `-ru` в имя папки. По сути это соглашение об именовании: `values` + дефис + ISO 639-1 код языка (`ru`, `en`, `fr`...). Диалог в IDE просто страхует от опечатки в коде языка — физически это обычная директория, которую можно было бы создать и вручную через `mkdir`.

### Позиционные плейсхолдеры `%1$s`, `%2$d`

Строки `dialog_delete_message` и `toast_background_info` — не статичные, в них подставляются динамические значения (имя/фамилия контакта; таймстемп и разница в минутах/секундах). Формат `%1$s`, `%2$d` — аналог `printf`, но с обязательным указанием **номера позиции** аргумента (`1$`, `2$`, `3$`). Это требование Android: разные языки могут требовать разного порядка слов в предложении, и если переводчик в `values-ru` переставит местами `%1$s` и `%2$s` в самой строке, подстановка всё равно отработает верно, потому что порядок задаётся номерами, а не порядком следования в шаблоне.

Используется так:

```java
getString(R.string.dialog_delete_message, contact.getName(), contact.getSurname());
// "Delete %1$s %2$s?" → "Delete John Smith?"

getString(R.string.toast_background_info, formattedTimestamp, minutes, seconds);
// "Backgrounded at %1$s (%2$d min %3$d sec ago)"
```

## Перенос строк из XML в ресурсы

### `activity_add_edit_contact.xml`

Было жёстко зашито `android:hint="Имя"` и т.д. — заменено на ссылки:

```xml
<EditText
    android:id="@+id/nameEditText"
    ...
    android:hint="@string/hint_name"
    android:inputType="textPersonName"
    android:autofillHints="name" />

<EditText
    android:id="@+id/surnameEditText"
    ...
    android:hint="@string/hint_surname"
    android:inputType="textPersonName" />

<EditText
    android:id="@+id/phoneEditText"
    ...
    android:hint="@string/hint_phone"
    android:inputType="phone"
    android:autofillHints="phone" />

<EditText
    android:id="@+id/emailEditText"
    ...
    android:hint="@string/hint_email"
    android:inputType="textEmailAddress"
    android:autofillHints="emailAddress" />

<EditText
    android:id="@+id/birthdayEditText"
    android:focusable="false"
    ...
    android:hint="@string/hint_birthday"
    android:inputType="date" />

<Button
    android:id="@+id/saveButton"
    ...
    android:text="@string/btn_save"
    android:backgroundTint="@color/primary" />
```

### `res/menu/main_menu.xml`

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_change_color"
        android:title="@string/menu_change_color"
        app:showAsAction="never" />
</menu>
```

## Перенос строк из Java

### `AddEditContactActivity.java`

```java
if (isEditMode) {
    toolbarTitle.setText(R.string.title_edit_contact);
    loadContact();
} else {
    toolbarTitle.setText(R.string.title_new_contact);
}
```

```java
if (name.isEmpty() || phone.isEmpty()) {
    Toast.makeText(this, R.string.error_name_phone_required, Toast.LENGTH_SHORT).show();
    return;
}
```

Важная деталь: `TextView.setText(int)` и `Toast.makeText(Context, int, int)` умеют принимать **id строкового ресурса напрямую**, без обёртки в `getString()`. Это работает за счёт перегрузки методов (`setText(CharSequence)` vs `setText(@StringRes int resId)`) — Java выбирает нужную версию метода по типу переданного аргумента на этапе компиляции, аналогично перегрузке функций в C++. А вот `AlertDialog.Builder.setMessage(...)` принимает только `CharSequence` — поэтому там, где нужна подстановка `%1$s`/`%2$s`, без `getString(id, args...)` не обойтись (см. ниже).

### `MainActivity.java` — диалог удаления

```java
contactListView.setOnItemLongClickListener((parent, view, position, id) -> {
    Contact contact = contactAdapter.getItem(position);
    new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, contact.getName(), contact.getSurname()))
            .setPositiveButton(R.string.dialog_delete_confirm, (dialog, which) -> {
                contactDao.deleteContact(contact.getId());
                refreshContactList();
            })
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show();
    return true;
});
```

### `MainActivity.java` — диалог выбора цвета

```java
private void showColorPickerDialog() {
    String[] colorNames = getResources().getStringArray(R.array.toolbar_color_names);
    int[] colorValues = {
            getColor(R.color.primary),
            Color.parseColor("#4C6EF5"),
            Color.parseColor("#40C057"),
            Color.parseColor("#7950F2"),
            Color.parseColor("#FA5252")
    };

    new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_color_title)
            .setItems(colorNames, (dialog, which) -> {
                int chosenColor = colorValues[which];
                toolbar.setBackgroundColor(chosenColor);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putInt(KEY_TOOLBAR_COLOR, chosenColor).apply();
            })
            .show();
}
```

Здесь массив названий цветов (`String[] colorNames`) теперь **не Java-литерал**, а ресурс `<string-array>`, прочитанный через `getResources().getStringArray(...)`. Критично важно: порядок элементов внутри `<string-array>` в `values/strings.xml` и в `values-ru/strings.xml` должен **строго совпадать** с порядком в Java-массиве `colorValues[]`. Связь между названием и цветом идёт только по **индексу** (`which` — позиция выбранного пункта в списке), а не по тексту. Если бы при переводе кто-то переставил, скажем, "Синий" и "Зелёный" местами в `values-ru`, а `values` (английский) оставил как было — на русском в списке было бы написано "Синий", а применялся бы зелёный цвет. Один из классических подводных камней локализации массивов — текст живёт отдельно от логики, и ничего не мешает им разъехаться, если не следить руками.

### `HangoutsApp.java` — Toast с двумя подставляемыми значениями

```java
String formattedTimestamp = new SimpleDateFormat(
        "HH:mm:ss dd.MM.yyyy", Locale.getDefault())
        .format(new Date(lastBackground));

String message = getString(
        R.string.toast_background_info,
        formattedTimestamp, minutes, seconds
);

Toast.makeText(HangoutsApp.this, message, Toast.LENGTH_LONG).show();
```

`getString(int id, Object... formatArgs)` — вариативный метод (как `printf` с произвольным числом аргументов в C), который сам подставит `%1$s` → строку, `%2$d`/`%3$d` → числа. Количество и порядок аргументов в вызове обязаны совпадать с номерами в самом шаблоне строки.

## Translations Editor — GUI-альтернатива ручной правке XML

В Android Studio есть встроенный инструмент: правый клик на любом `strings.xml` → **Open Translations Editor**. Он показывает таблицу "ключ × язык" вместо двух раздельных XML-файлов и подсвечивает ключи без перевода на какой-то из языков ("missing translation") — удобно для финального аудита перед сдачей, чтобы не забыть ни одной строки. Минус: нормально не работает с `<string-array>` — массив цветов всё равно проще и надёжнее редактировать напрямую в XML, как показано выше. Под капотом Translations Editor пишет ровно в те же файлы `values/strings.xml` / `values-ru/strings.xml` — никакого отдельного хранилища переводов нет, это просто другой способ смотреть на одни и те же данные.

## Почему приложение реагирует на смену языка "само", без единой строчки нашего кода

Когда пользователь меняет язык в системных настройках и возвращается в приложение, Android по умолчанию **уничтожает и заново создаёт текущую Activity** (вызывается `onDestroy()` → `onCreate()` заново) — потому что новая `Configuration` (включающая локаль) считается достаточно значимым изменением, чтобы пересобрать экран с нуля. При повторном `onCreate()` все layout-файлы инфлейтятся заново, и каждая ссылка `@string/...` резолвится уже под новую локаль.

Это поведение **отключается**, если в `AndroidManifest.xml` у Activity стоит атрибут `android:configChanges` со значением, включающим `locale` (например, `android:configChanges="locale|uiMode"`) — тогда система решает, что разработчик берёт смену языка на себя, не пересоздаёт Activity, а просто дёргает `onConfigurationChanged()`, в котором пришлось бы вручную вызывать `recreate()` или обновлять тексты. Мы это проверили — такого атрибута в манифесте нет ни у одной `Activity`, значит, всё работает "бесплатно" через стандартный пересоздание экрана.

## Проверка

1. Собрали и запустили — системный язык был английским, приложение полностью на английском: заголовки экранов, хинты полей, кнопка "Save", меню "Header color", диалоги, Toast.
2. Настройки телефона → Система → Языки → сделали первым языком русский.
3. Вернулись в приложение — всё переключилось на русский без перезапуска вручную: главный экран, экран добавления/редактирования, меню смены цвета, диалог удаления, Toast о сворачивании.
4. Проверили оба направления переключения (en→ru и ru→en) и то, что массив цветов не "съехал" — выбор "Синий" из списка всегда красит шапку в синий, независимо от текущего языка.

Всё подтверждено рабочим.

## Альтернативы, которые тоже стоило упомянуть

- **`AppCompatDelegate.setApplicationLocales(...)`** (AndroidX, API 33+ с graceful fallback на более старые версии через библиотеку) — позволяет **выбирать язык внутри самого приложения**, независимо от системного языка устройства (свитчер "язык приложения" в настройках самого приложения, как это сделано во многих крупных apps). Это другой сценарий, чем у нас — в задании явно требуется реакция на **системный** язык, а не собственный переключатель, так что эту технику не применяли, но для общего кругозора полезно знать, что она существует и что это AndroidX-API, а не чистый SDK.
- **Ручное пересоздание контекста через `Context.createConfigurationContext(Configuration)`** — более низкоуровневый способ временно "подменить" локаль для конкретного `Context` без выбора языка на уровне всей системы. Используется, когда нужно, например, отрисовать текст на двух языках одновременно на одном экране — у нас такой необходимости нет.
- Жёстко прибить `android:configChanges="locale"` и писать свою логику обновления текстов в `onConfigurationChanged()` — сознательно не стали делать, поскольку это усложнение без выгоды в нашем случае: стандартное пересоздание Activity работает корректно и не теряет введённые пользователем данные благодаря `onSaveInstanceState`/`onRestoreInstanceState`, которые Android вызывает автоматически при пересоздании.
