package Client;

import org.joml.Vector3f;

import tage.GameObject;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.shapes.AnimatedShape;

public class TrackPlayerAction extends BTAction {
    private MyGame game;
    private AnimatedShape turretS;
    private boolean animationStarted = false;
    private float elapsedTime = 0f;
    private final float animationDuration = 480f;

    public TrackPlayerAction(MyGame g) {
        game = g;
        turretS = (AnimatedShape) g.getTurret().getShape();
    }

    protected BTStatus update(float e) {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return BTStatus.BH_FAILURE;
        System.out.println("[TrackPlayerAction] Rotating turret to face closest player");
        Vector3f playerLoc = closest.getWorldLocation();
        turret.lookAt(playerLoc);
        return BTStatus.BH_SUCCESS;
    }
}

