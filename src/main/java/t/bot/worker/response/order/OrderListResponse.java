package t.bot.worker.response.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderListResponse {
    private boolean success;

    private List<OrderResponse> data;

    private PagingResponse paging;



}
