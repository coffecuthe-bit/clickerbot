package t.bot.worker.request;

import lombok.Data;

@Data
public class TenantsRequest {

    private String emailOrPhone;

    private String password;

    public TenantsRequest() {
    }
}
