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
    
    
   
    public Graphics2D g2d;
    vec3 cam;
    double camYaw, camPitch;
    vec3 light_source1;
    int totalTris;
    boolean debug = false;
    BufferedImage texture1;
    spawner sp;
    KeyFrame ArmAnim;
    GameObject shoulder,UPA,LRA;
    
   
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
        shoulder = new GameObject(new mesh[]{loader.load("Cube.obj",0,0,20,0.25,0.5,0.5)},0,0,0,0,0,null);
        UPA = new GameObject(new mesh[]{loader.load("Cube.obj",0,10,20,0.25,0.5,0.5)},0,0,0,0,0,shoulder);
        LRA = new GameObject(new mesh[]{loader.load("Cube.obj",0,20,20,0.25,0.5,0.5)},0,0,0,0,0,UPA);
        ArmAnim = this.makeArm();
    }
    public KeyFrame makeArm() {  
        vec3[][] animPaths = new vec3[3][];
        animPaths[0] = new vec3[]{
           
            new vec3(0, 0, 20, 0, 0),                     
            new vec3(0, 0, 20, 0, Math.toRadians(10)),     
            new vec3(0, 0, 20, 0, Math.toRadians(-10))                    
                            
    };
    
    animPaths[1] = new vec3[]{
       
        new vec3(0, 10, 20, 0, 0),             
        new vec3(0, -3, 20, 0, Math.toRadians(45)),     
        new vec3(0, -3, 20, 0, Math.toRadians(-45))                  
    };
    
    animPaths[2] = new vec3[]{
        
        new vec3(0, 10, 20, 0, 0),               
        new vec3(0, -7, 20, 0, Math.toRadians(45)),     
        new vec3(0, -7, 20, 0, Math.toRadians(-45))                    
    };
    
        KeyFrame kf = new KeyFrame(new GameObject[]{shoulder, UPA, LRA}, animPaths);
        kf.setFrameDuration(0.4);
        return kf;
    }
    public void loadTextures() {
        try {
            texture1 = ImageIO.read(new File("dir.png"));
            
        } catch (IOException e) {
            System.err.println("Texture load failed.");
            e.printStackTrace();
        }
    }
    
    
    //Main drawloop starts here! :)
    @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g2d = (Graphics2D) g;
    ArmAnim.runAnimation(0.1);
    drawMesh(shoulder.getMesh(0),g2d,texture1,0.5f,257,0,0);
    drawMesh(UPA.getMesh(0),g2d,texture1,0.5f,0,257,0);
    drawMesh(LRA.getMesh(0),g2d,texture1,0.5f,0,0,257);
    }
    //No edits past here! >:(
   public void drawMesh(mesh ts, Graphics2D g2d, BufferedImage texture, float alpha,int colorOffsetR,int colorOffsetG,int colorOffsetB) {
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
                      
                        if (colorOffsetR < -254 || colorOffsetG < -254 || colorOffsetB < -254) {
                          
                            g2d.setColor(Color.BLACK);
                        } else {
                            
                            int r = (int)(texColor.getRed() * intensity) + colorOffsetR;
                            int g = (int)(texColor.getGreen() * intensity) + colorOffsetG;
                            int b = (int)(texColor.getBlue() * intensity) + colorOffsetB;
                            
                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            b = Math.max(0, Math.min(255, b));
                            
                            g2d.setColor(new Color(r, g, b));
                        }
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

    if (dot <= 0.9) { // Point is in front of camera
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
    GameObject parent;
    AABB hitbox;
    double theta, phi, cx, cy, cz,lt,lp,lx,ly,lz;

    public GameObject(mesh[] anims, double theta, double phi, double cx, double cy, double cz,GameObject parent) {
        this.anims = anims;
        
        this.theta = theta; // Y-axis rotation
        this.phi = phi;     // Z-axis rotation
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }
    public mesh getMesh(int AnimIndex) {
        mesh lfys = anims[AnimIndex];
        if(parent != null){
            cx=parent.cx+cx;
            cy=parent.cy+cy;
            cz=parent.cz+cz;
            theta=parent.theta+theta;
            phi=parent.phi+phi;
        }
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
class KeyFrame {
    public double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    public vec3 lerpVec3(vec3 a, vec3 b, double t) {
        return new vec3(
            lerp(a.x, b.x, t),
            lerp(a.y, b.y, t),
            lerp(a.z, b.z, t),
            lerp(a.u, b.u, t),
            lerp(a.v, b.v, t)
        );
    }
    
    GameObject[] KEY; 
    vec3[][] AnimIndexforAnim; 
    private double animationTime = 0;
    private int currentFrame = 0;
    private boolean looping = true;
    private double frameDuration = 0.5; 
    public KeyFrame(GameObject[] KEY, vec3[][] AnimIndexforAnim) {
        this.KEY = KEY;
        this.AnimIndexforAnim = AnimIndexforAnim;
    }
    
    public void runAnimation(double deltaTime) {
        animationTime += deltaTime;
        
    
        int nextFrame = (currentFrame + 1) % AnimIndexforAnim[0].length;
      
        if (animationTime >= frameDuration) {
            currentFrame = nextFrame;
            nextFrame = (currentFrame + 1) % AnimIndexforAnim[0].length;
            animationTime = 0;
            
           
            if (!looping && currentFrame == AnimIndexforAnim[0].length - 1) {
                return;
            }
        }
        
       
        double t = animationTime / frameDuration;
        t = Math.max(0, Math.min(1, t));
        
      
        for (int objectIndex = 0; objectIndex < KEY.length; objectIndex++) {
            if (KEY[objectIndex] != null && objectIndex < AnimIndexforAnim.length) {
                animateObject(objectIndex, currentFrame, nextFrame, t);
            }
        }
        }
        
    private void animateObject(int objectIndex, int currentFrame, int nextFrame, double t) {
        
        vec3 currentPos = AnimIndexforAnim[objectIndex][currentFrame];
        vec3 nextPos = AnimIndexforAnim[objectIndex][nextFrame];
        
        
        vec3 newPos = lerpVec3(currentPos, nextPos, t);
        
       
        GameObject obj = KEY[objectIndex];
        obj.cx = newPos.x; 
        obj.cy = newPos.y;   
        obj.cz = newPos.z;  
        obj.theta = newPos.u;
        obj.phi = newPos.v;   
    }
    
    public void reset() {
        animationTime = 0;
        currentFrame = 0;
    }
    
    public void setLooping(boolean loop) {
        this.looping = loop;
    }
    
    public void setFrameDuration(double duration) {
        this.frameDuration = duration;
    }
    
    public boolean isFinished() {
        return !looping && currentFrame == AnimIndexforAnim[0].length - 1;
    }
}
