import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;




public class Main implements KeyListener {
    private dP d; 
    public static Object gameLock = new Object();
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
        
        Thread update = new Thread(() -> {
            final long frameTime = 16_666_666;

            while (true) {
                long start = System.nanoTime();

                synchronized (gameLock) {
                    d.update();
                }

                while (System.nanoTime() - start < frameTime) {
                    Thread.yield();
                }
            }
        });

        update.setDaemon(true);
        update.start();
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
     double[] zbuf = new double[320*240];
    BufferedImage buffer = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
    int[] fb = ((DataBufferInt)buffer.getRaster().getDataBuffer()).getData();
    public int deltaTime;
    public Graphics2D g2d;
    vec3 cam;
    double camYaw, camPitch;
    vec3 light_source1;
    int totalTris;
    boolean debug = false;
    BufferedImage texture1;

    KeyFrame ArmAnim;
    GameObject shoulder,UPA,LRA;
    float alpha = 0.5f;
    //dis is 4 da save
    int i = 1;
   
    Objloader loader = new Objloader();

    public dP() {
        setDoubleBuffered(true);
        cam = new vec3(0, 0, 15, 0, 0);
        cam.nx = Math.cos(camYaw);cam.ny = Math.cos(camPitch);cam.nz = Math.sin(camYaw);
        light_source1 = new vec3(cam.x, cam.y, cam.z - 2, 0, 0);
        camYaw = 0;
        camPitch = 0;
        loadTextures();
      
        shoulder = new GameObject(
    new mesh[]{loader.load("cube.obj",0,0,0,1,1,1,0)},
    0,0,0,   // rotation
    0,0,20,  // local position
    null
);

UPA = new GameObject(
    new mesh[]{loader.load("cube.obj",0,0,0,1,1,1,0)},
    0,0,0,
    10,0,0,  // offset from shoulder
    shoulder
);

LRA = new GameObject(
    new mesh[]{loader.load("cube.obj",0,0,0,1,1,1,0)},
    0,0,0,
    10,0,0,  // offset from UPA
    UPA
);

        shoulder.px = 0; shoulder.py = 0; shoulder.pz = 0;

    UPA.px = 0; UPA.py = -5; UPA.pz = 0;   // pivot at top of upper arm
    LRA.px = 0; LRA.py = -5; LRA.pz = 0;   // pivot at top of lower arm

        ArmAnim = this.makeArm();
    }
  public KeyFrame makeArm() {
    vec6[][] animPaths = new vec6[3][];

    // SHOULDER — rotate 90° around Z
    animPaths[0] = new vec6[]{
        new vec6(0, 0, 20, 0, 0, 0),
        new vec6(0, 0, 20, 0, 0, 0),
        new vec6(0, 0, 20, 0, 0, 0)
    };
    // UPPER ARM — rotate 90° around Z
    animPaths[1] = new vec6[]{
        new vec6(0, 0, 0, 0, 0, 0),
        new vec6(0, -0.5, 0, 0, 0,Math.toRadians(30)),
        new vec6(0, 0, 0, 0,0, 0)
    };
    
    // LOWER ARM — rotate 90° around Z
    animPaths[2] = new vec6[]{
        new vec6(0, 0, 0, 0,0, 0),
        new vec6(0, -0.5, 0, 0,0, Math.toRadians(30)),
        new vec6(0, 0, 0, 0, 0,0)
    };

    KeyFrame kf = new KeyFrame(new GameObject[]{shoulder, UPA, LRA}, animPaths);
    kf.setFrameDuration(1.0);
    return kf;
}

 public static BufferedImage loadImage(String path) {
    try (InputStream in = Main.class.getResourceAsStream("/" + path)) {
        if (in == null) return null;

        BufferedImage img = ImageIO.read(in);

        if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
            BufferedImage converted = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = converted.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            return converted;
        }

        return img;

    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}
    public void loadTextures() {
        
            texture1 = loadImage("dir.png");
       
    }
    vec3 sub(vec3 a, vec3 b) {
    return new vec3(a.x - b.x, a.y - b.y, a.z - b.z, 0, 0);
}

vec3 cross(vec3 a, vec3 b) {
    return new vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x,
        0, 0
    );
}
public static void applyDither(int[] fb, int w, int h) {

    // 4×4 Bayer matrix scaled to 0–7 (PS1 used 3-bit dither)
    final int[] BAYER = {
         0, 4, 1, 5,
         6, 2, 7, 3,
         1, 5, 0, 4,
         7, 3, 6, 2
    };

    int idx = 0;

    for (int y = 0; y < h; y++) {
        int by = (y & 3) << 2; // (y % 4) * 4

        for (int x = 0; x < w; x++, idx++) {
            if ((fb[idx] >>> 24) != 0xFF) continue; // skip UI

         int c = fb[idx];

            int r = (c >> 16) & 0xFF;
            int g = (c >>  8) & 0xFF;
            int b =  c        & 0xFF;

            int t = BAYER[by | (x & 3)]; // threshold 0–7

            // Add dither BEFORE truncation (PS1 behavior)
            r = (r + t) >> 3;  // convert to 5-bit
            g = (g + t) >> 3;
            b = (b + t) >> 3;

            // Expand back to 8-bit
            fb[idx] = 0xFF000000 |
                     ((r << 3) << 16) |
                     ((g << 3) << 8)  |
                     (b << 3);
        }
    }
}


    //Main drawloop starts here! :)
    @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g2d = (Graphics2D) g;  
    
        synchronized(Main.gameLock){
          if (zbuf == null || zbuf.length != 320 * 240)
    zbuf = new double[320 * 240];

Arrays.fill(zbuf, Double.POSITIVE_INFINITY);
Arrays.fill(fb, 0); // clear framebuffer to black



        ArmAnim.runAnimation(0.1);
        drawMesh(shoulder.getMesh(0),texture1);
        drawMesh(UPA.getMesh(0),texture1);
        drawMesh(LRA.getMesh(0),texture1);
        //drawMesh(loader.load("Cube.obj",0,0,16,1,1,1,0),g2d,texture1);
        //drawMesh(loader.load("Cube.obj",10,0,16,1,1,1,0),g2d,texture1);
    applyDither(fb, 320,240);
    g2d.drawImage(buffer, 0, 0, getWidth(), getHeight(), null);
        }
    }
    //No edits past here! >:(
    
public void drawMesh(mesh ts, BufferedImage texture) {
       
        int[] tex = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
       
       
    java.util.List<tri> sortedTris = new java.util.ArrayList<>();
    for (tri[] strip : ts.tris) {
        java.util.Collections.addAll(sortedTris, strip);
    }

     // In drawMesh(), before sorting:

    
   vec3 lightDir = new vec3(Math.sin(camYaw), 0.5, Math.cos(camYaw),0,0);

        double len = Math.sqrt(lightDir.x*lightDir.x + lightDir.y*lightDir.y + lightDir.z*lightDir.z);
        lightDir.x /= len;
        lightDir.y /= len;
        lightDir.z /= len;

    int screenW = 320;
    int screenH = 240;
    int texW = texture.getWidth();
    int texH = texture.getHeight();
    
    for (tri t : sortedTris) {
        /* 
        t.v1.u = clamp(t.v1.u,0,1);
        t.v1.v = clamp(t.v1.v,0,1);
        t.v2.u = clamp(t.v2.u,0,1);
        t.v2.v = clamp(t.v2.v,0,1);
        t.v3.u = clamp(t.v3.u,0,1);
        t.v3.v = clamp(t.v3.v,0,1);                
        */
       
        double nx = (t.v1.nx + t.v2.nx + t.v3.nx) / 3.0;
        double ny = (t.v1.ny + t.v2.ny + t.v3.ny) / 3.0;
        double nz = (t.v1.nz + t.v2.nz + t.v3.nz) / 3.0;
        double li = -(nx*lightDir.x + ny*lightDir.y + nz*lightDir.z);
    li = Math.clamp(li, 0.3, 1.0);

        len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
       
        
        

        double view = Math.sin(camYaw) * nx + Math.sin(camYaw) * ny + Math.sin(camYaw) * nz;
        if (view < 0) continue;
        
        if(ts.type==1 || ts.type == 4){
            li=1;
        } 
        totalTris++;

        vec2 v1 = t.v1.project(cam, camYaw, camPitch, screenW, screenH);
        vec2 v2 = t.v2.project(cam, camYaw, camPitch, screenW, screenH);
        vec2 v3 = t.v3.project(cam, camYaw, camPitch, screenW, screenH);
        //if (!t.frustumNearCull(0.1)) continue;
        if (Double.isNaN(v1.x) || Double.isNaN(v2.x) || Double.isNaN(v3.x)) continue;

        int x1 = (int) v1.x;
        int y1 = (int) v1.y;
        int x2 = (int) v2.x;
        int y2 = (int) v2.y;
        int x3 = (int) v3.x;
        int y3 = (int) v3.y;

        int minX = Math.max(0, Math.min(x1, Math.min(x2, x3)));
        int maxX = Math.min(screenW - 1, Math.max(x1, Math.max(x2, x3)));
        int minY = Math.max(0, Math.min(y1, Math.min(y2, y3)));
        int maxY = Math.min(screenH - 1, Math.max(y1, Math.max(y2, y3)));

        if (minX >= maxX || minY >= maxY) continue;

        float X1 = x1, Y1 = y1;
        float X2 = x2, Y2 = y2;
        float X3 = x3, Y3 = y3;

        float A01 = Y1 - Y2, B01 = X2 - X1;
        float A12 = Y2 - Y3, B12 = X3 - X2;
        float A20 = Y3 - Y1, B20 = X1 - X3;

        float area = A01 * (X3 - X1) + B01 * (Y3 - Y1);
        if (area == 0) continue;
        float invArea = 1.0f / area;

        double z1v = t.v1.depth, z2v = t.v2.depth, z3v = t.v3.depth;

        double u1v = t.v1.u, u2v = t.v2.u, u3v = t.v3.u;
        double v1v = t.v1.v, v2v = t.v2.v, v3v = t.v3.v;
        
        for (int y = minY; y <= maxY; y++) {
            int rowIndex = y * screenW;
            for (int x = minX; x <= maxX; x++) {
                float fx = x;
                float fy = y;

                float w0 = (A12 * (fx - X2) + B12 * (fy - Y2)) * invArea;
                float w1 = (A20 * (fx - X3) + B20 * (fy - Y3)) * invArea;
                float w2 = 1.0f - w0 - w1;

                if (w0 < 0 || w1 < 0 || w2 < 0) continue;

                int idx = rowIndex + x;

                double z = w0 * z1v + w1 * z2v + w2 * z3v;

if (ts.type != 3 && z > zbuf[idx]) continue;

                double u = w0 * u1v + w1 * u2v + w2 * u3v;
                double v = w0 * v1v + w1 * v2v + w2 * v3v;

                u = Math.clamp(u,0,1);
                v = Math.clamp(v,0,1);


                int texX = (int)(u * (texW - 1));
                int texY = (int)(v * (texH - 1));
                int rgb = tex[texY * texW + texX];

                int a = (rgb >>> 24) & 0xFF;
                if (a == 0) continue;

                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = (int)(r * li);
                g = (int)(g * li);
                b = (int)(b * li);

                if (r < 0) r = 0; else if (r > 255) r = 255;
                if (g < 0) g = 0; else if (g > 255) g = 255;
                if (b < 0) b = 0; else if (b > 255) b = 255;
                
   if(ts.type!=4)   {         

   }    
   
        if (z < zbuf[idx] && ts.type != 4) {
    if (!t.transparent) {
        zbuf[idx] = z;
        fb[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
            
        
        }
    }
    }
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
    repaint();
   


}
}
class vec3 {

    double x, y, z;
    double u, v;
    double nx, ny, nz;
    double depth;
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

        double rotX = nX * Math.cos(yaw) - nZ * Math.sin(yaw);
        double rotZ = nX * Math.sin(yaw) + nZ * Math.cos(yaw);

        double finalY = nY * Math.cos(pitch) - rotZ * Math.sin(pitch);
        double finalZ = nY * Math.sin(pitch) + rotZ * Math.cos(pitch);
        this.depth = finalZ;
        double scale = 200 / Math.max(finalZ, 1.0);
        double screenCenterX = screenWidth / 2.0;
        double screenCenterY = screenHeight / 2.0;
       
        return new vec2(rotX * scale + screenCenterX, finalY * scale + screenCenterY);
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
    boolean transparent = false;
    public tri(vec3 v1, vec3 v2, vec3 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
    public boolean frustumNearCull(double nearZ) {
    return v1.depth >= nearZ || v2.depth >= nearZ || v3.depth >= nearZ;
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


  class Transform {
    double x, y, z;
    double rx, ry, rz;

    Transform(double x, double y, double z, double rx, double ry, double rz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }
}

class GameObject {

    mesh[] anims;
    GameObject parent;

    // local position
    double lx, ly, lz;

    // local rotation
    double rx, ry, rz;

    // pivot
    double px = 0, py = 0, pz = 0;

    public GameObject(mesh[] anims, double ry, double rz, double rx,
                      double lx, double ly, double lz, GameObject parent) {

        this.anims = anims;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.lx = lx;
        this.ly = ly;
        this.lz = lz;
        this.parent = parent;
    }

    // ---------------------------
    // CORRECT WORLD TRANSFORM
    // ---------------------------
    public Transform world() {
        if (parent == null) {
            return new Transform(lx, ly, lz, rx, ry, rz);
        }

        Transform p = parent.world();

        // rotate local offset by parent rotation
        double[] off = rotateXYZ(lx, ly, lz, p.rx, p.ry, p.rz);

        return new Transform(
            p.x + off[0],
            p.y + off[1],
            p.z + off[2],
            p.rx + rx,
            p.ry + ry,
            p.rz + rz
        );
    }

    // ---------------------------
    // APPLY TRANSFORM TO VERTEX
    // ---------------------------
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

        return new mesh(out, 0);
    }

    private vec3 apply(vec3 v, Transform t) {

        // move to pivot
        double x = v.x - px;
        double y = v.y - py;
        double z = v.z - pz;

        // rotate around local axes
        double[] r1 = rotateXYZ(x, y, z, t.rx, t.ry, t.rz);

        // move back from pivot
        double fx = r1[0] + px;
        double fy = r1[1] + py;
        double fz = r1[2] + pz;

        // apply world translation
        vec3 o = v.copy();
        o.x = fx + t.x;
        o.y = fy + t.y;
        o.z = fz + t.z;

        // rotate normals too
        double[] n = rotateXYZ(v.nx, v.ny, v.nz, t.rx, t.ry, t.rz);
        o.nx = n[0];
        o.ny = n[1];
        o.nz = n[2];

        return o;
    }

    // ---------------------------
    // CORRECT ROTATION ORDER
    // X → Y → Z
    // ---------------------------
    private double[] rotateXYZ(double x, double y, double z, double rx, double ry, double rz) {

        // rotate X
        double cx = Math.cos(rx), sx = Math.sin(rx);
        double y1 = y * cx - z * sx;
        double z1 = y * sx + z * cx;

        // rotate Y
        double cy = Math.cos(ry), sy = Math.sin(ry);
        double x2 = x * cy + z1 * sy;
        double z2 = -x * sy + z1 * cy;

        // rotate Z
        double cz = Math.cos(rz), sz = Math.sin(rz);
        double x3 = x2 * cz - y1 * sz;
        double y3 = x2 * sz + y1 * cz;

        return new double[]{x3, y3, z2};
    }
}


class Objloader {
    public static InputStream loadResource(String path) {
    InputStream in = Main.class.getResourceAsStream("/" + path);
    if (in == null) {
        System.out.println("OBJ not found: " + path);
    }
    return in;
}

    public mesh load(String path, double offsetX, double offsetY, double offsetZ,double sizex,double sizez,double sizey, int t) {
        java.util.List<vec3> vertices = new java.util.ArrayList<>();
        java.util.List<vec2> uvs = new java.util.ArrayList<>();
        java.util.List<vec3> normals = new java.util.ArrayList<>();
        java.util.List<tri> triangles = new java.util.ArrayList<>();
        InputStream in = loadResource(path);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in));
) {
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

        return new mesh(new tri[][] { triangles.toArray(new tri[0]) }, t);
    }
}
class vec6{
    double x,y,z,t,h,p;
    public vec6(double x,double y,double z,double px, double py,double pz){
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = px;
        this.h = py;
        this.p = pz;
    }
}
class KeyFrame {
    public double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    public vec6 lerpVec6(vec6 a, vec6 b, double t) {
        return new vec6(
            lerp(a.x, b.x, t),
            lerp(a.y, b.y, t),
            lerp(a.z, b.z, t),
            lerp(a.t, b.t, t),
            lerp(a.h, b.h, t),
            lerp(a.p, b.p, t)
        );
    }
    
    GameObject[] KEY; 
    vec6[][] AnimIndexforAnim; 
    private double animationTime = 0;
    private int currentFrame = 0;
    private boolean looping = true;
    private double frameDuration = 0.5; 
    public KeyFrame(GameObject[] KEY, vec6[][] AnimIndexforAnim) {
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
        
        vec6 currentPos = AnimIndexforAnim[objectIndex][currentFrame];
        vec6 nextPos = AnimIndexforAnim[objectIndex][nextFrame];
        
        
        vec6 newPos = lerpVec6(currentPos, nextPos, t);
        
       
        GameObject obj = KEY[objectIndex];
        obj.lx = newPos.x;
        obj.ly = newPos.y;
        obj.lz = newPos.z;
        obj.rx = newPos.t;
        obj.ry = newPos.h;
        obj.rz = newPos.p;
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
