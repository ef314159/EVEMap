package EVERender;
 
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

/**
 * Extension of JME's ChaseCamera that modifies how scrolling is done.
 * 
 * Instead of scrolling out/in a given distance, LogChaseCamera scrolls in/out
 * on a log scale, resulting in more intuitive control of the camera distance.
 */
class LogChaseCamera extends ChaseCamera {
    public LogChaseCamera(Camera cam, Spatial target, InputManager inputManager) {
        super(cam, target, inputManager);
    }
    
    @Override protected void zoomCamera(float value) {
        if (!enabled) {
            return;
        }
        
        zooming = true;
        targetDistance *= Math.pow(2, value * zoomSensitivity);
        if (targetDistance > maxDistance) {
            targetDistance = maxDistance;
        }
        if (targetDistance < minDistance) {
            targetDistance = minDistance;
        }
        if (veryCloseRotation) {
            if ((targetVRotation < minVerticalRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVerticalRotation;
            }
        }
    }
    
    public Camera getCamera() {
        return cam;
    }
}
