package t.bot.worker.service.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import t.bot.worker.service.clicker.LionServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class SettingBotMenu {
    public static ReplyKeyboardMarkup getSettingBotMenu() {
        KeyboardRow row2 = new KeyboardRow();
        if(LionServiceImpl.enabled){
            row2.add(new KeyboardButton("Выключить"));
        }else{
            row2.add(new KeyboardButton("Включить"));
        }
        row2.add(new KeyboardButton("Главное меню"));
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row2);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;
    }

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Текущие заказы"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Статус бота"));
        row2.add(new KeyboardButton("Настройки"));


        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        return replyKeyboardMarkup;
    }
}
