package t.bot.worker.response.order;

import lombok.Data;

@Data
public class ProviderRewardResponse {

    private String currency;

    private Double value;

    @Override
    public String toString() {

        return value + " " + currency;
    }
}
