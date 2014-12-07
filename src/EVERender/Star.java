package EVERender;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

/**
 * Represents a point in space, with a location, size, color and unique ID.
 * Also contains a sprite object for rendering and data for kills and jumps
 * in a given star system, as read from EVE's API.
 * 
 * TODO: Move some of this into another subclass, existing subclasses
 * MovingPoint and FlashingPoint don't need an id or EVE data
 */
public class Star {
    public Vector3f location;
    public Vector3f color;
    public float size;
    public int id;
    
    protected Geometry sprite;
    
    public int shipKills = 0;
    public int jumps = 0;
    
    /**
     * Creates a Star.
     * @param id the EVE id of the system, or zero
     * @param location the star's location
     * @param size the size of the star's sprite
     * @param color the color of the star's sprite
     */
    public Star (int id, Vector3f location, float size, Vector3f color) {
        this.id = id;
        this.location = location;
        this.size = size;
        this.color = color;
    }
    
    /**
     * Creates a Star with an ID of zero. Used by subclasses.
     * @param location the star's location
     * @param size the size of the star's sprite
     * @param color the color of the star's sprite
     */
    protected Star (Vector3f location, float size, Vector3f color) {
        this(0, location, size, color);
    }
    
    /**
     * Updates the Star. Does nothing, as Stars are static unless subclassed.
     * @param tpf delta-time in seconds
     * @return always false
     */
    public boolean update(float tpf) {
        return false;
    }
}
