package t.bot.worker.response.order;

import lombok.Data;
import lombok.ToString;

@Data
public class EnterpriseReward {

    private String currency;

    private int value;

    @Override
    public String toString() {
        return value + " " + currency;
    }
}
