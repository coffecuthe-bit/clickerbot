package t.bot.worker.service.bot;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import t.bot.worker.config.BotConfig;
import t.bot.worker.response.order.*;
import t.bot.worker.service.clicker.LionServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static t.bot.worker.service.bot.SettingBotMenu.getMainMenuKeyboard;
import static t.bot.worker.service.bot.SettingBotMenu.getSettingBotMenu;
import static t.bot.worker.service.clicker.LionServiceImpl.blockedTask;

@Component
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {
    private final LionServiceImpl lionService;
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);
    private final BotConfig botConfig;
    private static Map<String, Boolean> allowedUsername = new HashMap<>();

    private final String HELP_TEXT = "Помощь\n\n" +
            "Доступные команды:\n" +
            "• /start - перезапустить бота\n" +
            "• Текущие заказы - показать список заказов\n\n";

    private static OrderListResponse updatedOrderList = new OrderListResponse();
    private static Map<String, Long> blacklistedOrders = new HashMap<>();

    private static Map<Long, Integer> userCurrentOrderIndex = new HashMap<>();

    static {
        allowedUsername.put("coffecuthe", true);
        allowedUsername.put("rushen_hyligans", true);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getChat().getUserName();

            if (allowedUsername.get(username) == null) {
                sendNoPerm(chatId, update.getMessage().getChat().getFirstName());
            } else {
                switch (messageText) {
                    case "/start":
                    case " Главное меню":
                        sendMessageWithKeyboard(chatId, "Главное меню", getMainMenuKeyboard());
                        break;
                    case "Настройки":
                        sendMessageWithKeyboard(chatId, "Настройки", getSettingBotMenu());
                        break;
                    case " Помощь":
                        sendHelpMessage(chatId);
                        break;
                    case "Текущие заказы":
                        userCurrentOrderIndex.put(chatId, 0);
                        sendOrderByIndex(chatId, lionService.getOrder(), 0);
                        break;
                    case "Статус бота":
                        if (LionServiceImpl.enabled) {
                            sendMessageWithKeyboard(chatId,
                                    "Бот активен ", getMainMenuKeyboard());
                        } else {
                            sendMessageWithKeyboard(chatId,
                                    "Бот не активен ", getMainMenuKeyboard());
                        }
                        break;
                    case "Включить":
                        if (!LionServiceImpl.enabled) {
                            LionServiceImpl.enabled = true;
                            sendMessageWithKeyboard(chatId,
                                    "Перелючено состояние на Активен ", getMainMenuKeyboard());
                        } else {
                            sendMessageWithKeyboard(chatId,
                                    "Бот уже активен ", getMainMenuKeyboard());
                        }
                        logger.info("Бот {} пользователем {}", LionServiceImpl.enabled, username);
                        break;
                    case "Выключить":
                        if (LionServiceImpl.enabled) {
                            LionServiceImpl.enabled = false;
                            sendMessageWithKeyboard(chatId,
                                    "Перелючено состояние на Выключен ", getMainMenuKeyboard());
                        } else {
                            sendMessageWithKeyboard(chatId,
                                    "Бот уже выключен ", getMainMenuKeyboard());
                        }
                        logger.info("Бот {} пользователем {}", LionServiceImpl.enabled, username);
                        break;
                    default:
                        sendMessageWithKeyboard(chatId,
                                "Неизвестная команда! Используйте кнопки ниже:",
                                getMainMenuKeyboard());
                }
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        String username = callbackQuery.getMessage().getFrom().getUserName();
        int messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("blacklist_add_")) {
            String orderId = data.substring("blacklist_add_".length());


            updatedOrderList = (lionService.getOrder());
            if (updatedOrderList == null || updatedOrderList.getData() == null) {
                return;
            }


            int currentIndex = userCurrentOrderIndex.getOrDefault(chatId, 0);


            addToBlacklist(username, orderId);


            if (!orderId.isEmpty()) {
                updatedOrderList.getData().removeIf(order -> order.getId().equals(orderId));
            }

            if (updatedOrderList.getData().isEmpty()) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(String.valueOf(chatId));
                deleteMessage.setMessageId(messageId);
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    logger.error("Ошибка при удалении сообщения: {}", e.getMessage());
                }

                sendMessage(chatId, " У вас нет текущих заказов.");

                userCurrentOrderIndex.remove(chatId);
            } else {
                int newIndex = currentIndex;
                if (currentIndex >= updatedOrderList.getData().size()) {
                    newIndex = updatedOrderList.getData().size() - 1;
                }

                userCurrentOrderIndex.put(chatId, newIndex);

                OrderResponse newOrder = updatedOrderList.getData().get(newIndex);
                int size = updatedOrderList.getData().size();
                String text = formatSingleOrder(newOrder, newIndex + 1, size);
                InlineKeyboardMarkup markup = createNavigationKeyboard(newIndex, size, newOrder.getId());

                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(messageId);
                edit.setText(text);
                edit.setParseMode("HTML");
                edit.setReplyMarkup(markup);

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    logger.error("Ошибка при редактировании сообщения: {}", e.getMessage());
                }
            }
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText(" Заказ добавлен в черный список");
            answer.setShowAlert(false);
            try {
                execute(answer);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при ответе на callback: {}", e.getMessage());
            }

        } else if (data.startsWith("order_next_")) {
            int currentIndex = Integer.parseInt(data.substring("order_next_".length()));
            OrderListResponse orders = getFilteredOrders(chatId);

            if (orders != null && currentIndex + 1 < orders.getData().size()) {
                userCurrentOrderIndex.put(chatId, currentIndex + 1);
                editOrderMessage(chatId, messageId, orders, currentIndex + 1);
            }

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            try {
                execute(answer);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при ответе на callback: {}", e.getMessage());
            }

        } else if (data.startsWith("order_prev_")) {
            int currentIndex = Integer.parseInt(data.substring("order_prev_".length()));
            OrderListResponse orders = getFilteredOrders(chatId); // Получаем отфильтрованные заказы

            if (orders != null && currentIndex - 1 >= 0) {
                userCurrentOrderIndex.put(chatId, currentIndex - 1);
                editOrderMessage(chatId, messageId, orders, currentIndex - 1);
            }

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            try {
                execute(answer);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при ответе на callback: {}", e.getMessage());
            }

        } else if (data.equals("close_orders")) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(String.valueOf(chatId));
            deleteMessage.setMessageId(messageId);
            try {
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при удалении сообщения: {}", e.getMessage());
            }

            userCurrentOrderIndex.remove(chatId);

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            try {
                execute(answer);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при ответе на callback: {}", e.getMessage());
            }
        }
    }

    private OrderListResponse getFilteredOrders(long chatId) {
        OrderListResponse allOrders = lionService.getOrder();
        if (allOrders == null || allOrders.getData().isEmpty()) {
            return allOrders;
        }


        List<OrderResponse> filteredOrders = new ArrayList<>();
        for (OrderResponse order : allOrders.getData()) {
            if (!isOrderBlacklisted(order.getId())) {
                filteredOrders.add(order);
            }
        }

        OrderListResponse filteredResponse = new OrderListResponse();
        filteredResponse.setData(filteredOrders);
        return filteredResponse;
    }

    private boolean isOrderBlacklisted(String orderId) {
        return blacklistedOrders.containsKey(orderId);
    }

    private void sendOrderByIndex(long chatId, OrderListResponse orders, int index) {
        if (orders == null || orders.getData().isEmpty()) {
            sendMessage(chatId, " У вас нет текущих заказов.");
            return;
        }

        OrderListResponse filteredOrders = getFilteredOrders(chatId);

        if (filteredOrders.getData().isEmpty()) {
            sendMessage(chatId, "У вас нет текущих заказов.");
            return;
        }

        if (index < 0 || index >= filteredOrders.getData().size()) {
            sendMessage(chatId, " Заказ не найден.");
            return;
        }

        OrderResponse order = filteredOrders.getData().get(index);
        String text = formatSingleOrder(order, index + 1, filteredOrders.getData().size());

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        InlineKeyboardMarkup markup = createNavigationKeyboard(index, filteredOrders.getData().size(), order.getId());
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке заказа: {}", e.getMessage());
        }
    }

    private void editOrderMessage(long chatId, int messageId, OrderListResponse orders, int newIndex) {
        if (newIndex < 0 || newIndex >= orders.getData().size()) return;

        OrderResponse order = orders.getData().get(newIndex);
        String text = formatSingleOrder(order, newIndex + 1, orders.getData().size());
        InlineKeyboardMarkup markup = createNavigationKeyboard(newIndex, orders.getData().size(), order.getId());

        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("HTML");
        edit.setReplyMarkup(markup);

        try {
            execute(edit);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при редактировании сообщения: {}", e.getMessage());
        }
    }

    private String formatSingleOrder(OrderResponse order, int currentNumber, int totalOrders) {
        StringBuilder sb = new StringBuilder();
        lionService.addContact(order);
        sb.append("🛒<b> ЗАКАЗ ").append(currentNumber).append(" ИЗ ").append(totalOrders).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🔗 ССЫЛКА НА ЗАКАЗ: <a href=\"https://lion.clicker.one/dashboard/orders/all/" + order.getId() + "/main\">" + order.getId() + "</a>\n");
        if (order.getVisitIntervals() != null) {
            for (VisitIntervalsResponse v : order.getVisitIntervals()) {
                sb.append(v.toString() + "\n");
            }
        } else {
            sb.append("Предложенные даты: <b>Отсутствуют</b>\n");
        }
        if (order.getProductDeliveryIntervals() != null) {
            for (ProductDeliveryIntervals p : order.getProductDeliveryIntervals()) {
                sb.append(p.toString() + "\n");
            }
        } else {
            sb.append("Дата доставки:<b>Отсутствуют</b>\n");
        }
        if (order.getCustomer() != null) {
            sb.append("📷 <b>ИМЯ</b>: " + order.getCustomer().getName() + "\n");
        }

        if (order.getWhatsapp() != null) {
            sb.append("💚 <b>WhatsApp</b>:" + order.getWhatsapp() + "\n");
        } else {
            sb.append("💚 <b>WhatsApp</b>: нет\n");
        }
        if (order.getPhone() != null) {
            sb.append("📱 <b>Телефон</b>: " + order.getPhone() + "\n");
        } else {
            sb.append("📱 <b>Телефон</b>: Отсутсвует\n");

        }
        for (ServiceItemResponse item : order.getServiceItems()) {
            sb.append("📅 Создан: ").append(formatDateTime(item.getCreatedAt())).append("\n");
            sb.append("🔄 Обновлен: ").append(formatDateTime(item.getUpdatedAt())).append("\n");

            if (item.getProviderReward() != null) {
                sb.append("💴 Цена : <b> ").append(item.getProviderReward().toString()).append("</b>\n");
            } else {
                sb.append("💴 Цена : <b> ").append("Не указана").append("</b>\n");
            }
            sb.append("🏷 Тип: <b>").append(order.getType().getName()).append("</b>\n");
            if (order.getAddress() != null) {
                sb.append("📍").append(order.getAddress().toString()).append("\n");
            } else {
                sb.append("📍").append("<b>Адресс не указан</b>").append("\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("🔧 <b>УСЛУГИ:</b>\n");


            sb.append("📌 ").append(item.getName()).append("\n");
            sb.append("   ").append(truncate(item.getDescription(), 1500, false)).append("\n");

            if (item.getNotIncludedText() != null && !item.getNotIncludedText().isEmpty()) {
                sb.append("⚠️ <b>НЕ ВКЛЮЧЕНО:</b>\n ").append(truncate(item.getNotIncludedText(), 2000, true)).append("\n");
            }
            sb.append("\n");

        }
        return sb.toString();
    }

    private InlineKeyboardMarkup createNavigationKeyboard(int currentIndex, int totalOrders, String orderId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();


        List<InlineKeyboardButton> navRow = new ArrayList<>();

        if (currentIndex > 0) {
            InlineKeyboardButton prevBtn = new InlineKeyboardButton();
            prevBtn.setText("◀️ НАЗАД");
            prevBtn.setCallbackData("order_prev_" + currentIndex);
            navRow.add(prevBtn);
        }

        InlineKeyboardButton counterBtn = new InlineKeyboardButton();
        counterBtn.setText((currentIndex + 1) + "/" + totalOrders);
        counterBtn.setCallbackData("noop");
        navRow.add(counterBtn);

        if (currentIndex < totalOrders - 1) {
            InlineKeyboardButton nextBtn = new InlineKeyboardButton();
            nextBtn.setText("ВПЕРЕД ▶️");
            nextBtn.setCallbackData("order_next_" + currentIndex);
            navRow.add(nextBtn);
        }

        rows.add(navRow);


        List<InlineKeyboardButton> blacklistRow = new ArrayList<>();
        InlineKeyboardButton blacklistBtn = new InlineKeyboardButton();
        blacklistBtn.setText("🚫 ДОБАВИТЬ В ЧЕРНЫЙ СПИСОК");
        blacklistBtn.setCallbackData("blacklist_add_" + orderId);
        blacklistRow.add(blacklistBtn);
        rows.add(blacklistRow);

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeBtn = new InlineKeyboardButton();
        closeBtn.setText(" ЗАКРЫТЬ");
        closeBtn.setCallbackData("close_orders");
        closeRow.add(closeBtn);
        rows.add(closeRow);

        markup.setKeyboard(rows);
        return markup;
    }

    private void addToBlacklist(String username, String orderId) {
        blockedTask.add(orderId);
        logger.info("Заказ {} добавлен в черный список пользователем {}", orderId, username);
    }

    private String formatDateTime(String isoDate) {
        try {
            String[] parts = isoDate.split("T");
            String date = parts[0].substring(5); // "04-27"
            String time = parts[1].substring(0, 5); // "09:40"
            return date + " " + time;
        } catch (Exception e) {
            return isoDate;
        }
    }

    private String truncate(String text, int maxLen, boolean formatted) {
        if (text == null) return "—";

        text = text.replace("*", "<b>*</b>");
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "…";
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void sendHelpMessage(long chatId) {
        sendMessage(chatId, HELP_TEXT);
    }

    private void sendMessageWithKeyboard(Long chatId, String textToSend, ReplyKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        if (textToSend != null && textToSend.length() > 0) {
            sendMessage.setText(textToSend);
        }
        sendMessage.setReplyMarkup(keyboard);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения с клавиатурой: {}", e.getMessage());
        }
    }


    private void sendNoPerm(Long chatId, String name) {
        String answer = "Привет " + name + ", у вас нет прав использовать бота";
        sendMessage(chatId, answer);
    }
}