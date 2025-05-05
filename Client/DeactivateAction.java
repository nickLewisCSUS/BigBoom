package Client;

import tage.GameObject;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.shapes.AnimatedShape;

public class DeactivateAction extends BTAction {
    private MyGame game;
    private AnimatedShape turretS;
    private boolean animationStarted = false;
    private float elapsedTime = 0f;
    private final float animationDuration = 3000f;

    public DeactivateAction(MyGame g) {
        game = g;
        turretS = (AnimatedShape) g.getTurret().getShape();
    }

    protected BTStatus update(float e) {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return BTStatus.BH_FAILURE;

        if (!animationStarted) {
            System.out.println("[DeactivateAction] Starting DEACTIVATE animation");
            turretS.playAnimation("DEACTIVATE", 3.0f, AnimatedShape.EndType.PAUSE, 0);
            animationStarted = true;
            elapsedTime = 0f;
            System.out.println(elapsedTime);
            return BTStatus.BH_RUNNING;
        }
        elapsedTime += e;
        System.out.println(elapsedTime);
        if (elapsedTime >= animationDuration) {
            System.out.println("[DeactivateAction] Deactivation animation completed, elapsedTime: " + elapsedTime);
            return BTStatus.BH_SUCCESS;
        }
        return BTStatus.BH_RUNNING; 
    }
}

