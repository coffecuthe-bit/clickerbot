package t.bot.worker.response.order;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerResponse {
    private String maskedExtCode;

    private List<PhoneResponse> phones = new ArrayList<>();

    private String name;
}
