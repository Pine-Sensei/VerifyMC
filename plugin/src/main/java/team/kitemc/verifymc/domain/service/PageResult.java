package team.kitemc.verifymc.domain.service;

import java.util.Collections;
import java.util.List;

public final class PageResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final int totalPages;

    public PageResult(List<T> items, int page, int pageSize, long total) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(Collections.emptyList(), page, pageSize, 0);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageResult<>(items, page, pageSize, total);
    }
}
