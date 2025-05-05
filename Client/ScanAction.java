package Client;

import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;
import tage.shapes.AnimatedShape;

public class ScanAction extends BTAction {
    private AnimatedShape turretS;
    private boolean started = false;

    public ScanAction(MyGame g) {
        game = g;
        turretS = (AnimatedShape) g.getTurret().getShape();
    }

    protected BTStatus update(float e) {
        if (!started) {
            System.out.println("[ScanAction] Playing SCAN animation (loop)");
            turretS.stopAnimation();
            turretS.playAnimation("SCAN", 1.0f, AnimatedShape.EndType.LOOP, 0);
            started = true;
        }
        return BTStatus.BH_SUCCESS;
    }

    public void reset() {
        started = false;
    }
}

