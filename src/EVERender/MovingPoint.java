package EVERender;

import com.jme3.math.Vector3f;

public class MovingPoint extends Star {
    private final Star dest;
    private final Star origin;
    private final Vector3f velocity;
    
    // distance to destination at last update
    private float prevDistance = Float.POSITIVE_INFINITY;
    
    // speed in units/second
    private static final float SPEED = 1/50f;
    
    /**
     * Creates a MovingPoint.
     * @param location the point's location
     * @param size the point's size
     * @param color the color of the point's sprite
     * @param dest the star this point is heading to
     * @param origin the star this point originated at
     */
    public MovingPoint (Vector3f location, float size, Vector3f color, Star dest, Star origin) {
        super(location, size, color);
        this.dest = dest;
        this.origin = origin;
        
        velocity = new Vector3f(
                dest.location.x - location.x,
                dest.location.y - location.y,
                dest.location.z - location.z).normalize().mult(SPEED);
    }
    
    /**
     * Updates this MovingPoint. Moves the sprite along the path set by this
     * point's velocity, and returns true if the sprite has reached its
     * desination and should be removed.
     * 
     * @param tpf delta-time in seconds
     * @return whether this point has reached its destination
     */
    @Override public boolean update(float tpf) {
        // calculates color by lerping the source star's color with destination
        /*Vector3f c = FastMath.interpolateLinear(
                location.distance(origin.location) / dest.location.distance(origin.location),
                origin.color,
                dest.color);*/
        
        // move the stored location and the sprite
        location = location.add(velocity.mult(tpf));
        sprite.move(velocity.mult(tpf));
        
        // should modify this point's color to the lerped color.
        // Does nothing atm. TODO: fix
        /*sprite.getMesh().setBuffer(VertexBuffer.Type.Color, 3, 
                BufferUtils.createFloatBuffer(color));*/
        
        // the point is at its destination once the distance to it has started increasing
        boolean atDestination = location.distance(dest.location) > prevDistance;
        prevDistance = location.distance(dest.location);
        
        return atDestination;
    }
}
