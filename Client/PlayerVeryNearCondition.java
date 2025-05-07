package Client;

import tage.ai.behaviortrees.BTCondition;
import tage.GameObject;
import org.joml.Vector3f;

import Client.TurretAIController.TurretState;

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
        turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return false;

        float dist = closest.getWorldLocation().distance(turret.getWorldLocation());
        boolean inRange = (dist <= 10.0f);
        System.out.println("Previous State: " + controller.getPreviousState());
        if (inRange && controller.getPreviousState() != TurretAIController.TurretState.VERY_NEAR) {
            controller.setPreviousState(TurretState.VERY_NEAR);
            activateAction.setScanActivationStarted(false);
            System.out.println("DEBUG [PlayerVeryNearCondition]: Closest distance = " + dist + ", In range: " + (dist <= 10.0f));
        }
        return inRange;
    }
}
