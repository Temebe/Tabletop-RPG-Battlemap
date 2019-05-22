package tests;

import controllers.BattlemapController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BattlemapControllerTests {
    BattlemapController battlemapController = new BattlemapController();

    @Test
    @DisplayName("When there were no players getPlayerNick throws exception")
    private void getPlayerNickThrowsExceptionWhenWrongPIDIsGiven() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> battlemapController.getPlayerNick(0));
    }
}
