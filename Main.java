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
    
    
    public int deltaTime;
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
    float alpha = 0.5f;
    //dis is 4 da save
    int i = 1;
   
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
    drawMesh(shoulder.getMesh(0),g2d,texture1);
    drawMesh(UPA.getMesh(0),g2d,texture1);
    drawMesh(LRA.getMesh(0),g2d,texture1);
    }
    //No edits past here! >:(
    
   public void drawMesh(mesh ts, Graphics2D g2d, BufferedImage texture) {
    if(ts.type == 1){
        tri t = ts.tris[0][0]; 
        if(t.v1.z<cam.z) return;
        vec2 p1 = t.v1.project(cam, camYaw, camPitch, getWidth(), getHeight());
        vec2 p3 = t.v3.project(cam, camYaw, camPitch, getWidth(), getHeight());

        int sx = (int)p1.x;
        int sy = (int)p1.y;
        int sw = (int)(p3.x - p1.x);
        int sh = (int)(p3.y - p1.y);
        
        g2d.drawImage(texture, sx, sy, sw, sh, null);
        
        return;
    }
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

    vec3 lightDir = new vec3(0,0,1,0, 0);

    for (tri t : sortedTris) {
        double nx = (t.v1.nx + t.v2.nx + t.v3.nx) / 3.0;
        double ny = (t.v1.ny + t.v2.ny + t.v3.ny) / 3.0;
        double nz = (t.v1.nz + t.v2.nz + t.v3.nz) / 3.0;

        // Calculate vector from camera to face center
        
           
            vec3 camDir = new vec3(
                Math.sin(camYaw),
                Math.sin(camPitch),
                Math.cos(camYaw),
                0, 0
            );
            // Dot product determines if face is visible
            if(ts.type!=2){
                double dot = nx * camDir.x  + nz * camDir.z;
                if (dot < 0) continue;
            }

            
        
        totalTris+=1;
        vec2 v1 = t.v1.project(cam, camYaw, camPitch, getWidth(), getHeight());
        vec2 v2 = t.v2.project(cam, camYaw, camPitch, getWidth(), getHeight());
        vec2 v3 = t.v3.project(cam, camYaw, camPitch, getWidth(), getHeight());

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

                    double i = ((nx * lightDir.x) + (ny * lightDir.y) + (nz * lightDir.z));
                    
                    int texX = (int)(Math.max(0, Math.min(1, u)) * texture.getWidth());
                    int texY = (int)(Math.max(0, Math.min(1, v)) * texture.getHeight());


                  
                    if (texX >= 0 && texX < texture.getWidth() && texY >= 0 && texY < texture.getHeight()) {
                        int rgb = texture.getRGB(texX, texY);
                        Color texColor = new Color(rgb,true);
                      
                        
                            int r = (int)(texColor.getRed() * i);
                            int g = (int)(texColor.getGreen() * i);
                            int b = (int)(texColor.getBlue() * i);
                            
                            r = Math.max(0, Math.min(255, r));
                            g = Math.max(0, Math.min(255, g));
                            b = Math.max(0, Math.min(255, b));
                            int a = texColor.getAlpha();
                             g2d.setColor(new Color(r, g, b, a)); 
                             if(debug)  {
                                f((nz+ny+nx)<0){
                                    g2d.setColor(new Color(255,0,0,100));
                             }
                             if((nz+ny+nx)>0){
                                    g2d.setColor(new Color(0,255,0,100));
                             }
                            }
                        
                        g2d.fillRect(x, y, 1, 1);
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
    deltaTime++;
    if(debug){
        
        System.out.print("\033[1A");
        System.out.println(totalTris + " deltaTime: " + deltaTime + "\r");
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

        double scale = 200 / Math.max(finalZ, 1.0);
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
    int type;
    public mesh(tri[][] tris, int type) {
        this.tris = tris;
        this.type = type;
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
    GameObject parent;

    double lx, ly, lz;
    double ry, rz;

    public GameObject(mesh[] anims,
                      double ry, double rz,
                      double lx, double ly, double lz,
                      GameObject parent) {
        this.anims = anims;
        this.ry = ry;
        this.rz = rz;
        this.lx = lx;
        this.ly = ly;
        this.lz = lz;
        this.parent = parent;
    }

    private Transform world() {
    if (parent == null) {
        return new Transform(lx, ly, lz, ry, rz);
    }

    Transform p = parent.world();

    double cosY = Math.cos(p.ry);
    double sinY = Math.sin(p.ry);

    double offX = lx * cosY - lz * sinY;
    double offZ = lx * sinY + lz * cosY;

    return new Transform(
        p.x + offX,
        p.y + ly,
        p.z + offZ,
        p.ry + ry,
        p.rz + this.rz 
    );
}


    public mesh getMesh(int i) {
        mesh src = anims[i];
        Transform t = world();

        tri[][] out = new tri[src.tris.length][];
        for (int r = 0; r < src.tris.length; r++) {
            out[r] = new tri[src.tris[r].length];
            for (int c = 0; c < src.tris[r].length; c++) {
                tri tr = src.tris[r][c];
                out[r][c] = new tri(
                    apply(tr.v1, t),
                    apply(tr.v2, t),
                    apply(tr.v3, t)
                );
            }
        }
        return new mesh(out);
    }

    private vec3 apply(vec3 v, Transform t) {
        double x = v.x;
        double y = v.y;
        double z = v.z;

        double x1 = x * Math.cos(t.ry) - z * Math.sin(t.ry);
        double z1 = x * Math.sin(t.ry) + z * Math.cos(t.ry);

        double x2 = x1 * Math.cos(t.rz) - y * Math.sin(t.rz);
        double y2 = x1 * Math.sin(t.rz) + y * Math.cos(t.rz);

        vec3 o = v.copy();
        o.x = x2 + t.x;
        o.y = y2 + t.y;
        o.z = z1 + t.z;

        // rotate normal
        double nnx = v.nx;
        double nny = v.ny;
        double nnz = v.nz;
        
        // yaw rotation
        double nnx1 = nnx * Math.cos(t.ry) - nnz * Math.sin(t.ry);
        double nnz1 = nnx * Math.sin(t.ry) + nnz * Math.cos(t.ry);
        
        // pitch rotation
        double nnx2 = nnx1 * Math.cos(t.rz) - nny * Math.sin(t.rz);
        double nny2 = nnx1 * Math.sin(t.rz) + nny * Math.cos(t.rz);
        
        o.nx = nnx2;
        o.ny = nny2;
        o.nz = nnz1;

        return o;
    }
}

class Transform {
    double x, y, z;
    double ry, rz;

    Transform(double x, double y, double z, double ry, double rz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ry = ry;
        this.rz = rz;
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

        return new mesh(new tri[][] { triangles.toArray(new tri[0]) }, 0);
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
        obj.lx = newPos.x;
        obj.ly = newPos.y;
        obj.lz = newPos.z;
        obj.ry = newPos.u;
        obj.rz = newPos.v;
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
class Save{
  
    public void makeSave(dP d){
        
        try{
            BufferedWriter hjh = new BufferedWriter(new FileWriter("Save/s.ssm"));
            hjh.write("<Something>\n");
            hjh.write(String.valueOf(d.i) + "\n");
            hjh.write("</Something>\n");
            hjh.close();
        } catch (IOException e) {
            System.err.println("Save failed.");
            e.printStackTrace();
    }
}
    public void loadSave(dP d){
        try{
            BufferedReader br = new BufferedReader(new FileReader("Save/s.ssm"));
            String line;
            while((line = br.readLine()) != null){
                if(line.equals("<PlayerPosition>")){
                    /* 
                    whatever you want to load
                        */
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println("This is not an error. This just signifies that you do NOT have a save file. Do not pannic! \n");
            e.printStackTrace();
    }
}
}
class Billboard {
    double x, y, z;
    double width, height;
    vec3 v1,v2,v3,v4;
    public mesh tg;

    public Billboard(double x, double y, double z, double width, double height) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        v1 = new vec3(0,0,0, 0,0);
v2 = new vec3(0,0,0, 0,1);
v3 = new vec3(0,0,0, 1,1);
v4 = new vec3(0,0,0, 1,0);


       tg = new mesh(new tri[][] {
            {
                new tri(
                   v1, v2, v3
                ),
                new tri(
                   v1,v3,v4
                )
            }
        }, 1);
    }
    public void update(dP d) {

    double dx = d.cam.x - x;
    double dz = d.cam.z - z;
    double theta = -Math.atan2(dx, dz);

    double cos = Math.cos(theta);
    double sin = Math.sin(theta);

    double sWP = width  * 200 / Math.max(z - d.cam.z, 0.1);
    double SHP = height * 200 / Math.max(z - d.cam.z, 0.1);

    double lx = x - sWP * cos;
    double lz = z - sWP * sin;
    double rx = x + sWP * cos;
    double rz = z + sWP * sin;

    // update the quad vertices
    v1.x = lx; v1.y = y - SHP; v1.z = lz;   // top-left
v2.x = lx; v2.y = y + SHP; v2.z = lz;   // bottom-left
v3.x = rx; v3.y = y + SHP; v3.z = rz;   // bottom-right
v4.x = rx; v4.y = y - SHP; v4.z = rz;   // top-right


}
}
