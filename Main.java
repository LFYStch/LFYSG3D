import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;




public class Main implements KeyListener {
    private dP d; 

    public Main() {
        JFrame w = new JFrame();
        d = new dP();
        w.setTitle("3D Test");
        w.setSize(500, 500);
        w.setResizable(true);
        w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        w.add(d);
        w.setVisible(true);
        w.addKeyListener(this);

        new javax.swing.Timer(50, e -> d.update()).start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_F5:
                System.out.println("");
                d.debug = !d.debug;
                break;
           
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        new Main();
    }
}

class dP extends JPanel {
    
    private AnimatedObject animatedCube;
    private boolean animationPlaying = false;
   
    private GameObject cube1;
    private GameObject cube2;
    public Graphics2D g2d;
    vec3 cam;
    double camYaw, camPitch;
    vec3 light_source1;
    int totalTris;
    boolean debug = false;
    BufferedImage texture1;
    spawner sp;
    float alpha = 0.5f;
    
   
    Objloader loader = new Objloader();

    public dP() {
        setDoubleBuffered(true);
        cam = new vec3(0, 0, -30, 0, 0);
        cam.nx = cam.x + Math.cos(camYaw);cam.ny = cam.y + Math.cos(camPitch);cam.nz = cam.z + Math.sin(camYaw) + 10;
        light_source1 = new vec3(cam.x, cam.y, cam.z - 2, 0, 0);
        camYaw = 0;
        camPitch = 0;
        loadTextures();
        sp = new spawner();
        this.setupExample();
    }

    public void loadTextures() {
        try {
            texture1 = ImageIO.read(new File("dir.png"));
            
        } catch (IOException e) {
            System.err.println("Texture load failed.");
            e.printStackTrace();
        }
    }
    
    
   public void setupExample() {
    
    cube1 = new GameObject(
        new mesh[]{
            loader.load("Cube.obj", 0, 0, 20, 0.1, 0.1, 0.5)
        },
        new AABB(new vec3(0, 0, 0, 0, 0), new vec3(0, 0, 0, 0, 0)),
        0, 0, -5, 0, 0  
    );

    cube2 = new GameObject(
        new mesh[]{
            loader.load("Cube.obj", 0, 0, 20, 0.1, 0.1, 0.5)
        },
        new AABB(new vec3(0, 0, 0, 0, 0), new vec3(0, 0, 0, 0, 0)),
        0, 0, 5, 0, 0   
    );

   
    vec3[][] animPaths = new vec3[2][];
    
    
    animPaths[0] = new vec3[]{
        new vec3(-3, -2, 20, 0, 0),
        new vec3(-3, 2, 20, Math.PI, 0),
        new vec3(-3, -2, 20, 2*Math.PI, 0)
    };
    
    
    animPaths[1] = new vec3[]{
        new vec3(3, -2, 20, 0, 0),
        new vec3(3, 2, 20, Math.PI, 0),
        new vec3(3, -2, 20, 2*Math.PI, 0)
    };

    Animation combinedAnimation = new Animation(
        new GameObject[]{cube1, cube2}, 
        animPaths
    );

    animatedCube = new AnimatedObject(
        new Animation[]{combinedAnimation},
        new GameObject[]{cube1, cube2}
    );
}
    //Main drawloop starts here! :)
    @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g2d = (Graphics2D) g;
    
    // Draw all animated objects
    for (GameObject obj : animatedCube.Objects) {
            if (obj != null) {
                mesh currentMesh = obj.getMesh(0);
                if (currentMesh != null) {
                    drawMesh(currentMesh, g2d, texture1);
                }
            }
        }
}
    //No edits past here! >:(
   public void drawMesh(mesh ts, Graphics2D g2d, BufferedImage texture) {
    java.util.List<tri> sortedTris = new java.util.ArrayList<>();
    for (tri[] strip : ts.tris) {
        Collections.addAll(sortedTris, strip);
    }

    // Sort triangles by average Z depth (back-to-front)
    sortedTris.sort((a, b) -> {
        double za = (a.v1.z + a.v2.z + a.v3.z) / 3.0;
        double zb = (b.v1.z + b.v2.z + b.v3.z) / 3.0;
        return Double.compare(zb, za);
    });

    vec3 lightDir = new vec3(0, 0, -1, 0, 0); // Light from camera direction

    for (tri t : sortedTris) {
        double nx = (t.v1.nx + t.v2.nx + t.v3.nx) / 3.0;
        double ny = (t.v1.ny + t.v2.ny + t.v3.ny) / 3.0;
        double nz = (t.v1.nz + t.v2.nz + t.v3.nz) / 3.0;

        // Calculate vector from camera to face center
        
        

        // Dot product determines if face is visible
        double dot = nx * cam.nx + ny * cam.ny + nz * cam.nz;

        // Only render faces pointing toward camera
        if (dot > 0) continue;

        totalTris+=1;
        vec2 v1 = t.v1.project(cam, camYaw, camPitch,getWidth(),getHeight());
        vec2 v2 = t.v2.project(cam, camYaw, camPitch,getWidth(),getHeight());
        vec2 v3 = t.v3.project(cam, camYaw, camPitch,getWidth(),getHeight());

        if (Double.isNaN(v1.x) || Double.isNaN(v2.x) || Double.isNaN(v3.x)) continue;

        int[] xPoints = { (int) v1.x, (int) v2.x, (int) v3.x };
        int[] yPoints = { (int) v1.y, (int) v2.y, (int) v3.y };

        int minX = Math.max(0, Math.min(xPoints[0], Math.min(xPoints[1], xPoints[2])));
        int maxX = Math.min(getWidth() - 1, Math.max(xPoints[0], Math.max(xPoints[1], xPoints[2])));
        int minY = Math.max(0, Math.min(yPoints[0], Math.min(yPoints[1], yPoints[2])));
        int maxY = Math.min(getHeight() - 1, Math.max(yPoints[0], Math.max(yPoints[1], yPoints[2])));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double[] bary = computeBarycentric(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2], x, y);
                double l1 = bary[0], l2 = bary[1], l3 = bary[2];

                if (l1 >= 0 && l2 >= 0 && l3 >= 0) {
                    double u = l1 * t.v1.u + l2 * t.v2.u + l3 * t.v3.u;
                    double v = l1 * t.v1.v + l2 * t.v2.v + l3 * t.v3.v;

                    nx = l1 * t.v1.nx + l2 * t.v2.nx + l3 * t.v3.nx;
                    ny = l1 * t.v1.ny + l2 * t.v2.ny + l3 * t.v3.ny;
                    nz = l1 * t.v1.nz + l2 * t.v2.nz + l3 * t.v3.nz;

                    double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0.0001) {
                        nx /= len;
                        ny /= len;
                        nz /= len;
                    }
                    
                    dot = nx * lightDir.x + ny * lightDir.y + nz * lightDir.z;
                    float intensity;
                    if (dot > 0.95) intensity = 1.0f;
                    else if (dot > 0.5) intensity = 0.6f;
                    else intensity = 0.3f;

                    int texX = (int)(u * texture.getWidth());
                    int texY = (int)(v * texture.getHeight());

                    if (texX >= 0 && texX < texture.getWidth() && texY >= 0 && texY < texture.getHeight()) {
                        int rgb = texture.getRGB(texX, texY);
                        Color texColor = new Color(rgb);
                        int r = (int)(texColor.getRed() * intensity);
                        int g = (int)(texColor.getGreen() * intensity);
                        int b = (int)(texColor.getBlue() * intensity);
                        g2d.setColor(new Color(clamp(r), clamp(g), clamp(b)));
                        g2d.drawLine(x, y, x, y);
                    }
                }
            }
        }
    }
}

private int clamp(int val) {
    return Math.max(0, Math.min(255, val));
}



double[] computeBarycentric(double x1, double y1, double x2, double y2, double x3, double y3, int px, int py) {
    double det = (y2 - y3)*(x1 - x3) + (x3 - x2)*(y1 - y3);
    double l1 = ((y2 - y3)*(px - x3) + (x3 - x2)*(py - y3)) / det;
    double l2 = ((y3 - y1)*(px - x3) + (x1 - x3)*(py - y3)) / det;
    double l3 = 1 - l1 - l2;
    return new double[]{l1, l2, l3};
}
public void update(){
  repaint();
  
    if(debug){
        
        System.out.print("\033[1A");
        System.out.println(totalTris + "\r");
    }
    totalTris = 0;

    animatedCube.playAnimation(0);


}
}
class vec3 {
    double x, y, z;
    double u, v;
    double nx,ny,nz;
    

    public vec3(double x, double y, double z, double u, double v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }
    public vec3 copy() {
    return new vec3(this.x, this.y, this.z, this.u, this.v);
}


    public vec2 project(vec3 cam, double yaw, double pitch, int screenWidth, int screenHeight) {
    double nX = this.x - cam.x;
    double nY = this.y - cam.y;
    double nZ = this.z - cam.z;
    double dot  = (cam.nx * nX) + (cam.ny * nY) + (cam.nz * nZ);

    if (1==1) { // Point is in front of camera
        double rotX = nX * Math.cos(yaw) - nZ * Math.sin(yaw);
        double rotZ = nX * Math.sin(yaw) + nZ * Math.cos(yaw);
        double finalY = nY * Math.cos(pitch) - rotZ * Math.sin(pitch);
        double finalZ = nY * Math.sin(pitch) + rotZ * Math.cos(pitch);

        double scale = 200 / Math.max(finalZ, 0.1);
        double screenCenterX = screenWidth / 2.0;
        double screenCenterY = screenHeight / 2.0;

        return new vec2(rotX * scale + screenCenterX, finalY * scale + screenCenterY);
    } else {
        return new vec2(Double.NaN, Double.NaN);
    }
}
}

class vec2 {
    double x, y;
    public vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

class tri {
    vec3 v1, v2, v3;
    
    public tri(vec3 v1, vec3 v2, vec3 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        
    }
}

class mesh {
    tri[][] tris;
    public mesh(tri[][] tris) {
        this.tris = tris;
    }
}



class spawner {
    Objloader loader = new Objloader();

    
    public mesh LFYS(double x, double y, double z, int aI, double theta, double psi) {
    
    GameObject LFYS = new GameObject(new mesh[]{
        loader.load("Cube.obj",x,y,z,1,1,1)
    }, new AABB(new vec3(0, 0, 0, 0, 0), new vec3(0, 0, 0, 0, 0)), theta, psi, x, y, z);
    return LFYS.getMesh(aI);
}

}


class AABB {
    vec3 min, max;

    public AABB(vec3 min, vec3 max) {
        this.min = min;
        this.max = max;
    }

    public boolean onColide(AABB aabbO, AABB aabbT) {
        return aabbO.min.x > aabbT.max.x && aabbO.min.x < aabbT.max.x &&
               aabbO.min.y > aabbT.max.y && aabbO.min.y < aabbT.max.y &&
               aabbO.min.z > aabbT.max.z && aabbO.min.z < aabbT.max.z;
    }
    public vec3 HMTMAC(AABB aabbO,AABB aabbT){
        if(aabbO.onColide(aabbO,aabbT)){
            return new vec3( aabbT.max.x-aabbO.min.x,aabbT.max.y-aabbO.min.y,aabbT.max.z-aabbO.min.z,0,0);
        }else{
            return new vec3(0,0,0,0,0);
    }
}
}

  class GameObject {
    mesh[] anims;
    AABB hitbox;
    double theta, phi, cx, cy, cz;

    public GameObject(mesh[] anims, AABB hitbox, double theta, double phi, double cx, double cy, double cz) {
        this.anims = anims;
        this.hitbox = hitbox;
        this.theta = theta; // Y-axis rotation
        this.phi = phi;     // Z-axis rotation
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    public mesh getMesh(int AnimIndex) {
        mesh lfys = anims[AnimIndex];

        for (tri[] row : lfys.tris) {
            for (tri t : row) {
                for (vec3 v : new vec3[]{t.v1, t.v2, t.v3}) {
                    
                    double x = v.x - cx;
                    double y = v.y - cy;
                    double z = v.z - cz;
                     double nx = v.nx;
                    double ny = v.ny;
                    double nz = v.nz;
                    // Y-axis rotation (around vertical axis)
                    double x1 = x * Math.cos(theta) - z * Math.sin(theta);
                    double z1 = x * Math.sin(theta) + z * Math.cos(theta);
                    
                    double nx1 = nx * Math.cos(theta) - nz * Math.sin(theta);
                    double nz1 = nx * Math.sin(theta) + nz * Math.cos(theta);
                    
                    // Z-axis rotation (phi)
                    double nx2 = nx1 * Math.cos(phi) - ny * Math.sin(phi);
                    double ny2 = nx1 * Math.sin(phi) + ny * Math.cos(phi);

                    // Z-axis rotation (around forward axis)
                    double x2 = x1 * Math.cos(phi) - y * Math.sin(phi);
                    double y2 = x1 * Math.sin(phi) + y * Math.cos(phi);

                    // Translate back
                    
                    v.x = x2 + cx;
                    v.y = y2 + cy;
                    v.z = z1 + cz;
                    v.nx = nx2;
                    v.ny = ny2;
                    v.nz = nz1;
                    // Translate to origin
                    
                    
                }
            }
        }

        return lfys;
    }
}


class Objloader {
    public mesh load(String path, double offsetX, double offsetY, double offsetZ,double sizex,double sizez,double sizey) {
        java.util.List<vec3> vertices = new java.util.ArrayList<>();
        java.util.List<vec2> uvs = new java.util.ArrayList<>();
        java.util.List<vec3> normals = new java.util.ArrayList<>();
        java.util.List<tri> triangles = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "v": {
                        double x = (Double.parseDouble(parts[1]) + offsetX)*sizex;
                        double y = (Double.parseDouble(parts[2]) + offsetY)*sizey;
                        double z = (Double.parseDouble(parts[3]) + offsetZ)*sizez;
                        vertices.add(new vec3(x, y, z, 0, 0));
                        break;
                    }

                    case "vt": {
                        double u = Double.parseDouble(parts[1]);
                        double v = 1.0 - Double.parseDouble(parts[2]);
                        uvs.add(new vec2(u, v));
                        break;
                    }

                    case "vn": {
                        double nx = Double.parseDouble(parts[1]);
                        double ny = Double.parseDouble(parts[2]);
                        double nz = Double.parseDouble(parts[3]);
                        normals.add(new vec3(nx, ny, nz, 0, 0));
                        break;
                    }

                    case "f": {
                        vec3[] faceVerts = new vec3[3];
                        for (int i = 0; i < 3; i++) {
                            String[] tokens = parts[i + 1].split("/");
                            int vIdx = Integer.parseInt(tokens[0]) - 1;
                            int uvIdx = tokens.length > 1 && !tokens[1].isEmpty() ? Integer.parseInt(tokens[1]) - 1 : -1;
                            int nIdx = tokens.length > 2 && !tokens[2].isEmpty() ? Integer.parseInt(tokens[2]) - 1 : -1;

                            vec3 base = vertices.get(vIdx);
                            vec3 copy = base.copy();

                            if (uvIdx >= 0 && uvIdx < uvs.size()) {
                                vec2 uv = uvs.get(uvIdx);
                                copy.u = uv.x;
                                copy.v = uv.y;
                            }

                            if (nIdx >= 0 && nIdx < normals.size()) {
                                vec3 normal = normals.get(nIdx);
                                copy.nx = normal.x;
                                copy.ny = normal.y;
                                copy.nz = normal.z;
                            }

                            faceVerts[i] = copy;
                        }

                        triangles.add(new tri(faceVerts[0], faceVerts[1], faceVerts[2]));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("OBJ load failed: " + e.getMessage());
        }

        return new mesh(new tri[][] { triangles.toArray(new tri[0]) });
    }
}
class AnimatedObject {
    Animation[] Animations;
    GameObject[] Objects;
    
    public AnimatedObject(Animation[] Animations, GameObject[] Objects) {
        this.Animations = Animations;
        this.Objects = Objects;
    }
    
    public void playAnimation(int Index) {
        if (Index >= 0 && Index < Animations.length) {
            Animations[Index].runAnimation(1);
        }
    }
}
class Animation {
    private final GameObject[] KEY;
    private final vec3[][] AnimPaths;
    private int currentFrame = 0;
    private long lastUpdateTime = 0;
    private boolean looping = false;
    private boolean animationFinished = false;

    private final double[] interpolationProgress;

    public Animation(GameObject[] KEY, vec3[][] AnimPaths) {
        
        this.KEY = KEY;
        this.AnimPaths = AnimPaths;
        this.interpolationProgress = new double[KEY.length];
        for (int i = 0; i < KEY.length; i++) {
            interpolationProgress[i] = 0.0;
        }

       
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    
    public void runAnimation(double speed) {
        if (animationFinished && !looping) {
            return;
        }
        if (speed <= 0.0) {
            throw new IllegalArgumentException("speed must be > 0 (seconds per segment)");
        }

        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0; // seconds
        lastUpdateTime = currentTime;

        // Throttle updates to ~60 FPS (16 ms). If deltaTime is very small, skip this frame.
        if (deltaTime < 0.016) {
            return;
        }

        boolean allReachedTarget = true;

        for (int i = 0; i < KEY.length; i++) {
            // If this key has no path, skip it
            if (AnimPaths[i] == null || AnimPaths[i].length == 0) {
                continue;
            }

            // If currentFrame is beyond this key's path, treat it as reached
            if (currentFrame >= AnimPaths[i].length) {
                continue;
            }

            vec3 target = AnimPaths[i][currentFrame];
            vec3 start = (currentFrame == 0)
                    ? new vec3(KEY[i].cx, KEY[i].cy, KEY[i].cz, KEY[i].theta, KEY[i].phi)
                    : AnimPaths[i][currentFrame - 1];

            // progress increases based on time and the configured speed (seconds per segment)
            interpolationProgress[i] += deltaTime / speed;
            if (interpolationProgress[i] > 1.0) {
                interpolationProgress[i] = 1.0;
            }

            KEY[i].cx = lerp(start.x, target.x, interpolationProgress[i]);
            KEY[i].cy = lerp(start.y, target.y, interpolationProgress[i]);
            KEY[i].cz = lerp(start.z, target.z, interpolationProgress[i]);
            KEY[i].theta = lerp(start.u, target.u, interpolationProgress[i]);
            KEY[i].phi = lerp(start.v, target.v, interpolationProgress[i]);

            if (interpolationProgress[i] < 1.0) {
                allReachedTarget = false;
            }
        }

        if (allReachedTarget) {
            // Advance frame only if there is at least one key with a path length > currentFrame
            boolean anyHasNext = false;
            for (int i = 0; i < AnimPaths.length; i++) {
                if (AnimPaths[i] != null && AnimPaths[i].length > currentFrame + 1) {
                    anyHasNext = true;
                    break;
                }
            }

            // Reset progress for all keys (ready for next segment)
            for (int i = 0; i < interpolationProgress.length; i++) {
                interpolationProgress[i] = 0.0;
            }

            if (looping) {
                // Advance frame and wrap around based on the maximum path length among keys
                int maxLen = 0;
                for (vec3[] path : AnimPaths) {
                    if (path != null && path.length > maxLen) {
                        maxLen = path.length;
                    }
                }
                if (maxLen > 0) {
                    currentFrame = (currentFrame + 1) % maxLen;
                }
            } else {
                // If any key has a next frame, advance; otherwise finish animation
                if (anyHasNext) {
                    currentFrame++;
                } else {
                    animationFinished = true;
                }
            }
        }
    }

    public boolean isFinished() {
        return animationFinished;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }
}
