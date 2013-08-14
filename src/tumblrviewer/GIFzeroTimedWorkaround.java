/*
 * Copyright (C) 2013 Jonathan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tumblrviewer;

import com.sun.imageio.plugins.gif.GIFImageMetadata;
import com.sun.imageio.plugins.gif.GIFImageReader;
import java.awt.Image;
import java.awt.image.ImageObserver;
import static java.awt.image.ImageObserver.FRAMEBITS;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JLabel;

/**
 * A couple of classes used to ensure GIF's which have their time delay set to
 * zero can still be displayed and animated correctly without using 100% CPU.
 *
 * @author jonathan
 */
public class GIFzeroTimedWorkaround
{
    private static final long FRAME_DELAY_MS = 100;

    /**
     * Checks if a GIF's delay time between frames is set to zero or not (which 
     * causes problems)
     * @param url the URL of the GIF to check
     * @return true if the GIF is zero timed, false if not a GIF or GIF is okay
     */
    @SuppressWarnings(
    {
        "BroadCatchBlock", "TooBroadCatch"
    })
    public static boolean isZeroTimedGif(URL url)
    {
        if (!url.getFile().toLowerCase().endsWith("gif"))
        {
            //Immediately exclude non-GIFs
            return false;
        }

        try
        {
            // Read the delay time for the first frame in the animated gif
            GIFImageReader reader = (GIFImageReader) ImageIO.getImageReadersByFormatName("GIF").next();
            ImageInputStream iis = ImageIO.createImageInputStream(url.openStream());
            reader.setInput(iis);
            GIFImageMetadata md = (GIFImageMetadata) reader.getImageMetadata(0);

            reader.dispose();
            iis.close();

            if (md.delayTime == 0)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            return true; //Play it safe - something went wrong
        }
        return false;
    }

    /**
     * Set this as the ImageObserver to workaround zero time delayed GIFs.
     * It will make them play back at a sensible speed again by pausing for a 
     * set delay time between updates.
     */
    public static class ImageObserverWorkaround implements ImageObserver
    {
        private final JLabel imageLabel;

        public ImageObserverWorkaround(JLabel imageLabel)
        {
            this.imageLabel = imageLabel;
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
        {
            imageLabel.repaint();
            if ((infoflags & FRAMEBITS) == 0)
            {
                return true;
            }
            else
            {
                try
                {
                    //Make the frame wait 100ms like browsers by do in this situation by sleeping the thread
                    //TODO: Sometimes thread's seem to join when the image is viewed in two different JFrame's simultaneously and then this get's applied twice slowing the image down twice as much - not a major issue but needs looking into why it's happening (inconsistent sometimes it's fine and this doesn't happen at all)
                    Thread.sleep(FRAME_DELAY_MS);
                }
                catch (InterruptedException ex)
                {
                }
            }
            return true;
        }
    }
}
