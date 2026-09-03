package t.bot.worker.response.order;

import lombok.Data;

@Data
public class Address {
    private String addressLine1;

    private String addressLine2;

    @Override
    public String toString() {
        if (addressLine1 != null && addressLine2 != null)
            return "<b>Адресс</b>:  <code>" + addressLine1 + "</code>\n" +
                    "<b>Описание</b>: <code>" + addressLine2 + "</code>\n";
        if (addressLine1 != null && addressLine2 == null)
            return "<b>Адресс</b>:  <code>" + addressLine1 + "</code>\n" +
                    "<b>Описание</b>: <code>Отсутсвует</code>\n";
        if (addressLine1 == null && addressLine2 != null)
            return "<b>Адресс</b>:  <code>Отсутсвует</code>\n" +
                    "<b>Описание</b>: <code>" + addressLine2 + "</code>\n";

        return "<b>Адресс</b>:  <code>Отсутсвует</code>\n" +
                "<b>Описание</b>: <code>Отсутсвует</code>\n";
    }
}
