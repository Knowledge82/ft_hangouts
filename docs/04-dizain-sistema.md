# Этап 4. Дизайн-система: цвета, тема, современный вид без внешних библиотек

## Задача

Сделать интерфейс визуально современным (скруглённые карточки, тени, ripple-эффект, светлая/тёмная тема), не нарушая условие задания "no external libraries including for UI design".

## Обнаруженная проблема: скрытые зависимости от шаблона

При создании проекта через "Empty Views Activity" Android Studio **сама** прописала в `app/build.gradle.kts`:
```kotlin
implementation(libs.material)
implementation(libs.constraintlayout)
```
и сделала базовую тему приложения наследницей `Theme.Material3.DayNight.NoActionBar` — то есть формально зависящей от Material Components, хотя в написанном вручную коде (layout-файлы, `ContactAdapter`) использовались только голые `LinearLayout`/`ListView`/`TextView`.

Раз ранее было принято решение работать исключительно на голом Android SDK — эти зависимости убраны:

**`app/build.gradle.kts`** — удалены строки `implementation(libs.material)` и `implementation(libs.constraintlayout)`, оставлены только `appcompat` и `activity-ktx` (без них не работает `AppCompatActivity`, от которого наследуется `MainActivity`).

**`res/values/themes.xml` и `res/values-night/themes.xml`** — родительская тема изменена с `Theme.Material3.DayNight.NoActionBar` на `Theme.AppCompat.DayNight.NoActionBar`. `AppCompat` — более базовая и старая библиотека AndroidX, уже неизбежная в проекте (без неё не собрать `AppCompatActivity`), в отличие от Material Components, который является отдельным, не обязательным для работы кодом. `DayNight` в названии темы — часть, обеспечивающая автоматическое переключение между светлым/тёмным вариантом; она реализована уже на уровне AppCompat, а не только в Material3.

## Механизм День/Ночь — `values` / `values-night`

Android поддерживает **альтернативные наборы ресурсов**, автоматически выбираемые системой по разным условиям (язык, ориентация экрана и т.д.) — в их числе и системная тема оформления. Если создать папку `res/values-night/` с файлом **того же имени**, что и в `res/values/`, и **теми же именами** ресурсов внутри — Android сам подставит нужный набор значений в зависимости от того, включена ли у пользователя тёмная тема.

Папка создаётся через **New → Android Resource Directory** (не через обычный "Directory" — тогда система не распознает её как набор ресурсов под конкретное условие): Resource type `values`, в Available Qualifiers выбрать **Night Mode** → значение **Night**, итоговое имя папки — `values-night`.

Поскольку весь layout-код ссылается на цвета по имени (`@color/background`, `@color/surface`...), а не хардкодит значения — переключение темы происходит **без единой правки layout-файлов**: меняются только сами значения в `colors.xml`.

### Светлая тема (персиковая) — `res/values/colors.xml`

```xml
<resources>
    <color name="primary">#FF8A5C</color>
    <color name="primary_dark">#E56A3D</color>
    <color name="accent">#FFB088</color>

    <color name="background">#FDEEE4</color>
    <color name="surface">#FFF8F0</color>

    <color name="text_primary">#2B211C</color>
    <color name="text_secondary">#8A776B</color>
</resources>
```

### Тёмная тема (изумрудная) — `res/values-night/colors.xml`

```xml
<resources>
    <color name="primary">#34D399</color>
    <color name="primary_dark">#1FA97A</color>
    <color name="accent">#6EE7B7</color>

    <color name="background">#0F1C17</color>
    <color name="surface">#182922</color>

    <color name="text_primary">#EDEFEE</color>
    <color name="text_secondary">#9CB3A8</color>
</resources>
```

Принцип подбора цветов в обеих палитрах: `surface` (цвет карточек) сознательно не белый/не чёрный, а с тёплым/зелёным оттенком (`#FFF8F0` вместо `#FFFFFF`, `#182922` вместо `#000000`), и `text_primary` — не чистый чёрный/белый, а слегка приглушённый — крайности выглядят более "стерильно" и менее приятно для глаз.

## Тема приложения — привязка цветов к системным атрибутам

Оба `themes.xml` (light и night) содержат **одинаковые** ссылки на имена цветов — различие только в значениях, определённых в соответствующих `colors.xml`:

```xml
<style name="Base.Theme.Ft_hangouts" parent="Theme.AppCompat.DayNight.NoActionBar">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryDark">@color/primary_dark</item>
    <item name="colorAccent">@color/accent</item>
    <item name="android:windowBackground">@color/background</item>
    <item name="android:statusBarColor">@color/primary_dark</item>
</style>
```

- **`colorPrimary`** — основной акцентный цвет темы, используется системными/AppCompat-виджетами по умолчанию.
- **`colorPrimaryDark`** — исторически цвет статус-бара; используется здесь согласованно с `statusBarColor`.
- **`colorAccent`** — вторичный акцент (курсор текстового поля, мелкие элементы выделения).
- **`android:windowBackground`** — фон самого окна Activity, применяется до отрисовки layout — предотвращает вспышку белым цветом при запуске экрана.
- **`android:statusBarColor`** — цвет статус-бара (доступен с API 21, `minSdk` проекта — 24).

**Известное ограничение:** на новых версиях Android (14–15) система переходит на **edge-to-edge** по умолчанию, при котором `android:statusBarColor` может игнорироваться, а статус-бар остаётся прозрачным независимо от темы. Это изменение поведения самой ОС, не связанное с нашим кодом. Поскольку статус-бар не входит в список требований задания — решено не тратить время на полноценную edge-to-edge адаптацию (обработку `WindowInsets`) на этом этапе и вернуться к этому вопросу только при наличии времени в конце.

## Карточка контакта — скруглённые углы и тень

### `res/drawable/bg_card.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface" />
    <corners android:radius="16dp" />
</shape>
```

`<shape>` — drawable-ресурс, описывающий геометрическую фигуру программно, без растрового изображения; часть `android.graphics.drawable`, не библиотека. `<corners android:radius="16dp">` даёт скруглённые углы вместо прямоугольных.

Применяется в `contact_list_item.xml` через `android:background="@drawable/bg_card"`, вместе с `android:elevation="2dp"` (системная тень, доступна с API 21) — что и создаёт визуальный эффект "приподнятой карточки".

## Ripple-эффект, обрезанный по форме карточки

### Проблема

Изначально использовался системный `android:foreground="?attr/selectableItemBackground"` — стандартный прямоугольный ripple. Он не учитывает форму фона под собой: при нажатии волна эффекта выходила прямоугольником за пределы скруглённых углов карточки, что выглядело неаккуратно.

### Решение — `res/drawable/bg_card_ripple.xml`

```xml
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?android:attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <solid android:color="@android:color/white" />
            <corners android:radius="16dp" />
        </shape>
    </item>
</ripple>
```

`<ripple>` — `RippleDrawable`, часть core SDK с API 21, никакой сторонней библиотеки. `android:color="?android:attr/colorControlHighlight"` — цвет ripple берётся из системной темы для консистентности.

**Ключевая деталь — `android:id="@android:id/mask"`.** Это зарезервированный системный идентификатор, специально распознаваемый `RippleDrawable`: слой с этим id используется не для отображения, а как **маска**, определяющая форму, в границах которой рисуется ripple. Задав маске ту же скруглённую форму (`corners android:radius="16dp"`), что и у `bg_card.xml`, ripple теперь аккуратно обрезается по контуру карточки. Конкретный цвет заливки маски (`@android:color/white`) не имеет значения — важна только геометрия, маска никогда не отображается как таковая.

В `contact_list_item.xml` заменено:
```xml
android:foreground="@drawable/bg_card_ripple"
```
(было `?attr/selectableItemBackground`).

## Зазор между карточками — известная особенность `ListView`

Изначальная попытка задать `layout_margin` на корневом элементе `contact_list_item.xml` не сработала — `ListView` не всегда учитывает margin, заданный на корне layout строки, при преобразовании его в свои внутренние `LayoutParams` (давно известная особенность именно этого виджета, не воспроизводящаяся в `RecyclerView`).

**Правильное решение** — через встроенный механизм `divider`/`dividerHeight` самого `ListView`:

```xml
<ListView
    android:id="@+id/contactListView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:divider="@android:color/transparent"
    android:dividerHeight="12dp"
    android:clipToPadding="false"
    android:paddingHorizontal="12dp"
    android:paddingVertical="8dp" />
```

- **`divider` = прозрачный цвет** (не `@null`) — сохраняет сам механизм разделителя (и его толщину), просто делает его невидимым.
- **`dividerHeight="12dp"`** — толщина невидимого разделителя, визуально воспринимаемая как зазор между карточками.
- **`paddingHorizontal`/`paddingVertical` на самом `ListView`** — отступ всего списка от краёв экрана (вместо ненадёжного margin на отдельной строке).
- **`clipToPadding="false"`** — без этого атрибута список обрезал бы контент точно по границе padding при прокрутке, и крайние карточки были бы наполовину срезаны в начале/конце списка.

## Итог этапа

Приложение полностью очищено от скрытых зависимостей на Material Components и ConstraintLayout — используется исключительно голый Android SDK. Реализованы: кастомная светлая/тёмная палитра с автоматическим переключением по системной настройке, скруглённые карточки с тенью, ripple-эффект, корректно обрезанный по форме карточки, и надёжный зазор между элементами списка. Визуальный результат проверен на реальном устройстве в обоих режимах темы.
