package tumblrviewer;

import com.tumblr.jumblr.types.PhotoPost;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;

/**
 *
 * @author jonathan
 */
public class PhotoPostViewer extends PostViewer
{
    private final MainViewGUI mainViewGUI;
    private final TumblrBackend tumblrBackend;
    private final PhotoPost photoPost;
    private final int photoNumberInPost;

    public PhotoPostViewer(MainViewGUI mainViewGUI, TumblrBackend tumblrBackend, PhotoPost photoPost, int photoNumberInPost)
    {
        super(mainViewGUI, tumblrBackend, photoPost);
        this.mainViewGUI = mainViewGUI;
        this.tumblrBackend = tumblrBackend;
        this.photoPost = photoPost;
        this.photoNumberInPost = photoNumberInPost;

        super.loadFromTumblr();
    }

    @Override
    void loadMainContent(Thread refreshControlsThread)
    {
        Thread loadMainContentThread = new Thread(new LoadMainContent(), "Post Content Loader");
        loadMainContentThread.start();
        refreshControlsThread.start();
    }

    private class LoadMainContent implements Runnable
    {
        private final String sourceImageUrl = photoPost.getPhotos().get(photoNumberInPost).getSizes().get(0).getUrl();
        private boolean isZeroTimed;

        @Override
        public void run()
        {
            ImageIcon icon;
            try
            {
                URL url = new URL(sourceImageUrl);
                icon = new ImageIcon(url);
                contentLabel.setIcon(icon);
                contentLabel.setText(null); //Get rid of "Loading..." message
                isZeroTimed = GIFzeroTimedWorkaround.isZeroTimedGif(url);

            }
            catch (MalformedURLException e)
            {
                contentLabel.setText(e.getMessage());
                return;
            }

            if (isZeroTimed)
            {
                icon.setImageObserver(new GIFzeroTimedWorkaround.ImageObserverWorkaround(contentLabel));
            }
        }
    }
}
