package t.bot.worker.response.order;

import lombok.Data;

import java.util.List;

@Data
public class OrderResponse {
    //"2026-04-26T13:30:13.409Z"
    private String createdAt;
    //"2026-04-26T13:30:13.409Z"
    private String updatedAt;
    //RUB
    private String currency;

    private String fio;
    //69ee136537cddd5d5af015d7
    private String id;

    private List<ProductDeliveryIntervals> productDeliveryIntervals;

    private List<VisitIntervalsResponse> visitIntervals;

    private String phone;

    private CustomerResponse customer;

    private String whatsapp;


    /*
createdAt: "2026-04-26T13:30:13.400Z"
id: "86249878-eba7-460d-af68-a708f454e5d4"
name: "Сборка мебели — Кровати"
     */
    private List<ServiceItemResponse> serviceItems;

    private Address address;
    //84d1b624-3d94-4659-a8ef-070df41e529e
    private String technicalName;

    /*
    id: "f45ab621-b2c1-4251-8b4a-3d0a0ed33eb1"
    logoId: "DefaultOrderType"
    name: "Установка"
     */
    private TypeResponse type;

}
