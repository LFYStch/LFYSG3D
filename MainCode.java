import Main.*;

public class MainCode extends JPanel{
  public void runTheCode(Graphics2D g2d){
    dP d = new dP();
    g2d.setColor(Color.BLUE); 
    g2d.fillRect(0, 0, getWidth(), getHeight()); 
    d.drawMesh(sp.LFYS(0,0,25,0,d.i,d.i),g2d,texture1);
  }
}
