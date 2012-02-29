package com.nullware.android.fortunequote;

import android.provider.BaseColumns;

public interface Quote extends BaseColumns {

    public static final String TABLE_NAME = "quotes";
    public static final String TOPIC_ID = "topic_id";
    public static final String ENTRY_ID = "entry_id";
    public static final String AUTHOR = "author";
    public static final String QUOTE = "quote";
    public static final String INDEX_TOPIC_ENTRY_NAME = "topic_entry_idx";
    public static final String INDEX_AUTHOR_NAME = "author_idx";

}
