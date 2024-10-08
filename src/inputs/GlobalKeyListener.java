package inputs;

import components.ShowPlanets;
import main.Game;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * author: ahror
 * <p>
 * since: 8/31/24
 */
public class GlobalKeyListener implements KeyEventDispatcher {
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) handleKeyPress(e);
        return false;
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_Q) Game.gameWindow.showHomePanel();

    }

}
