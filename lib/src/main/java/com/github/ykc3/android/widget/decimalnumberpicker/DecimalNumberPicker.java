/*
 * Copyright (c) 2022 Victor Antonovich <v.antonovich@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.ykc3.android.widget.decimalnumberpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.github.ykc3.android.widget.R;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;

public class DecimalNumberPicker extends RelativeLayout {
    private static final float DEFAULT_MIN_VALUE = 0;
    private static final float DEFAULT_MAX_VALUE = Integer.MAX_VALUE;
    private static final float DEFAULT_STEP = 1f;
    private static final int DEFAULT_NUM_DECIMALS = 3;

    private final Context context;

    private AttributeSet attrs;
    private int styleAttr;

    private BigDecimal minValue, maxValue;
    private int numDecimals;

    private BigDecimal lastValue, currentValue;
    private BigDecimal step;
    private boolean dynamicStepEnabled;

    private EditText valueEditText;
    private Button valueMinusButton;
    private Button valuePlusButton;

    private OnClickListener onClickListener;
    private OnValueChangeListener onValueChangeListener;

    public interface OnClickListener {
        void onClick(View view);
    }

    public interface OnValueChangeListener {
        void onValueChange(DecimalNumberPicker view, float oldValue, float newValue);
    }

    private static class SavedState extends BaseSavedState {
        private BigDecimal lastValue, currentValue;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            lastValue = readBigDecimalFromParcel(in);
            currentValue = readBigDecimalFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            writeBigDecimalToParcel(out, lastValue);
            writeBigDecimalToParcel(out, currentValue);
        }

        private static void writeBigDecimalToParcel(Parcel out, BigDecimal value) {
            out.writeByteArray(value.unscaledValue().toByteArray());
            out.writeInt(value.scale());
        }

        private static BigDecimal readBigDecimalFromParcel(Parcel in) {
            return new BigDecimal(new BigInteger(in.createByteArray()), in.readInt());
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public DecimalNumberPicker(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public DecimalNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        initView();
    }

    public DecimalNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attrs = attrs;
        this.styleAttr = defStyleAttr;
        initView();
    }

    private void initView(){
        inflate(context, R.layout.decimal_number_picker, this);

        valueEditText = findViewById(R.id.value);
        valueMinusButton = findViewById(R.id.btn_minus);
        valuePlusButton = findViewById(R.id.btn_plus);

        valueEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                // Loose focus
                v.clearFocus();
            }
            return false;
        });
        valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                EditText editText = ((EditText) v);
                // Hide software keyboard
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                // Try to set edited value
                trySetValue(editText.getText().toString().trim());
            }
        });
        valueMinusButton.setOnClickListener(mView -> updateValueByPlusOrMinusButton(false));
        valuePlusButton.setOnClickListener(mView -> updateValueByPlusOrMinusButton(true));

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DecimalNumberPicker, styleAttr, 0);
        setEnabled(a.getBoolean(R.styleable.DecimalNumberPicker_android_enabled, isEnabled()));
        numDecimals = a.getInt(R.styleable.DecimalNumberPicker_numDecimals, DEFAULT_NUM_DECIMALS);
        minValue = BigDecimal.valueOf(a.getFloat(R.styleable.DecimalNumberPicker_minValue, DEFAULT_MIN_VALUE));
        maxValue = BigDecimal.valueOf(a.getFloat(R.styleable.DecimalNumberPicker_maxValue, DEFAULT_MAX_VALUE));
        step = BigDecimal.valueOf(a.getFloat(R.styleable.DecimalNumberPicker_step, DEFAULT_STEP));
        dynamicStepEnabled = a.getBoolean(R.styleable.DecimalNumberPicker_dynamicStep, true);
        currentValue = BigDecimal.valueOf(a.getFloat(R.styleable.DecimalNumberPicker_value, minValue.floatValue()));
        float textSize = a.getDimension(R.styleable.DecimalNumberPicker_textSize, valueEditText.getTextSize());

        valueEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        valueMinusButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        valuePlusButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        setValue(currentValue, false);

        lastValue = currentValue;

        setSaveEnabled(true);

        a.recycle();
    }

    protected void updateValueByPlusOrMinusButton(boolean isPlusButton) {
        BigDecimal s = step;

        int lastValueLength = valueEditText.getText().length();

        int cursorPosition = valueEditText.isFocused() ? valueEditText.getSelectionEnd() : -1;
        if (cursorPosition > 0 && dynamicStepEnabled) {
            // Use dynamic step if enabled and cursor is set to position in the edited value text
            String valueText = valueEditText.getText().toString();
            int commaPosition = normalizeFloat(valueText).indexOf('.');
            // Set virtual comma position to the end of value string representation
            // if no actual comma present
            if (commaPosition < 0) {
                commaPosition = valueText.length();
            }
            int n = commaPosition - cursorPosition;
            s = BigDecimal.ONE.movePointRight((n >= 0) ? n : n + 1);
        }

        // Update value by the step according to selected direction
        setValue(isPlusButton ? currentValue.add(s) : currentValue.subtract(s), true);

        // Set cursor position after value update if needed
        if (cursorPosition > 0) {
            // Correct cursor position if value text representation length was changed
            cursorPosition += valueEditText.getText().length() - lastValueLength;
            if (cursorPosition > 0) {
                valueEditText.setSelection(cursorPosition);
            } else {
                valueEditText.clearFocus();
            }
        } else {
            valueEditText.clearFocus();
        }
    }

    private void notifyListeners(){
        if (onClickListener != null) {
            onClickListener.onClick(this);
        }

        if (onValueChangeListener != null && !Objects.equals(lastValue, currentValue)) {
            onValueChangeListener.onValueChange(this, lastValue.floatValue(),
                    currentValue.floatValue());
        }
    }

    private void trySetValue(String value) {
        try {
            setValue(BigDecimal.valueOf(parseFloat(value)), true);
        } catch (NumberFormatException e) {
            setValue(currentValue, false);
        }
    }

    private void setValue(BigDecimal value, boolean notifyListeners) {
        if (value.compareTo(maxValue) > 0) {
            value = maxValue;
        }

        if (value.compareTo(minValue) < 0) {
            value = minValue;
        }

        String valueText = String.format(Locale.ROOT, getFloatFormatter(), value);
        valueEditText.setText(valueText);

        lastValue = currentValue;
        currentValue = value;

        if (notifyListeners) {
            notifyListeners();
        }
    }

    private String getFloatFormatter() {
        return "%." + numDecimals + "f";
    }

    private static String normalizeFloat(String floatStr) {
        return floatStr.replace(",",".");
    }

    private static float parseFloat(String floatStr) throws NumberFormatException {
        return Float.parseFloat(normalizeFloat(floatStr));
    }

    public void setValue(float value) {
        setValue(BigDecimal.valueOf(value), false);
    }

    public float getValue() {
        return currentValue.floatValue();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.onValueChangeListener = onValueChangeListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        valueEditText.setEnabled(enabled);
        valueMinusButton.setEnabled(enabled);
        valuePlusButton.setEnabled(enabled);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState myState = new SavedState(superState);
        myState.lastValue = lastValue;
        myState.currentValue = currentValue;
        return myState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        lastValue = savedState.lastValue;
        currentValue = savedState.currentValue;
        setValue(currentValue, false);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchThawSelfOnly(container);
    }
}