package EVERender;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.SimpleBatchNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a galaxy map. Stores star/jumpgate data and updates the animated
 * parts of the map.
 */
public class Galaxy{
    private Map<Integer, Star> stars = new HashMap<Integer, Star>();
    private LinkedList<Star> points = new LinkedList<Star>();
    private Map<Star, List<Star>> gates = new HashMap<Star, List<Star>>();

    private SimpleBatchNode galaxy;
    public final Geometry skybox;
    private EveRender main;

    /**
     * Creates a Galaxy. Takes a reference back to an EveRender instance to
     * load data.
     * 
     * @param main an EveRender instance
     */
    public Galaxy(EveRender main) {
        this.main = main;

        main.loadSettings();
        stars = main.loadStarData();
        gates = main.loadGateData(stars);

        galaxy = new SimpleBatchNode();
        createEveMap();
        createParallaxStars(2048);
        galaxy.batch();

        main.universe.attachChild(galaxy);
        skybox = main.loadSkybox("textures/starmap.dds");
    }

    /**
     * Creates the EVE map. uses star and gate data to create sprite and line
     * meshes. Attaches these meshes to main.universe.
     */
    private void createEveMap() {
        for (Star s : stars.values()) {
            Geometry g = main.createSprite(
                    s.color, s.location, s.size, "textures/star2d.dds");
            galaxy.attachChild(g);
            s.sprite = g;
        }

        List<Vector3f> edgesList = new ArrayList<Vector3f>();

        for (Star s1 : gates.keySet()) {
            for (Star s2 : gates.get(s1)) {
                edgesList.add(s1.location);
                edgesList.add(s2.location);
            }
        }

        Vector3f[] edges = new Vector3f[edgesList.size()];

        for (int i = 0; i < edges.length; ++i) {
            edges[i] = edgesList.get(i);
        }

        Geometry g = main.createLine(main.lineColor, edges);
        main.universe.attachChild(g);
    }

    /**
     * Creates more 3d stars for a slight parallax effect in front of the
     * skybox.
     * 
     * @param numStars the number of stars to add
     */
    private void createParallaxStars(int numStars) {
        for (int i = 0; i < numStars; ++i) {
            float radius = FastMath.nextRandomFloat()*16 + 2;
            radius *= radius;
            float theta = FastMath.nextRandomFloat()*FastMath.TWO_PI;
            float phi = FastMath.nextRandomFloat()*FastMath.TWO_PI;

            Vector3f position = new Vector3f(
                    radius * FastMath.sin(theta) * FastMath.cos(phi),
                    radius * FastMath.sin(theta) * FastMath.sin(phi),
                    radius * FastMath.cos(theta)
                    );

            float size = FastMath.nextRandomFloat()*1.5f*FastMath.sqrt(radius);
            size *= size;

            Geometry g = main.createSprite(
                    Vector3f.UNIT_XYZ,
                    position,
                    size,
                    "textures/star2d.dds");
            main.universe.attachChild(g);
        }
    }

    /**
     * Sets the kills per hour of a given star.
     * 
     * @param id the ID of the star
     * @param kills the kills per hour to set
     */
    public void setKillsPerHour(int id, int kills) {
        Star s = stars.get(id);
        if (s != null) s.shipKills = kills;
    }

    /**
     * Sets the jumps per hour of a given star.
     * 
     * @param id the ID of the star
     * @param kills the jumps per hour to set
     */
    public void setJumpsPerHour(int id, int jumps) {
        Star s = stars.get(id);
        if (s != null) s.jumps = jumps;
    }

    /**
     * Updates the map. Adds moving and flashing points, updates existing ones,
     * removes tham if necessary.
     * 
     * @param tpf delta-time in seconds
     */
    public void update(float tpf) {
        List<Star> toRemove = new LinkedList<Star>();
        for (Star s : points) {
            if (s.update(tpf)) {
                galaxy.detachChild(s.sprite);
                toRemove.add(s);
            }
        }

        List<Star> toAdd = new LinkedList<Star>();

        /*
         * tpf is delta-t in seconds. s.shipKills and s.jumps are the number of
         * ship kills and jumps per hour on TQ. The number of kills and jumps
         * that happen in a single tpf (and therefore the probability of one
         * being generated) is [kills or jumps]/60/60*tpf.
         */
        for (Star s : stars.values()) {
            if (FastMath.nextRandomFloat() < s.shipKills/60f/60f*tpf*main.SPEEDUP_KILLS) {
                toAdd.add(createFlashingPoint(s));
            }
        }

        for (Star s : stars.values()) {
            if (FastMath.nextRandomFloat() < s.jumps/60f/60f*tpf*main.SPEEDUP_JUMPS) {
                List<Star> destinations = gates.get(s);
                if (destinations != null) {
                    int random = FastMath.nextRandomInt(0, destinations.size()-1);
                    toAdd.add(createMovingPoint(s, destinations.get(random)));
                }
            }
        }

        for (Star s : toAdd) {
            points.add(s);
        }

        for (Star s : toRemove) {
            points.remove(s);
        }
    }

    /**
     * Creates a flashing point representing a kill in a given system.
     * 
     * @param s the star at which to create the point
     * @return the new point
     */
    private Star createFlashingPoint(Star s) {
        FlashingPoint p = new FlashingPoint(
                s.location,
                s.size*0.25f+0.25f,
                new Vector3f(s.color.x, s.color.y, s.color.z));

        Geometry g = main.createSprite(
                p.color, p.location, p.size, "textures/flashingpoint.dds");
        galaxy.attachChild(g);
        p.sprite = g;
        return p;
    }

    /**
     * Creates a moving point representing a jump from one system to another.
     * 
     * @param s the star at which to create the point
     * @param dest the star towards which to travel
     * @return the new point
     */
    private Star createMovingPoint(Star s, Star dest) {
        MovingPoint p = new MovingPoint(
                s.location,
                FastMath.nextRandomFloat()*.1f + .1f,
                new Vector3f(s.color.x, s.color.y, s.color.z),
                dest,
                s);

        Geometry g = main.createSprite(
                p.color, p.location, p.size, "textures/movingpoint.dds");
        galaxy.attachChild(g);
        p.sprite = g;
        return p;
    }
}
