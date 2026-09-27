# Этап 6. Иконка приложения, меню смены цвета шапки, Toast о сворачивании

## Задача

Закрыть сразу три оставшихся обязательных требования: собственная иконка приложения (лого 42), пункт меню, меняющий цвет шапки, и Toast, показывающий момент и длительность последнего сворачивания приложения. По пути пришлось на ходу переработать часть UI (кнопка "Добавить" стала отдельной круглой кнопкой) и разобраться в двух новых механизмах — `SharedPreferences` и `Application.ActivityLifecycleCallbacks`.

## Часть 1 — Иконка приложения

### Adaptive icon — что это и зачем

С Android 8.0 (API 26) иконка приложения — не единая плоская картинка, а **adaptive icon**, состоящая из двух слоёв:
- **Foreground** — сам рисунок (у нас — лого 42);
- **Background** — подложка под ним (у нас — сплошной цвет `primary`).

Система сама комбинирует эти два слоя и обрезает результат под разную форму в зависимости от прошивки телефона (круг, квадрат со скруглением, "капля") — так все иконки на одном устройстве выглядят единообразно, даже если разработчики рисовали их независимо. До этой системы (Android 7 и старше) иконка была одной цельной картинкой без всякой автоматической обрезки, из-за чего приложения одной прошивки могли визуально не сочетаться друг с другом.

### Инструмент — Image Asset Studio

New (правый клик на `app` или `res`) → **Image Asset** → **Icon Type: Launcher Icons (Adaptive and Legacy)**:
- **Foreground Layer** — файл с логотипом 42;
- **Background Layer** — однотонный цвет (взят `primary` из нашей палитры — так иконка перекликается со стилем самого приложения);
- **Name** — имя итогового ресурса. Мастер создаёт набор файлов под этим именем во всех папках `mipmap-*` (под разные плотности экрана) плюс `mipmap-anydpi-v26/<имя>.xml` — современный XML-формат adaptive icon.

### Пойманная ошибка — забытое переименование в манифесте

**Важная деталь, в которую упёрлись на практике**: если в поле **Name** мастера ввести своё имя (не дефолтное `ic_launcher`) — мастер создаёт **новый, отдельный** набор ресурсов, но **не трогает** `AndroidManifest.xml`. А там как была ссылка на старую иконку:
```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```
так и осталась — то есть создались два параллельных набора файлов (`ic_launcher.xml`/`ic_launcher_round.xml` — старые, робот Android; `ft_hangouts_logo.xml`/`ft_hangouts_logo_round.xml` — новые, лого 42), а манифест продолжал ссылаться на старый. Отсюда — иконка и splash-экран продолжали показывать робота даже после успешного прогона мастера.

**Как искать реальное имя ресурса**, если забыла, что вводила: переключить панель **Project** с вида **Android** (группирует ресурсы по типу, скрывая структуру папок) на вид **Project** (показывает реальные файлы на диске) и заглянуть в `res/mipmap-anydpi-v26/` — там лежит искомый XML-файл, его имя без расширения и есть имя ресурса.

**Исправление** — привести оба атрибута в манифесте в соответствие с реальным именем:
```xml
android:icon="@mipmap/ft_hangouts_logo"
android:roundIcon="@mipmap/ft_hangouts_logo_round"
```
Старые неиспользуемые `ic_launcher*` файлы удалены из всех `mipmap-*` папок — иначе Lint рано или поздно подсветил бы их как unused resources.

### Splash-экран и кеш иконок

Начиная с Android 12 (API 31), система **сама** показывает системный splash screen при запуске любого приложения, автоматически беря иконку из той же `@mipmap/...` ссылки в манифесте — никакого отдельного кода для этого писать не нужно. Если после исправления манифеста иконка/splash всё ещё не обновляются — помогает полное удаление приложения с устройства (не просто пересборка поверх старой версии) — лаунчеры и система агрессивно кешируют иконки, и обычный "Run" поверх старой установки иногда не форсирует обновление этого кеша.

### Где физически смотреть результат

Установка через кнопку **Run** в Android Studio — это полноценная установка `.apk` на устройство, а не временный предпросмотр. Приложение реально лежит среди остальных установленных: иконку можно увидеть на рабочем столе, в общем списке приложений, в списке "последних приложений" и в Настройки → Приложения. Единственное отличие от версии из Google Play — подпись debug-сертификатом вместо боевого, на работу это никак не влияет.

## Часть 2 — Меню смены цвета шапки + переработка UI

### Что именно требует задание

Формулировка — "смена цвета шапки **через меню**" — это прямая отсылка к системному **Options Menu** (`onCreateOptionsMenu`/`onOptionsItemSelected`), уже реализованному на этапе добавления контакта. Пункт "Цвет шапки" обязан лежать именно там, а не быть, скажем, отдельной кнопкой на экране.

### Проблема — стандартный вид меню выглядел неряшливо

Дефолтное overflow-меню (три точки) — острые прямоугольные углы, никак не связанные визуально с палитрой приложения. К тому же единственный пункт меню "Добавить" в overflow плюс единственный пункт "Цвет шапки" — избыточно, раз на экране и так есть кнопка добавления в самом Toolbar.

### Решение — переосмысление по значимости действия

Взят стандартный UX-принцип (используемый в том числе и в самом Material Design, независимо от того, используем мы его библиотеку или нет): **частое** действие получает крупную, заметную, самостоятельную кнопку; **редкое** — уходит в скромное меню. У нас: "Добавить контакт" — частое действие → отдельная кнопка. "Цвет шапки" — разовая настройка → остаётся в overflow-меню, но становится визуально аккуратнее.

### Круглая кнопка "Добавить" — свой аналог FAB без библиотеки Material

Готовый `FloatingActionButton` — часть библиотеки Material Components, которую проект принципиально не использует. Сделан визуальный аналог вручную теми же средствами, что и карточки контактов — `<shape>` + `<ripple>` с маской.

**`res/drawable/bg_fab.xml`**:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/primary" />
</shape>
```

**`res/drawable/bg_fab_ripple.xml`**:
```xml
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?android:attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <shape android:shape="oval">
            <solid android:color="@android:color/white" />
        </shape>
    </item>
</ripple>
```
Тот же принцип маски, что и в `bg_card_ripple.xml`, только форма `oval` вместо скруглённого прямоугольника.

**`activity_main.xml`** — `ListView` обёрнут в `FrameLayout`, чтобы наложить кнопку поверх списка в правом нижнем углу:
```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ListView
        android:id="@+id/contactListView"
        ... />

    <TextView
        android:id="@+id/addContactButton"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_gravity="bottom|end"
        android:layout_margin="24dp"
        android:background="@drawable/bg_fab"
        android:foreground="@drawable/bg_fab_ripple"
        android:text="+"
        android:textSize="30sp"
        android:textStyle="bold"
        android:textColor="@android:color/white"
        android:gravity="center"
        android:elevation="6dp"
        android:clickable="true"
        android:focusable="true" />

</FrameLayout>
```

**Важное отличие от карточек контактов**: здесь `android:clickable="true"`/`android:focusable="true"` — **не ошибка**, в отличие от истории с `contact_list_item.xml`. Там эти атрибуты перехватывали клик у `ListView.OnItemClickListener`, потому что строка была **дочерним** элементом списка с общим механизмом диспетчеризации кликов. Кнопка `addContactButton` — самостоятельный элемент вне `ListView`, у неё свой собственный `OnClickListener`, конфликтовать не с чем.

### Кнопка залезала под системную навигацию

Та же edge-to-edge история, что и с Toolbar сверху, только теперь снизу — кнопка с фиксированным `layout_margin="24dp"` не учитывала системную область снизу экрана (панель навигации/жестов). Решение — тот же `WindowInsets`, но применяем к **margin**, а не к `padding`/высоте (кнопка маленькая, ей не нужно "растягиваться"):

```java
final int baseMarginPx = (int) (24 * getResources().getDisplayMetrics().density);

addContactButton.setOnApplyWindowInsetsListener((v, insets) -> {
    int navBarInset = insets.getSystemWindowInsetBottom();

    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
    params.bottomMargin = baseMarginPx + navBarInset;
    v.setLayoutParams(params);

    return insets;
});
```

`24 * getResources().getDisplayMetrics().density` — ручной перевод `24dp` в пиксели (то же преобразование, которое Android делает автоматически при чтении `layout_margin="24dp"` из XML, но нужно повторить в коде, раз margin меняется программно). Базовое значение посчитано **один раз** и хранится как константа — если бы вместо этого каждый раз читался уже изменённый margin из `getLayoutParams()`, при повторных вызовах слушателя (например, при повороте экрана) отступ рос бы бесконтрольно, накопительно.

`ViewGroup.MarginLayoutParams` — базовый класс, в котором объявлены поля `leftMargin`/`topMargin`/`rightMargin`/`bottomMargin`; конкретный `FrameLayout.LayoutParams` нашей кнопки наследуется от него, но `getLayoutParams()` по умолчанию возвращает более общий тип без этих полей — отсюда приведение типа.

### Скруглённое overflow-меню — `app:popupTheme`

**`res/values/styles.xml`**:
```xml
<resources>
    <style name="ToolbarPopupOverlay" parent="ThemeOverlay.AppCompat.Light">
        <item name="android:popupBackground">@drawable/bg_popup_menu</item>
    </style>
</resources>
```

**`res/drawable/bg_popup_menu.xml`**:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface" />
    <corners android:radius="12dp" />
</shape>
```

В `activity_main.xml`, на самом `Toolbar`: `app:popupTheme="@style/ToolbarPopupOverlay"`. `popupTheme` — атрибут `Toolbar` из AppCompat (не Material), задающий отдельную тему специально для всплывающих меню этого конкретного `Toolbar`, не затрагивая тему всего приложения. Внутри переопределён только `android:popupBackground` — на собственный скруглённый `shape` в цвете `surface`, том же, что у карточек — визуальная консистентность со всем остальным приложением.

**`main_menu.xml`** после переработки — остался один пункт:
```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_change_color"
        android:title="Цвет шапки"
        app:showAsAction="never" />
</menu>
```

### `SharedPreferences` — хранение выбранного цвета между запусками

```java
private static final String PREFS_NAME = "ft_hangouts_prefs";
private static final String KEY_TOOLBAR_COLOR = "toolbar_color";

// применение сохранённого цвета при старте
SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
int savedColor = prefs.getInt(KEY_TOOLBAR_COLOR, getColor(R.color.primary));
toolbar.setBackgroundColor(savedColor);
```

```java
private void showColorPickerDialog() {
    String[] colorNames = {"Персиковый (по умолчанию)", "Синий", "Зелёный", "Фиолетовый", "Красный"};
    int[] colorValues = {
            getColor(R.color.primary),
            Color.parseColor("#4C6EF5"),
            Color.parseColor("#40C057"),
            Color.parseColor("#7950F2"),
            Color.parseColor("#FA5252")
    };

    new AlertDialog.Builder(this)
            .setTitle("Цвет шапки")
            .setItems(colorNames, (dialog, which) -> {
                int chosenColor = colorValues[which];
                toolbar.setBackgroundColor(chosenColor);

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putInt(KEY_TOOLBAR_COLOR, chosenColor).apply();
            })
            .show();
}
```

**`SharedPreferences`** — встроенный механизм хранения мелких пар "ключ-значение" (чисел, строк, булевых флагов), сохраняющихся между запусками приложения; физически — XML-файл в приватной папке приложения на устройстве. Принципиально другой инструмент, чем SQLite: база — для структурированных связанных данных (контакты, сообщения), `SharedPreferences` — для разрозненных настроек уровня "приложение помнит одну вещь". Использовать SQLite ради одного цвета было бы избыточно.

`getSharedPreferences(PREFS_NAME, MODE_PRIVATE)` создаёт (или открывает существующий) файл настроек; `MODE_PRIVATE` — единственный сегодня актуальный режим доступа (файл виден только этому приложению; менее строгие режимы были признаны небезопасными и удалены из SDK).

`prefs.edit().putInt(...).apply()` — тот же паттерн Builder, что и `AlertDialog.Builder`: `edit()` возвращает `Editor`, `putInt` кладёт значение, `.apply()` асинхронно сохраняет на диск (есть ещё синхронный `.commit()`, но `.apply()` предпочтителен почти всегда).

`getColor(R.color.primary)` в качестве значения по умолчанию для `prefs.getInt(...)` — если пользователь ещё ни разу не менял цвет, вернётся именно дефолтный персиковый, а не случайное число.

## Часть 3 — Toast о последнем сворачивании приложения

### Почему `onPause`/`onResume` отдельной Activity не подходят

Первая мысль — сохранять момент в `onPause()` `MainActivity` и показывать Toast в её `onResume()`. Но это даёт ложные срабатывания: при переходе из `MainActivity` в `AddEditContactActivity` у `MainActivity` тоже вызывается `onPause()` — хотя приложение не сворачивается, пользователь просто перешёл на другой **свой же** экран. Нужен способ отличить реальный уход в фон (на рабочий стол/в другое приложение) от внутренней навигации между своими Activity.

### `Application` и `ActivityLifecycleCallbacks`

`Application` — объект, создаваемый системой один раз за весь жизненный цикл процесса, ещё до первой Activity, и живущий, пока жив процесс — общий "глобальный контекст" всего приложения, подходящее место для состояния, общего для всех экранов сразу.

`registerActivityLifecycleCallbacks(...)` — метод `Application`, принимающий слушателя, вызываемого системой при **каждом** переходе жизненного цикла **любой** Activity во всём приложении, не только конкретной.

**`HangoutsApp.java`**:
```java
package com.fortytwo.ft_hangouts;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HangoutsApp extends Application {

    private static final String PREFS_NAME = "ft_hangouts_prefs";
    private static final String KEY_LAST_BACKGROUND = "last_background_timestamp";

    private int startedActivitiesCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivitiesCount++;

                if (startedActivitiesCount == 1) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    long lastBackground = prefs.getLong(KEY_LAST_BACKGROUND, -1);

                    if (lastBackground != -1) {
                        long elapsedMillis = System.currentTimeMillis() - lastBackground;
                        long minutes = (elapsedMillis / 1000) / 60;
                        long seconds = (elapsedMillis / 1000) % 60;

                        String formattedTimestamp = new SimpleDateFormat(
                                "HH:mm:ss dd.MM.yyyy", Locale.getDefault())
                                .format(new Date(lastBackground));

                        String message = "Свёрнуто в " + formattedTimestamp
                                + " (" + minutes + " мин " + seconds + " сек назад)";

                        Toast.makeText(HangoutsApp.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivitiesCount--;

                if (startedActivitiesCount == 0) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit()
                            .putLong(KEY_LAST_BACKGROUND, System.currentTimeMillis())
                            .apply();
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
```

Регистрация в `AndroidManifest.xml` — добавлен атрибут к тегу `<application>`:
```xml
<application
    android:name=".HangoutsApp"
    ...>
```

### Разбор — счётчик как способ отличить "своя навигация" от "реальный уход в фон"

**Ключевая идея — счётчик `startedActivitiesCount`.** При переходе между **своими** экранами (`MainActivity` → `AddEditContactActivity`) система сначала вызывает `onActivityStarted` у **новой** Activity (счётчик становится `2`, старая ещё не остановилась), и только потом `onActivityStopped` у старой (счётчик падает обратно до `1`) — счётчик **ни разу не касается нуля**, ложного срабатывания не происходит.

При реальном сворачивании (кнопка "Домой") останавливается текущая Activity, и никакая новая Activity **внутри нашего приложения** взамен не стартует — счётчик падает до `0`, именно в этот момент сохраняется таймстемп. При возврате в приложение счётчик поднимается с `0` до `1` — единственный момент, когда показывается Toast.

### Арифметика перевода миллисекунд в минуты и секунды

```java
long elapsedMillis = System.currentTimeMillis() - lastBackground;
long minutes = (elapsedMillis / 1000) / 60;
long seconds = (elapsedMillis / 1000) % 60;
```

`System.currentTimeMillis()` — текущее время в миллисекундах с 1 января 1970 ("эпоха Unix"), стандартный способ получить временную метку в Java; `lastBackground` сохранён той же функцией, обе величины в одной системе отсчёта, можно напрямую вычитать.

Дальше — обычная арифметика целочисленного деления и остатка, как в C: `(elapsedMillis / 1000) / 60` переводит миллисекунды сначала в целые секунды, затем в целые минуты (дробная часть отбрасывается — деление `long` на `long` в Java всегда целочисленное, без округления); `(elapsedMillis / 1000) % 60` берёт **остаток** от деления секунд на `60` — "секунды сверх целых минут" (125 секунд → `125 / 60 = 2` минуты, `125 % 60 = 5` секунд).

## Итог этапа

Все три требования закрыты: собственная иконка (лого 42, adaptive icon), меню смены цвета шапки (с сохранением выбора между запусками через `SharedPreferences`) и Toast с моментом и длительностью последнего сворачивания (через `Application.ActivityLifecycleCallbacks`, корректно отличающий реальный уход в фон от внутренней навигации между экранами). Заодно кнопка "Добавить" переехала из меню в собственную круглую кнопку — визуальный аналог FAB, полностью на голом SDK, без единой внешней зависимости.
