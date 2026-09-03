package t.bot.worker.response.order;

import lombok.Data;

@Data
public class PagingResponse {
    private int pageNumber;
    private int totalPages;
    private int pageSize;
    private int totalElements;
}
