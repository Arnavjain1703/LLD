package library.search;

public class SearchCriteria {

    private String title;
    private String author;
    private String isbn;
    private String genre;
    private boolean availableOnly;
    private int page;
    private int pageSize;

    private SearchCriteria() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SearchCriteria c = new SearchCriteria();

        public Builder title(String title)        { c.title = title; return this; }
        public Builder author(String author)      { c.author = author; return this; }
        public Builder isbn(String isbn)          { c.isbn = isbn; return this; }
        public Builder genre(String genre)        { c.genre = genre; return this; }
        public Builder availableOnly(boolean val) { c.availableOnly = val; return this; }
        public Builder page(int page)             { c.page = page; return this; }
        public Builder pageSize(int size)         { c.pageSize = size; return this; }

        public SearchCriteria build() { return c; }
    }

    public String getTitle()         { return title; }
    public String getAuthor()        { return author; }
    public String getIsbn()          { return isbn; }
    public String getGenre()         { return genre; }
    public boolean isAvailableOnly() { return availableOnly; }
    public int getPage()             { return page; }
    public int getPageSize()         { return pageSize; }
}
