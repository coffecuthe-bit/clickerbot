package t.bot.worker.service.clicker;

import t.bot.worker.response.order.OrderListResponse;

public interface LionInterface {

    Boolean auth();

    OrderListResponse getOrder();

    void proposedOrder();

    void addBlackList();

    void primaryWords();

    void canselOrder();
}
