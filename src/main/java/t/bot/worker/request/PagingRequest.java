package t.bot.worker.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PagingRequest {
    private int pageNumber;
    private int pageSize;
    private int totalElements;
    private int totalPages;
}
