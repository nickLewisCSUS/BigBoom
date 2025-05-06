package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.GameObject;
import org.joml.Vector3f;

public class PlayerVeryNearCondition extends BTCondition {
    private MyGame game;
    private GameObject turret;
    private ActivateAction activateAction;
    private TurretAIController controller;

    public PlayerVeryNearCondition(boolean toNegate, MyGame game, GameObject turret,TurretAIController controller, ActivateAction activateAction) {
        super(toNegate);
        this.game = game;
        this.turret = turret; 
        this.controller = controller;
        this.activateAction = activateAction;
    }

    protected boolean check() {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return false;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        boolean inRange = (dist <= 10.0f);
        if (inRange && controller.getPreviousState() != TurretAIController.TurretState.FAR) {
            activateAction.setScanActivationStarted(false);
            System.out.println("DEBUG [PlayerVeryNearCondition]: Closest distance = " + dist + ", In range: " + (dist <= 10.0f));
        }
        return inRange;
    }
}
