package t.bot.worker.response;

import lombok.Data;

import java.util.List;

@Data
public class TenantsResponses {

    private List<TenantsResponse> data;

    private boolean success;

}
