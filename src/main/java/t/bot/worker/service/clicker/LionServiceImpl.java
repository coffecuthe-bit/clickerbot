package t.bot.worker.service.clicker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import t.bot.worker.request.LoginRequest;
import t.bot.worker.request.TenantsRequest;
import t.bot.worker.response.AuthResponse;
import t.bot.worker.response.TenantsResponse;
import t.bot.worker.response.TenantsResponses;
import t.bot.worker.response.order.OrderListResponse;
import t.bot.worker.response.order.OrderResponse;
import t.bot.worker.response.order.PhoneResponse;
import t.bot.worker.service.ClickerService.Endpoint;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Component
@Singleton
public class LionServiceImpl implements LionInterface {

    public static OrderListResponse orderResponse = new OrderListResponse();
    public static boolean enabled = true;

    private HttpRequestBuilder httpRequestBuilder = new HttpRequestBuilder();

    private static Timestamp lastUpdate = new Timestamp(System.currentTimeMillis());
    private static Timestamp lastLog = new Timestamp(System.currentTimeMillis());
    @Value("{auth.url}")
    private String url = "";

    private final String sortParam = URLEncoder.encode("$createdAt$,DESC", StandardCharsets.UTF_8);

    private TenantsResponse authResponse = null;
    @Value("{auth.login}")
    private String LOGIN;
    @Value("{auth.password}")
    private String PASSWORD;

    public static List<String> blockedTask = new CopyOnWriteArrayList<>();

    public LionServiceImpl(HttpRequestBuilder httpRequestBuilder) {
        this.httpRequestBuilder = httpRequestBuilder;
    }

    @Override
    @Scheduled(fixedDelay = 5 * 3600 * 1000)
    public Boolean auth() {
        String id = getTenantsId();
        if (id != null) {
            LoginRequest requestBody = new LoginRequest();
            requestBody.setTenantId(id);
            requestBody.setPassword(PASSWORD);
            requestBody.setEmailOrPhone(LOGIN);
            HttpRequest request = httpRequestBuilder.buildPostRequest(url + Endpoint.AUTH.getValue(), requestBody, false);
            AuthResponse response = httpRequestBuilder.sendRequest(request, AuthResponse.class, true);
            System.out.println("Auth response: " + response.getSuccess());
            return response.getSuccess();
        }
        return false;
    }


    private String getTenantsId() {
        TenantsRequest requestBody = new TenantsRequest();
        String tenUrl = url + Endpoint.AUTH_TENANTS.getValue();
        requestBody.setEmailOrPhone(LOGIN);
        requestBody.setPassword(PASSWORD);
        HttpRequest request = httpRequestBuilder.buildPostRequest(tenUrl, requestBody, true);
        TenantsResponses dataResponses = httpRequestBuilder.sendRequest(request, TenantsResponses.class, false);
        if (dataResponses != null && dataResponses.isSuccess() && dataResponses.getData() != null) {
            authResponse = dataResponses.getData().get(0);
        }
        return authResponse.getId();
    }


    @Override
    public OrderListResponse getOrder() {
        if (orderResponse.getData() == null || orderResponse.getData().isEmpty()) {
            return updateOrderList();
        } else {
            return updateOrderList();
        }
    }

    @Scheduled(fixedDelay = 3000)
    public void WatchAndAccept() {

        if (enabled && authResponse != null) {


            String finalUrl = url + Endpoint.ORDERS.getValue() +
                    "?sort=" + sortParam +
                    "&tenantId=" + authResponse.getId() +
                    "&distribution=OFFER" +
                    "&regionIds=";
            HttpRequest request = httpRequestBuilder.buildGetRequest(finalUrl, true);
            OrderListResponse orderResponse1 = httpRequestBuilder.sendRequest(request, OrderListResponse.class, false);
            if (System.currentTimeMillis() - lastLog.getTime() > 600_000) {
                log.info("Watch and accept called {}", orderResponse1.getData().size());
                lastLog = new Timestamp(System.currentTimeMillis() + 3_600_000);
            }
            if (orderResponse1 != null && orderResponse1.getData() != null && !orderResponse1.getData().isEmpty()) {
                log.info("Order response: " + orderResponse1.getData().size());
                for (OrderResponse order : orderResponse1.getData()) {
                    if (!blockedTask.contains(order.getId())) {
                        if (order.getId() != null) {
                            String finalURL = "https://crm-api.clicker.one/distribution/order/" + order.getId() + "/offer/accept";
                            acceptOrder(order.getId(), finalURL);
                        }
                    } else {
                        log.info("Skipped blackList order: " + order.getId());
                    }
                }
            }
        }
    }

    public static Map<String, String> getAllContacts(String jsonResponse) {
        Map<String, String> contacts = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode contactsNode = root.path("data")
                    .path("clientAppSettings")
                    .path("defaultCustomerSupportContacts");

            for (JsonNode contact : contactsNode) {
                String type = contact.path("type").asText();
                String phone = contact.path("valueForCustomer").asText();
                contacts.put(type, phone);
            }
        } catch (Exception e) {
            log.error(e.getMessage());

        }

        return contacts;
    }

    public void addContact(OrderResponse response) {

        if (response.getPhone() != null || response.getWhatsapp() != null) {
            return;
        }
        String url = "[TEXT]" + response.getId();
        HttpRequest request = httpRequestBuilder.buildGetRequest(url, true);
        String resp = httpRequestBuilder.sendRequest(request, String.class, false);
        if (resp != null && !resp.isEmpty()) {
            Map<String, String> contacts = getAllContacts(resp);
            String whatsapp = "<code>" + contacts.get("WHATSAPP") + "</code>";
            StringBuilder phone = new StringBuilder();
            phone.append("<code>")
                    .append(contacts.get("PHONE")).append("</code>");
            if (response.getCustomer() != null) {
                if (!response.getCustomer().getPhones().isEmpty())
                    for (PhoneResponse phoneResponse : response.getCustomer().getPhones()) {
                        phone.append("<code>").append(" доб. " + phoneResponse.getMaskedExtCode()).append("</code>\n");
                    }
            }
            response.setWhatsapp(whatsapp);
            response.setPhone(String.valueOf(phone));
        }
    }

    private void acceptOrder(String orderId, String url) {
        HttpRequest request1 = httpRequestBuilder.buildPostRequest(url, "", true);
        httpRequestBuilder.sendRequest(request1, String.class, false);
        log.info("Заказ принят" + orderId);
    }


    private OrderListResponse updateOrderList() {
        if (System.currentTimeMillis() - lastUpdate.getTime() > 60_000) {
            lastUpdate = new Timestamp(System.currentTimeMillis());

            String filterParam = URLEncoder.encode(
                    "$status.value$ in['IN_PROGRESS_PROVIDER_NOT_SET','IN_PROGRESS_PROVIDER_IS_SET','IN_PROGRESS_PROVIDER_IS_SET_WORK_IN_PROGRESS']",
                    StandardCharsets.UTF_8
            );

            String finalUrl = url + Endpoint.ORDERS.getValue() +
                    "?sort=" + sortParam +
                    "&filter=" + filterParam +
                    "&tenantId=" + authResponse.getId() +
                    "&regionIds=";

            HttpRequest request = httpRequestBuilder.buildGetRequest(finalUrl, true);

            OrderListResponse r = httpRequestBuilder.sendRequest(request, OrderListResponse.class, false);
            if (r.getData() != null) {
                orderResponse = r;
            }
            return orderResponse;
        } else {
            return orderResponse;
        }
    }


    @Override
    public void proposedOrder() {

    }

    @Override
    public void addBlackList() {

    }

    @Override
    public void primaryWords() {

    }

    @Override
    public void canselOrder() {

    }


}
