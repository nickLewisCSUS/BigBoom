package tage;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;

import java.lang.Math;

/**
 * Manages a 3D orbiting camera around an avatar.
 * Allows the camera to orbit based on azimuth, elevation, and radius.
 * Supports input controls for modifying camera positioning dynamically.
 */
public class CameraOrbit3D {
    private Engine engine;
    private Camera camera;
    private GameObject avatar;
    private float cameraAzimuth;
    private float cameraElevation;
    private float cameraRadius;
    private final float minElevation = 5.0f;
    private final float maxElevation = 85.0f;
    private final float closeDistance = 1.0f;
    private final float farDistance = 50.0f;
    public static float overheadHeight = 10.0f;

    /**
     * Constructs a CameraOrbit3D object.
     * @param cam The camera object being controlled.
     * @param av The target avatar for the camera to orbit around.
     * @param gpName The name of the gamepad controller.
     * @param suffix Identifier for left or right viewport.
     * @param e The engine managing input and updates.
     */
    public CameraOrbit3D(Camera cam, GameObject av, String gpName, String suffix, Engine e){
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f;
        cameraElevation = 12.0f;
        cameraRadius = 10.0f;
        setupInputs(gpName, suffix);
        updateCameraPosition();
    }

    /**
     * Configures input controls for adjusting camera movement.
     * @param gp The gamepad name.
     * @param suffix Identifier for left or right viewport.
     */
    private void setupInputs(String gp, String suffix) {

        System.out.println("Available controllers:");
        for (Controller controller : engine.getInputManager().getControllers()) {
            System.out.println(" - " + controller);
        }

        OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
        OrbitElevationAction elevAction = new OrbitElevationAction();
        OrbitRadiusAction radAction = new OrbitRadiusAction();

        InputManager im = engine.getInputManager();
        if (gp != null) {
            // Left Viewport Controls
            if (suffix.equals("LEFT")) {
                im.associateAction(gp,
                        net.java.games.input.Component.Identifier.Axis.Z, azmAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                im.associateAction(gp,
                        net.java.games.input.Component.Identifier.Axis.RZ, elevAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                im.associateAction(gp,
                        net.java.games.input.Component.Identifier.Axis.Y, radAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                System.out.println("SUCCESS: Left Orbit Camera Inputs assigned.");
            }

            if (suffix.equals("RIGHT")) {
                ZoomOverheadCameraAction zoomAction = new ZoomOverheadCameraAction();
                im.associateAction(gp,
                        net.java.games.input.Component.Identifier.Axis.X, zoomAction,
                        InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                System.out.println("SUCCESS: Right Overhead Camera Zoom Inputs assigned.");
            }


        } else {
            System.out.println("WARNING: No gamepad detected. Skipping gamepad input setup.");
        }
    }

    /**
     * Updates the camera position relative to the avatar.
     * Converts spherical coordinates (azimuth, elevation, radius) into Cartesian coordinates.
     */
    public void updateCameraPosition() {
        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math.toDegrees((double) avatarRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));
        float totalAz = cameraAzimuth - (float)avatarAngle;
        double theta = Math.toRadians(totalAz);
        double phi = Math.toRadians(cameraElevation);

        float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float)(Math.sin(phi));
        float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));

        Vector3f newCamPos = new Vector3f(x, y, z).add(avatar.getWorldLocation());

        camera.setLocation(newCamPos);
        camera.lookAt(avatar);
    }

    /** Handles azimuth (horizontal rotation) input. */
    private class OrbitAzimuthAction extends AbstractInputAction
    { public void performAction(float time, Event event) {

        float rotAmount;
        if (event.getValue() < -0.2)
        { rotAmount=-1.5f; }
        else
        { if (event.getValue() > 0.2)
        { rotAmount=1.5f; }
        else
        { rotAmount=0.0f; }
        }
        cameraAzimuth += rotAmount;
        cameraAzimuth = cameraAzimuth % 360;

        updateCameraPosition();
    } }

    /** Handles elevation (vertical rotation) input. */
    private class OrbitElevationAction extends AbstractInputAction
    { public void performAction(float time, Event event) {

        float rotAmount;
        if (event.getValue() > -0.2)
        { rotAmount=-1.5f; }
        else
        { if (event.getValue() < 0.2)
        { rotAmount=1.5f; }
        else
        { rotAmount=0.0f; }
        }

        cameraElevation += rotAmount;

        if (cameraElevation < minElevation) cameraElevation = minElevation;
        if (cameraElevation > maxElevation) cameraElevation = maxElevation;

        updateCameraPosition();
    } }

    /** Handles zooming in and out by modifying camera radius. */
    private class OrbitRadiusAction extends AbstractInputAction
    { public void performAction(float time, Event event) {

        float scrollAmount = event.getValue();  // Get the scroll amount

        // Scroll up (positive value) -> Zoom in (decrease radius)
        // Scroll down (negative value) -> Zoom out (increase radius)
        if (scrollAmount < 0) {
            cameraRadius -= 0.2f;
        } else if (scrollAmount > 0) {
            cameraRadius += 0.2f;
        }

        // Enforce min/max distance limits
        if (cameraRadius < closeDistance) cameraRadius = closeDistance;
        if (cameraRadius > farDistance) cameraRadius = farDistance;

        updateCameraPosition();
    } }

    /** Handles zooming of the overhead camera view. */
    private class ZoomOverheadCameraAction extends AbstractInputAction {
        @Override
        public void performAction(float time, Event event) {
            float scrollAmount = event.getValue();

            // Zoom in (scroll up) decreases overheadHeight
            if (scrollAmount < -0.2) {
                overheadHeight -= 0.5f;
            }
            // Zoom out (scroll down) increases overheadHeight
            else if (scrollAmount > 0.2) {
                overheadHeight += 0.5f;
            }

            // Limit zoom range
            if (overheadHeight < 3.0f) overheadHeight = 3.0f;
            if (overheadHeight > 30.0f) overheadHeight = 30.0f;

            System.out.println("Overhead Camera Zoom Level: " + overheadHeight);
        }
    }
}
