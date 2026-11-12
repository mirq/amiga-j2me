/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class ImageItem extends Item {
    public static final int LAYOUT_DEFAULT = 0, LAYOUT_LEFT = 1, LAYOUT_RIGHT = 2, LAYOUT_CENTER = 3,
                            LAYOUT_NEWLINE_BEFORE = 0x100, LAYOUT_NEWLINE_AFTER = 0x200;
    private Image img;
    private String altText;
    private final int mode;

    public ImageItem(String label, Image img, int layout, String altText) {
        this(label, img, layout, altText, PLAIN);
    }

    public ImageItem(String label, Image image, int layout, String altText, int appearanceMode) {
        this.label = label; this.img = image; this.layout = layout; this.altText = altText; this.mode = appearanceMode;
    }

    public Image getImage() { return img; }
    public void setImage(Image i) { img = i; changed(); }
    public String getAltText() { return altText; }
    public void setAltText(String t) { altText = t; }
    public int getAppearanceMode() { return mode; }

    int contentHeight(int w) { return img == null ? 0 : img.getHeight() + 2; }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        if (img == null) return;
        int ix = x;
        if ((layout & 3) == LAYOUT_CENTER) ix = x + (w - img.getWidth()) / 2;
        else if ((layout & 3) == LAYOUT_RIGHT) ix = x + w - img.getWidth();
        g.drawImage(img, ix, y, Graphics.TOP | Graphics.LEFT);
        if (focused) { g.setColor(Screen.HI_BG); g.drawRect(ix - 1, y - 1, img.getWidth() + 1, img.getHeight() + 1); }
    }
}
