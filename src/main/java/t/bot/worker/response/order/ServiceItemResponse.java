package t.bot.worker.response.order;

import lombok.Data;

@Data
public class ServiceItemResponse {
    private String createdAt;

    private String description;

    private String updatedAt;
    //Сборка двуспальной кровати (160x 210)
    private String name;
    //* Выезд за границу города\n* Доработка мебели\n* Перемещение мебели другую комнату, на другой этаж, занос мебели с улицы в квартиру клиента\n* Скрепления модулей/элементов мебели межсекционной стяжкой\n* Установка подъемного механизма кровати \n* Сборка ящиков на кровати\n* Крепление мебели к стене\n* Расходные  материалы\n* Утилизация упаковки
//Поле ждя ключевых слов
    private String notIncludedText;

    private ProviderRewardResponse providerReward = new ProviderRewardResponse();
    /*
createdAt : "2026-04-26T13:30:13.400Z"
id: "86249878-eba7-460d-af68-a708f454e5d4"
name: "Сборка мебели — Кровати"
*/

    private ServiceCategory serviceCategory;


    /*
{currency: "RUB", value: 420, initialValue: 420}
     */
    private EnterpriseReward enterpriseReward;

    //"* Выезд специалиста в пределах административной границы города( для г. Москва выезд в пределах МКАД) в котором приобретена сборка \n* Распаковка и визуальная проверка мебели.\n* Сборка мебели согласно инструкции изготовителя\n* Установка в указанное клиентом место"
    private String includedText;

}
