# Этап 3. Главный экран: список контактов

## Задача

Отобразить контакты из базы (`ContactDao.getAllContacts()`) на главном экране приложения — то самое требование задания "Homepage displaying a summary for each contact".

## Design-редактор vs Code-редактор в Android Studio

Прежде чем перейти к самим файлам — важная деталь про работу с XML-разметкой в IDE. У каждого layout-файла есть **два представления одного и того же файла**, переключаемые кнопками в правом верхнем углу редактора:

- **Design** — визуальный конструктор: панель **Palette** (готовые компоненты для перетаскивания мышкой) и **Component Tree** (иерархия элементов экрана в виде дерева). Открывается по умолчанию.
- **Code** — обычный текстовый XML, тот же файл, что и в Design-режиме, просто в текстовом виде.
- **Split** — оба вида рядом.

Оба режима работают с одним и тем же файлом на диске: правка в Design-режиме (перетащил компонент) генерирует XML-теги, правка в Code-режиме (вписал теги руками) сразу отражается в визуальном превью. Для работы с готовым кодом (когда правки пишутся руками, а не собираются мышкой) удобнее Code-режим.

## Файлы разметки

### `res/layout/activity_main.xml`

Корневой экран, растянутый на весь экран `ListView`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <ListView
        android:id="@+id/contactListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

`android:id="@+id/contactListView"` — присваивает элементу идентификатор; `@+id/...` (плюс перед `id`) означает "создать новый идентификатор в `R.id`, если его ещё нет". По этому идентификатору элемент будет найден из Java-кода через `findViewById`.

### `res/layout/contact_list_item.xml`

Разметка **одной строки** списка — отдельный layout-файл, применяемый к каждому контакту:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="12dp">

    <TextView
        android:id="@+id/contactNameTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/contactPhoneTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp" />

</LinearLayout>
```

**`wrap_content` по высоте** — строка занимает ровно столько места, сколько нужно её содержимому (в отличие от `match_parent`, который растянул бы одну строку на весь экран).

**`dp` (density-independent pixels)** — единица измерения для отступов и размеров, выглядящая одинаково на экранах с разной плотностью пикселей, в отличие от обычных `px` (которые физически меняют размер на экранах разного разрешения).

**`sp` (scale-independent pixels)** — аналог `dp`, но для размера текста, дополнительно учитывающий настройку размера шрифта, выбранную пользователем в системных настройках телефона.

## `ContactAdapter.java` — связующее звено между данными и списком

`ListView` не умеет самостоятельно превращать объекты `Contact` в строки на экране — для этого нужен **адаптер**.

```java
package com.fortytwo.ft_hangouts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {

    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.contact_list_item, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.contactNameTextView);
        TextView phoneView = convertView.findViewById(R.id.contactPhoneTextView);

        nameView.setText(contact.getName() + " " + contact.getSurname());
        phoneView.setText(contact.getPhone());

        return convertView;
    }
}
```

### Разбор

**`extends ArrayAdapter<Contact>`.** `ArrayAdapter` — готовый класс Android SDK, связывающий список объектов с отображением в `ListView`/`Spinner` и подобных. `<Contact>` — **generic**-параметр (обобщённый тип, аналог шаблонов C++), конкретизирующий, что этот адаптер работает именно со списком контактов.

**`super(context, 0, contacts);`** — второй параметр обычно указывает ID layout-ресурса для строки, но здесь передан `0`, потому что `getView()` переопределён вручную — стандартная разметка родителю не нужна.

**`getView(int position, View convertView, ViewGroup parent)`** — вызывается системой для каждой видимой строки списка. Параметры:
- `position` — индекс элемента в списке данных
- `convertView` — уже готовая, но более не видимая на экране строка, доступная для переиспользования
- `parent` — родительский `ViewGroup` (сам `ListView`)

**View recycling (`if (convertView == null)`).** Ключевая оптимизация: при списке из тысяч элементов, из которых на экране видно только 10, Android не создаёт тысячи объектов `View` — переиспользует те немногие, что уже существуют, при прокрутке (строка, ушедшая за край экрана, тут же используется повторно для новой строки на другом краю).

**`LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item, parent, false)`** — превращает XML-файл `contact_list_item.xml` в реальный объект `View` в памяти (аналог того, как `setContentView` превращает `activity_main.xml` в экран Activity). Третий параметр `false` означает "не присоединяй сразу к `parent`" — присоединением займётся сам `ListView`.

**`getItem(position)`** — унаследованный метод, возвращающий объект данных по индексу — обращается к списку, переданному в конструктор.

## `MainActivity.java`

```java
package com.fortytwo.ft_hangouts;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ContactDao contactDao;
    private ContactAdapter contactAdapter;
    private ListView contactListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactDao = new ContactDao(this);
        contactListView = findViewById(R.id.contactListView);

        List<Contact> contacts = contactDao.getAllContacts();
        contactAdapter = new ContactAdapter(this, contacts);
        contactListView.setAdapter(contactAdapter);
    }
}
```

Тестовый код с `Log.d`/ручным `addContact` из предыдущего этапа удалён — свою роль (проверка слоя данных) он выполнил.

`findViewById(R.id.contactListView)` находит `ListView` из `activity_main.xml`. `contactListView.setAdapter(contactAdapter)` подключает адаптер — после чего `ListView` сам вызывает `getView()` адаптера столько раз, сколько нужно для заполнения видимой области.

## Проверка

Запуск на реальном устройстве (Samsung по USB) показал список из тестовых контактов, накопившихся на этапе проверки БД (Иван Петров, Мария Сидорова и их дубликаты после теста персистентности) — ФИО жирным шрифтом, телефон обычным начертанием под ним. Подтверждает, что вся цепочка `ContactDao → ContactAdapter → ListView` работает корректно.

## Итог этапа

Главный экран отображает реальные данные из базы. Следующий шаг — добавление возможности **создавать** новый контакт (форма `AddEditContactActivity`) и **переходить** к деталям контакта по тапу на строку списка.
