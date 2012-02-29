package com.nullware.android.fortunequote;

import java.util.HashSet;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * A {@link ListPreference} that displays a list of entries as a dialog and
 * allows multiple selections.  The selected items are stored as a string in
 * the SharedPreferences, using | (pipe) as a delimiter.
 */
public class ListPreferenceMultiSelect extends ListPreference {

    private boolean[] selectedEntries;

    public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.selectedEntries = new boolean[getEntries().length];
    }
    public ListPreferenceMultiSelect(Context context) {
        this(context, null);
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        this.selectedEntries = new boolean[entries.length];
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();
        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException("ListPreferenceMultiSelect requires an entries array and an entryValues array which are both the same length");
        }
        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, this.selectedEntries,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean val) {
                        selectedEntries[which] = val;
                    }
        });
    }

    /**
     * Return an array of strings parsed from a single pipe delimited string.
     *
     * @param value Pipe delimited string
     * @return parsed string array
     */
    public static String[] parseValue(CharSequence value) {
        if (value.length() > 0) {
            return ((String)value).split("\\|");
        } else {
            return new String[0];
        }
    }

    private void restoreCheckedEntries() {
        CharSequence[] entryValues = getEntryValues();
        String[] values = parseValue(getValue());
        HashSet<String> valuesSet = new HashSet<String>();
        for (int i = 0; i < values.length; i++) {
            valuesSet.add(values[i]);
        }
        for (int i = 0; i < entryValues.length; i++) {
            if (valuesSet.contains(entryValues[i])) {
                this.selectedEntries[i] = true;
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null) {
            StringBuffer value = new StringBuffer();
            for (int i = 0; i < entryValues.length; i++) {
                if (this.selectedEntries[i]) {
                    value.append(entryValues[i]).append("|");
                }
            }
            String str = value.toString();
            if (str.length() > 0) str = str.substring(0, str.length() - 1);
            if (callChangeListener(str)) {
                setValue(str);
            }
        }
    }

}
