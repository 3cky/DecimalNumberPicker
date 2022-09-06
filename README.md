# Decimal Number 

[![](https://jitpack.io/v/3cky/DecimalNumberPicker.svg)](https://jitpack.io/#3cky/DecimalNumberPicker)

The android library provides a simple decimal number picker with value increase/decrease buttons.
It's suitable for frequency tune inputs, correction values, etc.

## Features

- Customizable range, supports negative values
- Customizable precision (number of fractional decimals)
- Customizable step, including dynamic by the cursor position

## Usage

### Gradle

Add the dependency to your `build.gradle`:

```gradle
allprojects {
    repositories {
       maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.3cky:decimalnumberpicker:1.0.0'
}
```

### XML

Add `xmlns:app="http://schemas.android.com/apk/res-auto"` to your layout. 

```xml
<com.github.ykc3.android.widget.decimalnumberpicker.DecimalNumberPicker
    android:id="@+id/decimal_number_picker"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:minValue="-1000"
    app:maxValue="1000"
    app:numDecimals="3"
    app:step="1"
    app:dynamicStep="true"
    app:value="1" />
```

### Java

```java
import com.github.ykc3.android.widget.decimalnumberpicker.DecimalNumberPicker;
//...
DecimalNumberPicker decimalNumberPicker = findViewById(R.id.decimal_number_picker);
decimalNumberPicker.setOnValueChangeListener((view, oldValue, newValue) ->
    Toast.makeText(this, getString(R.string.toast_text, oldValue, newValue),
        Toast.LENGTH_SHORT).show());
```

See also included [example](app) project.

## Attributes

|attribute name|attribute type|attribute description|default|
|:---:|:---:|:---:|:---:|
|minValue|float|Minimum value|0
|maxValue|float|Maximum value|Integer.MAX
|step|float|Value change step|1
|dynamicStep|boolean|Enable dynamic step|true
|numDecimals|integer|Number of fractional decimals|3
|textSize|dimension|Value text size|

## License

The source code is licensed under the [MIT](LICENSE) license.