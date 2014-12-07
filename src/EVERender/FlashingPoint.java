package EVERender;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * A quickly disppearing rendered in the galaxy map. Represents player kills.
 */
public class FlashingPoint extends Star {
    // remaining time
    float life;
    
    /**
     * Creates a FlashingPoint.
     * @param location the point's location
     * @param size the point's size
     * @param color the color of the point's sprite
     */
    public FlashingPoint (Vector3f location, float size, Vector3f color) {
        super(location, size, color);
        this.life = 1.0f;
    }
    
    /**
     * Updates this FlashingPoint. modifies the sprite's color to make the
     * point disappear over time. Returns true if this point has disappeared
     * and should be removed.
     * 
     * @param tpf delta-time in seconds
     * @return whether this point has disappeared
     */
    @Override public boolean update(float tpf) {
        life -= tpf*4;
        sprite.getMesh().setBuffer(VertexBuffer.Type.Color, 3, 
                BufferUtils.createFloatBuffer(new Vector3f(
                color.x*life, color.y*life, color.z*life)));
        
        return (life < 0);
    }
}
