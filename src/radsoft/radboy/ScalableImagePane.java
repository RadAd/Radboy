package radsoft.radboy;

import java.awt.Dimension;
import java.awt.Image;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;

public class ScalableImagePane extends JPanel
{
    private static final long serialVersionUID = 1;
    
    private Image master;
    private boolean toFit;
    private Image scaled;

    public ScalableImagePane(Image master)
    {
        this(master, true);
    }

    public ScalableImagePane(Image master, boolean toFit)
    {
        this.master = master;
        setToFit(toFit);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return master == null ? super.getPreferredSize() : getMasterSize();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g)
    {
        super.paintComponent(g);
        Image toDraw = null;
        if (scaled != null)
            toDraw = scaled;
        else if (master != null)
            toDraw = master;

        if (toDraw != null)
        {
            int x = (getWidth() - toDraw.getWidth(this)) / 2;
            int y = (getHeight() - toDraw.getHeight(this)) / 2;
            g.drawImage(toDraw, x, y, this);
        }
    }

    @Override
    public void invalidate()
    {
        generateScaledInstance();
        super.invalidate();
    }

    public boolean isToFit()
    {
        return toFit;
    }

    public void setToFit(boolean value)
    {
        if (value != toFit)
        {
            toFit = value;
            invalidate();
        }
    }
    
    private Dimension getMasterSize()
    {
        return new Dimension(master.getWidth(this), master.getHeight(this));
    }

    protected void generateScaledInstance()
    {
        Dimension masterSize = getMasterSize();
        Dimension size = getSize();
        BufferedImage image = (BufferedImage) master;
        if (image == null)
        {
            image = createCompatibleImage(masterSize, java.awt.Transparency.OPAQUE);
            java.awt.Graphics2D g2d = image.createGraphics();
            g2d.drawImage(master, 0, 0, this);
            g2d.dispose();
        }
        scaled = getScaledInstance(
                        image,
                        isToFit() ? getScaleFactorToFit(masterSize, size) : getScaleFactorToFill(masterSize, size),
                        //java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                        true);
    }

    static private double getScaleFactor(int iMasterSize, int iTargetSize)
    {
        return (double) iTargetSize / (double) iMasterSize;
    }

    static private double getScaleFactorToFit(Dimension original, Dimension toFit)
    {
        double dScale = 1d;
        if (original != null && toFit != null)
        {
            double dScaleWidth = getScaleFactor(original.width, toFit.width);
            double dScaleHeight = getScaleFactor(original.height, toFit.height);
            dScale = Math.min(dScaleHeight, dScaleWidth);
        }
        return dScale;
    }

    static private double getScaleFactorToFill(Dimension masterSize, Dimension targetSize)
    {
        double dScaleWidth = getScaleFactor(masterSize.width, targetSize.width);
        double dScaleHeight = getScaleFactor(masterSize.height, targetSize.height);

        return Math.max(dScaleHeight, dScaleWidth);
    }

    private BufferedImage createCompatibleImage(Dimension size, int transparency)
    {
        return createCompatibleImage(size.width, size.height, transparency);
    }

    private BufferedImage createCompatibleImage(int width, int height, int transparency)
    {
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null)
            gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        BufferedImage image = gc.createCompatibleImage(width, height, transparency);
        image.coerceData(true);
        return image;
    }

    private Image getScaledInstance(BufferedImage img, double dScaleFactor, Object hint, boolean bHighQuality)
    {
        Image imgScale = img;
        int iImageWidth = (int) Math.round(img.getWidth() * dScaleFactor);
        int iImageHeight = (int) Math.round(img.getHeight() * dScaleFactor);
        
        if (!bHighQuality)
        {
            imgScale = getResizedInstance(img, iImageWidth, iImageHeight, BufferedImage.TYPE_INT_RGB, hint);
        }
        else if (dScaleFactor <= 1.0d)
        {
            imgScale = getScaledDownInstance(img, iImageWidth, iImageHeight, hint);
        }
        else
        {
            imgScale = getScaledUpInstance(img, iImageWidth, iImageHeight, hint);
        }

        return imgScale;
    }

    private Image getScaledDownInstance(BufferedImage img,
                    int targetWidth,
                    int targetHeight,
                    Object hint)
    {
        int type = (img.getTransparency() == java.awt.Transparency.OPAQUE)
                        ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

        Image ret = img;

        if (targetHeight > 0 || targetWidth > 0)
        {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            int w = img.getWidth();
            int h = img.getHeight();

            while (w != targetWidth || h != targetHeight)
            {
                if (w > targetWidth)
                {
                    w /= 2;
                    if (w < targetWidth)
                        w = targetWidth;
                }
                
                if (h > targetHeight)
                {
                    h /= 2;
                    if (h < targetHeight)
                        h = targetHeight;
                }

                ret = getResizedInstance(ret, w, h, type, hint);
            }
        }
        else
        {
            //ret = new BufferedImage(1, 1, type);
            ret = createCompatibleImage(1, 1, type);
        }

        return ret;
    }

    private Image getScaledUpInstance(BufferedImage img,
                    int targetWidth,
                    int targetHeight,
                    Object hint)
    {
        int type = (img.getTransparency() == java.awt.Transparency.OPAQUE)
                        ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

        Image ret = img;

        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        int w = img.getWidth();
        int h = img.getHeight();

        while (w != targetWidth || h != targetHeight)
        {
            if (w < targetWidth)
            {
                w *= 2;
                if (w > targetWidth)
                    w = targetWidth;
            }

            if (h < targetHeight)
            {
                h *= 2;
                if (h > targetHeight)
                    h = targetHeight;
            }

            ret = getResizedInstance(ret, w, h, type, hint);
        }
        
        return ret;
    }

    private BufferedImage getResizedInstance(Image img,
                        int width,
                        int height,
                        int type,
                        Object hint)
    {
        //BufferedImage tmp = new BufferedImage(width, height, type);
        BufferedImage tmp = createCompatibleImage(width, height, type);
        java.awt.Graphics2D g2 = tmp.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(img, 0, 0, width, height, null);
        g2.dispose();
        return tmp;
    }
}
