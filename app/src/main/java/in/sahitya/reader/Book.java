package in.sahitya.reader;

public class Book {
    public long id;
    public String title;
    public String subject;
    public String language;
    public String path;
    public String format;
    public int progress;

    public Book(long id, String title, String subject, String language, String path, String format, int progress) {
        this.id = id; this.title = title; this.subject = subject; this.language = language;
        this.path = path; this.format = format; this.progress = progress;
    }
}
