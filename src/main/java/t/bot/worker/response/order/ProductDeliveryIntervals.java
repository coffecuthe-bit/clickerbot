package t.bot.worker.response.order;

import lombok.Data;

@Data
public class ProductDeliveryIntervals {

    private String from;

    private String to;

    @Override
    public String toString() {
        return "<b>Дата доставки</b>:\n   " + getMskTime(from) + " -- " + getMskTime(to);
    }

    private String getMskTime(String utcTime) {
        if (utcTime != null && utcTime.length() > 0) {
            String date = utcTime.substring(0, 10);
            int hour = Integer.parseInt(utcTime.substring(11, 13)) + 3;
            String time = String.format("%02d", hour) + ":" + utcTime.substring(14, 16);
            return date + " " + time;
        }
        return "Неизвестно";
    }
}
