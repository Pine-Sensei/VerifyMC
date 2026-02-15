package team.kitemc.verifymc.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaginatedResult<T> {
    private List<T> items;
    private int currentPage;
    private int pageSize;
    private int totalCount;
    private int totalPages;
    
    public PaginatedResult() {
        this.items = new ArrayList<>();
    }
    
    public PaginatedResult(List<T> items, int currentPage, int pageSize, int totalCount) {
        this.items = items != null ? items : new ArrayList<>();
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
    }
    
    public static PaginatedResult<Map<String, Object>> fromMaps(List<Map<String, Object>> items, int currentPage, int pageSize, int totalCount) {
        return new PaginatedResult<>(items, currentPage, pageSize, totalCount);
    }
    
    public static PaginatedResult<User> fromUsers(List<User> users, int currentPage, int pageSize, int totalCount) {
        return new PaginatedResult<>(users, currentPage, pageSize, totalCount);
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("items", items);
        map.put("currentPage", currentPage);
        map.put("pageSize", pageSize);
        map.put("totalCount", totalCount);
        map.put("totalPages", totalPages);
        map.put("hasNextPage", currentPage < totalPages);
        map.put("hasPrevPage", currentPage > 1);
        return map;
    }
    
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public boolean hasNextPage() { return currentPage < totalPages; }
    public boolean hasPrevPage() { return currentPage > 1; }
    
    public boolean isEmpty() { return items == null || items.isEmpty(); }
    
    public int size() { return items != null ? items.size() : 0; }
}
