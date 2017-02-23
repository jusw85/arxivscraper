package edu.soton.ecs.arxivscraper;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ArxivEntry implements Serializable {

    private String id;
    private String title;
    private Date published;
    private Date updated;
    private String summary;
    private List<String> authors;
    private List<Link> links;
    private List<String> categories;
    private String primaryCategory;
    private String comment;
    private String doi;
    private String journalRef;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getPrimaryCategory() {
        return primaryCategory;
    }

    public void setPrimaryCategory(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getJournalRef() {
        return journalRef;
    }

    public void setJournalRef(String journalRef) {
        this.journalRef = journalRef;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("title", title)
                .add("id", id)
                .add("published", published)
                .add("updated", updated)
                .add("summary", summary)
                .add("authors", authors)
                .add("links", links)
                .add("categories", categories)
                .add("primaryCategory", primaryCategory)
                .add("comment", comment)
                .add("doi", doi)
                .add("journalRef", journalRef)
                .toString();
    }

    public static class Link implements Serializable {
        private String title;
        private String href;
        private String rel;
        private String type;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("title", title)
                    .add("href", href)
                    .add("rel", rel)
                    .add("type", type)
                    .toString();
        }
    }

}
