import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** The Vodafone and JBlend 3D packages drawing into an image, step by step. */
public class J3dTest {
    public static void main(String[] args) throws Exception {
        System.out.println("1 image");
        Image vi = Image.createImage(64, 64);
        System.out.println("2 figure");
        com.vodafone.v10.graphics.j3d.Figure vf = new com.vodafone.v10.graphics.j3d.Figure("/mc_old.mbac");
        System.out.println("3 texture");
        vf.setTexture(new com.vodafone.v10.graphics.j3d.Texture("/mc.bmp", true));
        com.vodafone.v10.graphics.j3d.AffineTrans va = new com.vodafone.v10.graphics.j3d.AffineTrans();
        va.setIdentity();
        System.out.println("4 graphics");
        Graphics g = vi.getGraphics();
        System.out.println("5 draw");
        ((com.vodafone.v10.graphics.j3d.Graphics3D) g).drawFigure(vf, 0, 0,
            new com.vodafone.v10.graphics.j3d.FigureLayout(va, 2048, 2048, 32, 32), new com.vodafone.v10.graphics.j3d.Effect3D());
        System.out.println("6 count");
        int[] px = new int[64 * 64];
        int n = 0;
        vi.getRGB(px, 0, 64, 0, 0, 64, 64);
        for (int i = 0; i < px.length; i++) if ((px[i] & 0xFFFFFF) != 0xFFFFFF) n++;
        System.out.println("vodafone drew " + n + " pixels");
        System.out.println("7 jblend figure");
        Image ji = Image.createImage(64, 64);
        com.jblend.graphics.j3d.Figure jf = new com.jblend.graphics.j3d.Figure("/mc_old.mbac");
        jf.setTexture(new com.jblend.graphics.j3d.Texture("/mc.bmp", true));
        com.jblend.graphics.j3d.AffineTrans ja = new com.jblend.graphics.j3d.AffineTrans();
        ja.setIdentity();
        System.out.println("8 jblend draw");
        ((com.jblend.graphics.j3d.Graphics3D) ji.getGraphics()).drawFigure(jf, 0, 0,
            new com.jblend.graphics.j3d.FigureLayout(ja, 2048, 2048, 32, 32), new com.jblend.graphics.j3d.Effect3D());
        n = 0;
        ji.getRGB(px, 0, 64, 0, 0, 64, 64);
        for (int i = 0; i < px.length; i++) if ((px[i] & 0xFFFFFF) != 0xFFFFFF) n++;
        System.out.println("jblend drew " + n + " pixels");
        System.out.println("9 v3 figures first, as McTest does");
        com.mascotcapsule.micro3d.v3.Figure f5 = new com.mascotcapsule.micro3d.v3.Figure("/mc.mbac");
        com.motorola.graphics.j3d.Figure mf = new com.motorola.graphics.j3d.Figure("/mc_old.mbac");
        System.out.println("10 done " + f5.getNumPattern() + " " + (mf != null));
    }
}
