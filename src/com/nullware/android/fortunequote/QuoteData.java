package com.nullware.android.fortunequote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class QuoteData extends SQLiteOpenHelper {

    private static final String TAG = FortuneQuote.TAG + ".QuoteData";
    private static final String PREFERENCES_NAME = "QuoteDataPreferences";

    private static final String DATABASE_NAME = "quotes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
        "CREATE TABLE " + Quote.TABLE_NAME
        + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + Quote.TOPIC_ID + " INTEGER NOT NULL, "
        + Quote.ENTRY_ID + " INTEGER NOT NULL, "
        + Quote.AUTHOR + " TEXT NOT NULL, "
        + Quote.QUOTE + " TEXT NOT NULL);"
        + " CREATE INDEX UNIQUE INDEX IF NOT EXISTS " + Quote.INDEX_TOPIC_ENTRY_NAME
        + " ON TABLE " + Quote.TABLE_NAME
        + " (" + Quote.TOPIC_ID + ", " + Quote.ENTRY_ID + ");"
        + " CREATE INDEX UNIQUE INDEX IF NOT EXISTS " + Quote.INDEX_AUTHOR_NAME
        + " ON TABLE " + Quote.TABLE_NAME + " (" + Quote.AUTHOR + ");";

    private static final String DATABASE_DROP =
        "DROP TABLE IF EXISTS " + Quote.TABLE_NAME;

    private static final String[] QUOTE_TABLE_SELECT_COLUMNS = { Quote.AUTHOR, Quote.QUOTE };

    /**
     * List of quote topic names.
     */
    public static final String[] quoteTopics = {
        "art",
        "ascii_art",
        "bofh_excuses",
        "computers",
        "cookie",
        "debian",
        "debian_hints",
        "definitions",
        "disclaimer",
        "drugs",
        "education",
        "ethnic",
        "food",
        "fortunes",
        "goedel",
        "humorists",
        "kids",
        "knghtbrd",
        "law",
        "linux",
        "linuxcookie",
        "literature",
        "love",
        "magic",
        "medicine",
        "men_women",
        "miscellaneous",
        "news",
        "paradoxum",
        "people",
        "perl",
        "pets",
        "platitudes",
        "politics",
        "riddles",
        "science",
        "songs_poems",
        "sports",
        "startrek",
        "tao",
        "translate_me",
        "wisdom",
        "work",
        "zippy"
    };

    /**
     * List of quote topic file locations.
     */
    private static final Integer[] quoteTopicFiles = {
        R.raw.art,
        R.raw.ascii_art,
        R.raw.bofh_excuses,
        R.raw.computers,
        R.raw.cookie,
        R.raw.debian,
        R.raw.debian_hints,
        R.raw.definitions,
        R.raw.disclaimer,
        R.raw.drugs,
        R.raw.education,
        R.raw.ethnic,
        R.raw.food,
        R.raw.fortunes,
        R.raw.goedel,
        R.raw.humorists,
        R.raw.kids,
        R.raw.knghtbrd,
        R.raw.law,
        R.raw.linux,
        R.raw.linuxcookie,
        R.raw.literature,
        R.raw.love,
        R.raw.magic,
        R.raw.medicine,
        R.raw.men_women,
        R.raw.miscellaneous,
        R.raw.news,
        R.raw.paradoxum,
        R.raw.people,
        R.raw.perl,
        R.raw.pets,
        R.raw.platitudes,
        R.raw.politics,
        R.raw.riddles,
        R.raw.science,
        R.raw.songs_poems,
        R.raw.sports,
        R.raw.startrek,
        R.raw.tao,
        R.raw.translate_me,
        R.raw.wisdom,
        R.raw.work,
        R.raw.zippy
    };

    private Context context;
    private static SharedPreferences preferences;

    private static int[] quoteTopicCount = new int[QuoteData.quoteTopics.length];
    private static int quoteCount = -1;

    private static Thread loadThread;
    private static int loadThreadTimeOut = 1000; // one second

    private class AuthorQuote {
        String author = "";
        String quote = "";
    }

    /**
     * Class that handles loading quotes into a DB and querying them.
     *
     * @param context
     */
    public QuoteData(Context context) {
        super(context, DATABASE_NAME,  null, DATABASE_VERSION);
        this.context = context;
        if (preferences == null) preferences = context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        loadPreferences();
        loadQuotes();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DATABASE_DROP);
        onCreate(db);
    }

    /**
     * Return the next topicId that needs to be loaded or -1 if there are no
     * more to load.
     *
     * @return topicId
     */
    protected static int getNextLoadTopic() {
        if (quoteTopicCount[quoteTopicCount.length-1] > 0) {
            return -1;
        } else {
            int nextTopicId = 0;
            for (int topicId = 0; topicId < QuoteData.quoteTopics.length; topicId++) {
                if (quoteTopicCount[topicId] > 0) {
                    nextTopicId += 1;
                } else {
                    break;
                }
            }
            return nextTopicId;
        }
    }

    /**
     * Calculate the total number of quotes.
     *
     * @return Number of quotes
     */
    private static int getQuoteCount() {
        int quoteCount = 0;
        for (int topicId = 0; topicId < QuoteData.quoteTopics.length; topicId++) {
            quoteCount += quoteTopicCount[topicId];
        }
        return quoteCount;
    }

    /**
     * Initial loading of quotes from flat files into the SQLite DB.  Starts
     * {@link loadThread} thread to load them in the background.
     */
    protected synchronized void loadQuotes() {
        final QuoteData quoteData = this;
        // only want one db loading thread running
        if (loadThread == null && getNextLoadTopic() >= 0) {
            loadThread = new Thread(new Runnable(){
                synchronized public void run() {
                    SQLiteDatabase db = quoteData.getReadableDatabase();
                    Integer nextLoadTopic = QuoteData.getNextLoadTopic();
                    if (nextLoadTopic >= 0) {
                        try {
                            Log.i(TAG, "Initial quote data load started");
                            for (int topicId = nextLoadTopic; topicId < QuoteData.quoteTopics.length; topicId++) {
                                quoteData.loadQuote(db, topicId);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error performing initial quote data load", e);
                        } catch (InterruptedException e) {
                            Log.i(TAG, "Initial quote data load stopped", e);
                        }
                    }
                }
            });
            loadThread.start();
        }
    }

    /**
     * Called if this program is destroyed.  Stops {@link loadThread}
     * if it is running.
     *
     * @throws InterruptedException
     */
    protected synchronized void stopLoadQuotes() throws InterruptedException {
        if (loadThread != null && loadThread.isAlive()) {
            loadThread.interrupt();
            // give the thread some time to close
            int i = 0;
            int incr = loadThreadTimeOut/10;
            while (loadThread.isAlive() && i < loadThreadTimeOut) {
                Thread.sleep(incr);
                i += incr;
            }
            if (loadThread.isAlive()) {
                String err = "Error stopping initial data load";
                Log.e(TAG, err);
                throw new InterruptedException(err);
            }
            loadThread = null;
        }
    }

    /**
     * Stop any quote loading and close the DB.
     */
    public void close() {
        try {
            this.stopLoadQuotes();
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }
        super.close();
    }

    /**
     * Load a single quote topicId (file) into the DB.
     *
     * @param db
     * @param topicId
     * @throws IOException
     * @throws InterruptedException
     */
    private void loadQuote(SQLiteDatabase db, int topicId) throws IOException, InterruptedException {
        assert (db != null);
        Log.i(TAG, "Loading quote for topic='" + ((quoteTopics[topicId] == null) ? "null" : quoteTopics[topicId]) + "', topicId=" + topicId);
        assert (quoteTopics[topicId] != null);
        int entryId = 0;
        InputStream is = this.context.getResources().openRawResource(quoteTopicFiles[topicId]);
        assert (is != null);
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        assert (reader != null);
        quoteTopicCount[topicId] = 0;
        preferences.edit().putInt(this.context.getString(R.string.quoteTopicCount_prefix_key) + topicId, quoteTopicCount[topicId]).commit();
        quoteCount = getQuoteCount();
        String where = Quote.TOPIC_ID + " = " + topicId;
        db.delete(Quote.TABLE_NAME, where, null);
        int c1 = 0, c2 = 0, c3 = 0;
        Writer writer = new StringWriter();
        String quote;
        while ((c1 = reader.read()) != -1) {
            if (c1 == '\n' && c2 == '%' && c3 == '\n') {
                quote = writer.toString();
                if (quote.length() > 1) {
                    quote = quote.substring(0, quote.length()-2);
                }
                insertQuote(db, topicId, entryId, quote);
                entryId += 1;
                quoteTopicCount[topicId] += 1;
                quoteCount += 1;
                writer = new StringWriter();
            } else {
                writer.write(c1);
            }
            c3 = c2;
            c2 = c1;
            Thread.sleep(0);
        }
        preferences.edit().putInt(this.context.getString(R.string.quoteTopicCount_prefix_key) + topicId, quoteTopicCount[topicId]).commit();
        Log.i(TAG, "Loaded " + entryId + " quotes for topic='" + ((quoteTopics[topicId] == null) ? "null" : quoteTopics[topicId]) + "', topicId=" + topicId);
    }

    /**
     * Insert a single quote into the DB.
     *
     * @param db
     * @param topicId
     * @param entryId
     * @param quote
     */
    private void insertQuote(SQLiteDatabase db, int topicId, int entryId, String quote) {
        assert (db != null);
        assert (quote != null);
        //Log.d(TAG, "Inserting quote: entryId=" + entryId + ", quote: " + quote.replace("\n", "\\\\n"));
        AuthorQuote aq = parseQuote(quote);
        //Log.d(TAG, "Modified quote: entryId=" + entryId + ", author=" + author + ", quote: " + quote.replace("\n", "\\\\n"));
        ContentValues values = new ContentValues();
        values.put(Quote.TOPIC_ID, topicId);
        values.put(Quote.ENTRY_ID, entryId);
        values.put(Quote.AUTHOR, aq.author);
        values.put(Quote.QUOTE, aq.quote);
        db.insertOrThrow(Quote.TABLE_NAME, null, values);
    }

    private static final String AUTHOR_PREFIX = "\n\\s+--\\s+";
    private static Pattern authorRegexp = Pattern.compile(AUTHOR_PREFIX);

    /**
     * Parse a quote, cleaning it up and extracting the author.
     *
     * @param quote
     * @return {@link AuthorQuote} author and quote
     */
    private AuthorQuote parseQuote(String quote) {
        assert (quote != null);
        String author = "";
        String[] parts = authorRegexp.split(quote);
        if (parts.length == 2) {
            quote = parts[0];
            author = parts[1];
            author = unwrapAuthor(author);
        }
        quote = unwrapString(quote);
        AuthorQuote aq = new AuthorQuote();
        aq.author = author;
        aq.quote = quote;
        return aq;
    }

    private static final String NL = "\n";
    private static Pattern nlRegexp = Pattern.compile(NL);
    private static final String STARTING_WHITESPACE = "^\\s+";
    private static Pattern startingWSRegexp = Pattern.compile(STARTING_WHITESPACE);
    private static final String STARTING_LOWERCASE = "^\\s+[:lower:]";
    private static Pattern startingLowerCaseRegexp = Pattern.compile(STARTING_LOWERCASE);
    private static final String STARTING_QUESTION = "^\\s*Q:\\s+";
    private static Pattern startingQuestionRegexp = Pattern.compile(STARTING_QUESTION);
    private static final String STARTING_ANSWER = "^\\s*A:\\s+";
    private static Pattern startingAnswerRegexp = Pattern.compile(STARTING_ANSWER);

    /**
     * Try to remove the 80-column format for an author string.
     *
     * @param author
     * @return Formatted author
     */
    private static String unwrapAuthor(String author) {
        assert (author != null);
        StringBuilder sb = new StringBuilder(author.length());
        if (author.length() > 0) {
            String[] lines = nlRegexp.split(author);
            for (String line : lines) {
                if (line.length() > 0) {
                    if (startingWSRegexp.matcher(line).lookingAt()) {
                        if (sb.length() > 0) {
                            sb.append(" " + startingWSRegexp.split(line, 2)[1]);
                        } else {
                            sb.append(startingWSRegexp.split(line, 2)[1]);
                        }
                    } else {
                        if (sb.length() > 0) {
                            sb.append(" " + line);
                        } else {
                            sb.append(line);
                        }
                    }
                }
            }
        }
        return sb.toString();
   }

    /**
     * Try to remove the 80-column format for a string.
     *
     * @param str
     * @return Formatted string
     */
    private static String unwrapString(String str) {
        assert (str != null);
        StringBuilder sb = new StringBuilder(str.length());
        if (str.length() > 0) {
            String[] lines = nlRegexp.split(str);
            boolean wrapping = false;
            for (String line : lines) {
                if (wrapping) {
                    if (startingLowerCaseRegexp.matcher(line).lookingAt()) {
                        String[] parts = startingWSRegexp.split(line);
                        line = parts[1];
                    }
                    if (startingWSRegexp.matcher(line).lookingAt() || line.length() == 0 ||
                            startingQuestionRegexp.matcher(line).lookingAt() ||
                            startingAnswerRegexp.matcher(line).lookingAt()) {
                        sb.append('\n');
                        if (line.length() > 0) {
                            sb.append(line);
                        } else {
                            sb.append('\n');
                            wrapping = false;
                        }
                    } else {
                        sb.append(" " + line);
                    }
                } else {
                    if (line.length() > 0) {
                        sb.append(line);
                        wrapping = true;
                    } else {
                        sb.append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Return a random integer from 0 to max-1.
     *
     * @param max Upper bound of random number
     * @return A random integer from 0 to max-1
     */
    private static int random(int max) {
        return (int) (Math.random() * max);
    }

    /**
     * Return a single quote picked randomly.
     *
     * @param db Quote {@link SQLiteDatabase}
     * @param topics An array of topics to choose from
     * @param randomTopic If true first pick a topic randomly, then pick a random quote within that topic
     * @return quote
     */
    protected String getRandomQuote(int[] topics, boolean randomTopic) {
        SQLiteDatabase db = getReadableDatabase();
        String quote = this.context.getString(R.string.no_quote_text);
        // remove any topics that have not been loaded
        if (topics.length > 0) {
            ArrayList<Integer> topicsSet = new ArrayList<Integer>();
            for (int i = 0; i < topics.length; i++) {
                if (quoteTopicCount[topics[i]] > 0) topicsSet.add(topics[i]);
            }
            topics = new int[topicsSet.size()];
            for (int i = 0; i < topicsSet.size(); i++) {
                topics[i] = topicsSet.get(i);
            }
            if (topics.length == 0) {
                if (quoteTopicCount[0] > 0) {
                    quote = this.context.getString(R.string.no_quote_for_topics_text);
                } else {
                    quote = this.context.getString(R.string.no_quote_text);
                }
            } else if (quoteCount > 0) {
                // pick random quote
                int topicId = -1;
                int entryId = -1;
                if (randomTopic) {
                    // first pick a random topic, then a random entry within
                    while (entryId < 0) {
                        if (topics.length > 0) {
                            // restricted topic list
                            topicId = topics[random(topics.length)];
                        } else {
                            // all topics
                            topicId = random(quoteTopics.length);
                        }
                        if (quoteTopicCount[topicId] > 0) {
                            entryId = random(quoteTopicCount[topicId]);
                        }
                    }
                } else {
                    // pick a random quote from within all requested topics
                    if (topics.length > 0) {
                        // restricted topic list
                        int max = 0;
                        for (int i = 0; i < topics.length; i++) {
                            max += quoteTopicCount[topics[i]];
                        }
                        int num = random(max);
                        int i = 0;
                        while (num >= quoteTopicCount[topics[i]]) {
                            num -= quoteTopicCount[topics[i]];
                            i += 1;
                        }
                        topicId = topics[i];
                        entryId = num;
                    } else {
                        // all topics
                        int num = random(quoteCount);
                        int i = 0;
                        while (num >= quoteTopicCount[i]) {
                            num -= quoteTopicCount[i];
                            i += 1;
                        }
                        topicId = i;
                        entryId = num;
                    }
                }
                String where = Quote.TOPIC_ID + " = " + topicId + " AND " + Quote.ENTRY_ID + " = " + entryId;
                Cursor cursor = db.query(Quote.TABLE_NAME, QUOTE_TABLE_SELECT_COLUMNS, where, null, null, null, null);
                assert (cursor != null);
                if (cursor.moveToFirst()) {
                    String author = cursor.getString(0);
                    quote = cursor.getString(1);
                    if (author != null && author.length() > 0) {
                        quote = quote + "\n\n  -- " + author;
                    }
                }
                cursor.close();
                Log.d(TAG, "Random quote: topicId=" + topicId + ", entryId=" + entryId + ", quote: " + quote.replace("\n", "\\\\n"));
            }
        } else {
            quote = this.context.getString(R.string.no_topics_text);
        }
        return quote;
    }

    /**
     * Load preferences.
     */
    private synchronized void loadPreferences() {
        assert (preferences != null);
        // load static values once
        if (quoteCount == -1) {
            for (int topicId = 0; topicId < QuoteData.quoteTopics.length; topicId++) {
                quoteTopicCount[topicId] = preferences.getInt(this.context.getString(R.string.quoteTopicCount_prefix_key) + topicId, 0);
            }
            quoteCount = getQuoteCount();
        }
    }

}
