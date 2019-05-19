package tests;

import controllers.BattlemapController;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import network_interface.Chat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ChatTests {
    private TextField chatField = new TextField();
    private Chat chat = new Chat(new AnchorPane(), new ScrollPane(), chatField, new BattlemapController(), 0);

    @ParameterizedTest
    @DisplayName("Throws at improper argument")
    @CsvSource({"3", "3da", "cac"})
    void throwWhenImproperArgumentsGivenForRollCommand(String arguments) {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                chat.receiveMessage("/roll " + arguments, 0));
    }

    @ParameterizedTest(name = "{0} is integer")
    @CsvSource({"313", "139", "99991999291939123992392193"})
    void isIntegerReturnsTrueWhenGivenInteger(String integer) {
        Assertions.assertTrue(Chat.isInteger(integer));
    }

    @ParameterizedTest(name = "{0} is not an integer")
    @CsvSource({"a2a", "alabama", "0120"})
    void isIntegerReturnsFalseWhenGivenNotInteger(String notInteger) {
        Assertions.assertFalse(Chat.isInteger(notInteger));
    }
}
