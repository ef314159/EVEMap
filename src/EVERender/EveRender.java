package EVERender;
 
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The main class of the EVE map renderer. Loads data from assets and sets up
 * the Galaxy and application state. Handles input.
 */
public class EveRender extends SimpleApplication implements RawInputListener {
    public Node universe;
    private LogChaseCamera camera;
    
    public float SPEEDUP_JUMPS = 1.0f;
    public float SPEEDUP_KILLS = 1.0f;
    
    private Galaxy g;
    private APIScraper apiScraper;
    
    public Vector3f
            nullColor1,
            nullColor2,
            lowColor1,
            lowColor2,
            highColor1,
            highColor2,
            lineColor;
    
    // whether to exit as soon as any input is recieved
    private boolean screensaver = false;
 
    public static void main(String[] args){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screenSize.getWidth();
        int height = (int)screenSize.getHeight();
        
        EveRender app = new EveRender();
        AppSettings s = new AppSettings(true);
        s.setFullscreen(true);
        s.setSamples(16);
        s.setWidth(width);
        s.setHeight(height);
        app.setSettings(s);
        app.setShowSettings(false);
        app.start();
    }
    
    /**
     * Called when ending the program. Terminates the API scraper if it's still
     * running.
     */
    @Override public void destroy() {
        apiScraper.terminate();
    }

    /**
     * JME method to initialize the app. Called by JME.
     */
    @Override public void simpleInitApp() {
        // remove default flyCam
        stateManager.detach( stateManager.getState(FlyCamAppState.class));
        
        // set up universe node to contain everything except skybox
        universe = new Node();
        rootNode.attachChild(universe);
        inputManager.setCursorVisible(false);
        
        setUpCamera();
        setUpHud();
        
        g = new Galaxy(this);
        apiScraper = new APIScraper(g);
        apiScraper.start();
        
        inputManager.setCursorVisible(false);
        inputManager.addRawInputListener(this);
    }
    
    private void setUpCamera() {
        camera = setCameraDefaults(new LogChaseCamera(cam, rootNode, inputManager));
        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 1/64f, 1024);
    }
    
    private void setUpHud() {
        // disable JME debug stuffs
        setDisplayFps(false);
        setDisplayStatView(false);
    }
    
    private LogChaseCamera setCameraDefaults(LogChaseCamera camera) {
        camera.setRotationSpeed(3);
        camera.setZoomSensitivity(0.1f);
        camera.setDefaultDistance(0.7f);
        camera.setInvertVerticalAxis(true);
        camera.setMinVerticalRotation((float)(-FastMath.HALF_PI + 0.001));
        camera.setMinDistance(0.1f);
        camera.setMaxDistance(4);
        camera.setDefaultVerticalRotation(0.5f);
        
        return camera;
    }

    /**
     * JME method updating the app. Centers the skybox on the camera, then
     * calls Galaxy's update().
     * 
     * @param tpf delta-time in seconds
     */
    @Override public void simpleUpdate(float tpf) {
        camera.update(tpf);
        g.skybox.setLocalTranslation(camera.getCamera().getLocation());
        g.skybox.setLocalRotation(universe.getLocalRotation());
        
        universe.rotate(0, tpf*FastMath.DEG_TO_RAD, 0);
        g.update(tpf);
    }
    
    /*
     * MODELLING METHODS - update the galaxy map's model
     */
    
    /**
     * Creates a point sprite and returns it. Does not attach it to any nodes.
     * Sprites are rendered unshaded with additive blend mode.
     * 
     * @param color the sprite's color
     * @param position the sprite's position
     * @param size the sprite's radius
     * @param texture the sprite's texture
     * @return a Geometry object containing the sprite
     */
    public Geometry createSprite(Vector3f color, Vector3f position,
            float size, String texture) {
        Mesh spriteMesh = new Mesh();
        spriteMesh.setMode(Mesh.Mode.Points);

        spriteMesh.setBuffer(VertexBuffer.Type.Normal, 3,
                BufferUtils.createFloatBuffer(Vector3f.ZERO));
        spriteMesh.setBuffer(VertexBuffer.Type.Color, 3, 
                BufferUtils.createFloatBuffer(color));
        spriteMesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(new Vector3f(0f, 0f, 0f)));
        spriteMesh.setBuffer(VertexBuffer.Type.TexCoord, 4,
                BufferUtils.createFloatBuffer(new Vector4f(0.0f, 0.0f, 1.0f, 1.0f)));
        spriteMesh.setBuffer(VertexBuffer.Type.Size, 1,
                BufferUtils.createFloatBuffer(size));

        Geometry spriteGeo = new Geometry("Star", spriteMesh);
        spriteGeo.setLocalTranslation(position);

        Material spriteMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        spriteMat.setTexture("Texture", assetManager.loadTexture(texture));
        spriteMat.setFloat("Quadratic", 20f);
        spriteMat.setBoolean("PointSprite", true);
        spriteMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        spriteMat.getAdditionalRenderState().setDepthTest(false);

        spriteGeo.setMaterial(spriteMat);
        
        return spriteGeo;
    }
    
    /**
     * Creates a line mesh and returns it. Does not attach it to any nodes.
     * Lines are rendered unshaded with additive blend mode. Mesh array must
     * have even size, with each pair of Vector3f's representing a single line.
     * 
     * For better performance, the entire jumpgate mesh is created as a single
     * mesh with a single call to createLine().
     * 
     * @param color the line's color
     * @param verts pairs of endpoints for each line in the mesh
     * @return the Geometry representing the line mesh
     */
    public Geometry createLine(Vector3f color, Vector3f[] verts) {
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);

        lineMesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(verts));

        Geometry spriteGeo = new Geometry("Line", lineMesh);

        Material lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", new ColorRGBA(color.x, color.y, color.z, .02f));
        lineMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        lineMat.getAdditionalRenderState().setDepthTest(false);

        spriteGeo.setMaterial(lineMat);
        
        return spriteGeo;
    }
    
    /**
     * Creates a skybox with the given texture and attaches it to rootNode.
     * Skybox is rendered unshaded using additive blend mode.
     * 
     * @param texture the texture to use
     * @return the Geometry representing the skybox
     */
    public Geometry loadSkybox(String texture) {
        Box cube = new Box(16f, 16f, 16f);
        Geometry sky = new Geometry("Sky", cube);
        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture stars = assetManager.loadTexture(texture);
        
        skyMat.setTexture("ColorMap", stars);
        skyMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        skyMat.getAdditionalRenderState().setDepthTest(false);
        skyMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
        sky.setMaterial(skyMat);
        
        rootNode.attachChild(sky);
        
        return sky;
    }
    
    /*
     * LOADING METHODS - get text data from files
     */
    
    /**
     * Turns this instance into a screensaver (instructs the camera to hide the
     * mouse cursor)
     */
    private void makeScreensaver() {
        screensaver = true;
        camera.setDragToRotate(false);
    }
    
    /**
     * Loads settings from config.txt. Defines star/line colors and speed of
     * jump/kill simulation.
     */
    public void loadSettings() {
        ArrayList<String> settingsText = null;
        try {
            Path path = Paths.get("config.txt");
            settingsText = (ArrayList<String>)Files.readAllLines(
                    path, StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            System.out.println("IOException when reading config.txt");
        }
        
        for (String line : settingsText) {
            int commentIndex = line.indexOf("//");
            if (commentIndex >= 0) line = line.substring(0, commentIndex);
            
            String[] tokens = line.split(":");
            if (tokens.length < 2) continue;
            
            String type = tokens[0].toLowerCase().trim();
            
            if (type.equals("nullsec") || type.equals("lowsec") || type.equals("highsec")) {
                String[] colors = tokens[1].split(";");
                if (colors.length != 2) continue;
                
                if (type.equals("nullsec")) {
                    nullColor1 = parseColor(colors[0]);
                    nullColor2 = parseColor(colors[1]);
                } else if (type.equals("lowsec")) {
                    lowColor1 = parseColor(colors[0]);
                    lowColor2 = parseColor(colors[1]);
                } else if (type.equals("highsec")) {
                    highColor1 = parseColor(colors[0]);
                    highColor2 = parseColor(colors[1]);
                }
            } else if (type.equals("lines")) {
                lineColor = parseColor(tokens[1]);
            } else if (type.equals("jumpspeed")) {
                SPEEDUP_JUMPS = Float.parseFloat(tokens[1]);
            } else if (type.equals("killspeed")) {
                SPEEDUP_KILLS = Float.parseFloat(tokens[1]);
            } else if (type.equals("screensaver")) {
                if(parseBool(tokens[1])) makeScreensaver();
            }
        }
    }
    
    private boolean parseBool(String in) {
        in = in.trim().toLowerCase();
        
        if (in.equals("true") ||
                in.equals("yes") ||
                in.equals("hija'") || // Klingon
                in.equals("hislah")) return true;
        
        if (in.equals("false") ||
                in.equals("no") ||
                in.equals("ghobe'")) return false;
        
        return false;
    }
    
    /**
     * Parses a single color with the format "%f,%f,%f". Returns null if a
     * NumberFormatException was raised.
     * 
     * @param in the string to parse
     * @return the parsed color, or null
     */
    private Vector3f parseColor(String in) {
        String[] rgb = in.split(",");
        if (rgb.length != 3) return null;
        
        Vector3f color;
        
        try {
            color = new Vector3f(
                    Float.parseFloat(rgb[0]),
                    Float.parseFloat(rgb[1]),
                    Float.parseFloat(rgb[2]));
        } catch(NumberFormatException e) {
            color = null;
        }
        
        return color;
    }
    
    /**
     * Loads star data from systems.txt, copied from EVE's static data dump.
     * Returns EVE's IDs mapped to Stars. Uses vars populated in loadColorData(),
     * so should be called after it.
     * 
     * @return the Map of IDs to stars
     */
    public Map<Integer, Star> loadStarData() {
        ArrayList<String> starsText = null;
        Map<Integer, Star> stars = new HashMap<Integer, Star>();
        
        try {
            Path path = Paths.get("assets/systems.txt");
            starsText = (ArrayList<String>)Files.readAllLines(
                    path, StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            System.out.println("IOException when reading systems.txt");
        }
        
        for (String line : starsText) {
            String[] tokens = line.split("\\s");
            Vector3f location = null;
            float size = 0;
            float security = 0;
            int id = 0;
            
            float positionFactor = FastMath.pow(2, 60);
            float sizeFactor = .02f;
            
            try {
                location = new Vector3f(
                        Float.parseFloat(tokens[2])/positionFactor + 0.075f,
                        Float.parseFloat(tokens[3])/positionFactor,
                        Float.parseFloat(tokens[4])/positionFactor);
                
                security = Float.parseFloat(tokens[6]);
                
                id = Integer.parseInt(tokens[0]);
                
                size = Float.parseFloat(tokens[5])*Float.parseFloat(tokens[7]);
                size = FastMath.pow(size,0.125f)*sizeFactor;
            }
            catch (NumberFormatException e) { }
            
            // exclude invalid positions and w-space
            // (offset from the origin in a group by itself)
            if (location != null && location.x-location.z < 5) {
                Vector3f color;
                
                if (security < 0f) {
                    color = FastMath.interpolateLinear(security+1, nullColor1, nullColor2);
                } else if (security < 0.5f) {
                    color = FastMath.interpolateLinear(security*2, lowColor1, lowColor2);
                } else {
                    color = FastMath.interpolateLinear((security-.5f)*2, highColor1, highColor2);
                }
                
                stars.put(id, new Star(id, location, size, color));
            }
        }
        
        return stars;
    }
    
    /**
     * Given a Map of stars from loadStarData() and the assets/gates.txt file,
     * generates a list of destinations for each star representing jumpgates.
     * 
     * @param stars the Map of stars returned by loadStarData()
     * @return a Map of stars to lists of destinations
     */
    public Map<Star, List<Star>> loadGateData(Map<Integer, Star> stars) {
        ArrayList<String> gatesText = null;
        Map<Star, List<Star>> gates = new HashMap<Star, List<Star>>();
        
        try {
            Path path = Paths.get("assets/gates.txt");
            gatesText = (ArrayList<String>)Files.readAllLines(
                    path, StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            System.out.println("IOException when reading gates.txt");
        }
        
        for (String line : gatesText) {
            String[] tokens = line.split("\\s");
            
            int id1 = 0, id2 = 0;
            try {
                id1 = Integer.parseInt(tokens[0]);
                id2 = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) { }
            
            Star s1 = stars.get(id1), s2 = stars.get(id2);
            
            if (s1 != null && s2 != null) {
                if (gates.get(s1) == null) {
                    gates.put(s1, new LinkedList<Star>());
                }
                gates.get(s1).add(s2);
                
                if (gates.get(s2) == null) {
                    gates.put(s2, new LinkedList<Star>());
                }
                gates.get(s2).add(s1);
            }
        }
        
        return gates;
    }
    
    /*
     * INPUT FUNCTIONS
     */

    public void beginInput() { }
    public void endInput() { }

    public void onJoyAxisEvent(JoyAxisEvent evt) {
        if (screensaver) this.stop();
    }

    public void onJoyButtonEvent(JoyButtonEvent evt) {
        if (screensaver) this.stop();
    }

    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (screensaver) this.stop();
    }

    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (screensaver) this.stop();
    }

    public void onKeyEvent(KeyInputEvent evt) {
        if (screensaver) this.stop();
    }

    public void onTouchEvent(TouchEvent evt) {
        if (screensaver) this.stop();
    }
}