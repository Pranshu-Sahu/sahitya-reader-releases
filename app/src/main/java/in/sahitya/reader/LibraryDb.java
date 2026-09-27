package in.sahitya.reader;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class LibraryDb extends SQLiteOpenHelper {
    public LibraryDb(Context c) { super(c, "sahitya.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE books(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,subject TEXT NOT NULL,language TEXT NOT NULL,path TEXT NOT NULL,format TEXT NOT NULL,progress INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE annotations(id INTEGER PRIMARY KEY AUTOINCREMENT,book_id INTEGER NOT NULL,type TEXT NOT NULL,excerpt TEXT,note TEXT,position INTEGER DEFAULT 0,created_at INTEGER NOT NULL,FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    public long addBook(String title, String subject, String language, String path, String format) {
        ContentValues v = new ContentValues(); v.put("title", title); v.put("subject", subject); v.put("language", language); v.put("path", path); v.put("format", format);
        return getWritableDatabase().insert("books", null, v);
    }
    public List<Book> books(String filter) {
        List<Book> out = new ArrayList<>(); String where = "All".equals(filter) ? null : "subject=?";
        try (Cursor c = getReadableDatabase().query("books", null, where, where == null ? null : new String[]{filter}, null, null, "title COLLATE NOCASE")) {
            while (c.moveToNext()) out.add(new Book(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6)));
        }
        return out;
    }
    public Book book(long id) {
        try (Cursor c = getReadableDatabase().query("books", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? new Book(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6)) : null;
        }
    }
    public void progress(long id, int value) { ContentValues v = new ContentValues(); v.put("progress", value); getWritableDatabase().update("books", v, "id=?", new String[]{String.valueOf(id)}); }
    public void annotate(long id, String type, String excerpt, String note, int position) {
        ContentValues v = new ContentValues(); v.put("book_id", id); v.put("type", type); v.put("excerpt", excerpt); v.put("note", note); v.put("position", position); v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("annotations", null, v);
    }
    public List<String> annotations(long id) {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("annotations", new String[]{"type", "excerpt", "note"}, "book_id=?", new String[]{String.valueOf(id)}, null, null, "created_at DESC")) {
            while (c.moveToNext()) { String type = c.getString(0); String excerpt = c.getString(1); String note = c.getString(2); out.add(type + "  ·  " + (excerpt == null ? "" : excerpt) + (note == null || note.isEmpty() ? "" : "\n" + note)); }
        }
        return out;
    }
    public List<String> highlights(long id) {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("annotations", new String[]{"excerpt"}, "book_id=? AND type='Highlight'", new String[]{String.valueOf(id)}, null, null, "created_at")) {
            while (c.moveToNext()) { String excerpt=c.getString(0); if(excerpt!=null&&!excerpt.isEmpty())out.add(excerpt); }
        }
        return out;
    }
    public void deleteBook(long id) { getWritableDatabase().delete("annotations", "book_id=?", new String[]{String.valueOf(id)}); getWritableDatabase().delete("books", "id=?", new String[]{String.valueOf(id)}); }
}
