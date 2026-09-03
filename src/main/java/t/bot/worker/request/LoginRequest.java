package t.bot.worker.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class LoginRequest extends TenantsRequest {
    private String tenantId;


}
