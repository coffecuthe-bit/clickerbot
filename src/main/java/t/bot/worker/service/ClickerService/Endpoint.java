package t.bot.worker.service.ClickerService;

import lombok.Getter;


@Getter
public enum Endpoint {
    AUTH_TENANTS("/auth/tenants"),
    AUTH("/auth/login"),
    ORDERS_VIEW("/order-views"),
    ORDERS("/orders");

   private final String value;

    Endpoint(String value) {
        this.value = value;
    }

}
