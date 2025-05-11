package Client;

import tage.Engine;
import tage.GameObject;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.shapes.AnimatedShape;
import tage.nodeControllers.RotationController;
import org.joml.Vector3f;

public class ActivateAction extends BTAction {
    private MyGame game;
    private Engine engine;
    private AnimatedShape turretS;
    private boolean activateAnimationStarted = false;
    private boolean scanAnimationStarted = false;
    private AnimatedShape.EndType scanEndType = AnimatedShape.EndType.LOOP;
    private float elapsedTime = 0f;
    private final float animationDuration = 180f;

    public ActivateAction(MyGame g) {
        this.game = g;
        this.engine = g.getEngine();  
        if (game.getUseAnimations()) {
            turretS = (AnimatedShape) g.getTurret().getShape();
        }
    }

    protected BTStatus update(float e) {
        GameObject turret = game.getTurret();
        GameObject closest = game.getClosestAvatar(turret);
        if (closest == null) return BTStatus.BH_FAILURE;

        
        System.out.println(elapsedTime);
        if (!activateAnimationStarted) {
            //System.out.println("[ActivateAction] Starting ACTIVATE animation");  
            if (game.getUseAnimations()) {
                turretS.playAnimation("ACTIVATE", 3.0f, AnimatedShape.EndType.PAUSE, 0);
            }
            activateAnimationStarted = true;
            elapsedTime = 0f;
            System.out.println(elapsedTime);
        }
        elapsedTime += e;
        System.out.println(elapsedTime);
        if (elapsedTime >= animationDuration) {
            if (!scanAnimationStarted) {
                //System.out.println("[ActivateAction] Activation animation completed, elapsedTime: " + elapsedTime);
                if (game.getUseAnimations()) {
                    turretS.playAnimation("SCAN", 3.0f, scanEndType, 0);
                }
                scanAnimationStarted = true;
            }
            return BTStatus.BH_SUCCESS;
        }
        return BTStatus.BH_RUNNING;
    }

    public boolean getActivateAnimationStarted() {
        return activateAnimationStarted;
    }

    public void setActivateAnimationStarted(boolean started) {
        this.activateAnimationStarted = started;
    }

    public boolean getScanActivationStarted() {
        return scanAnimationStarted;
    }

    public void setScanActivationStarted(boolean started) {
        this.scanAnimationStarted = started;
    }
}
