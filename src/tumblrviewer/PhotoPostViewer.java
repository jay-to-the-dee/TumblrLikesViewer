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
    void loadMainContent(PostViewer postViewer)
    {
        Thread loadMainContentThread = new Thread(new LoadMainContent(), "Post Content Loader");
        loadMainContentThread.start();
        postViewer.doRefreshControls();
        photoPostAdditions();
    }

    private void photoPostAdditions()
    {
        if (photoPost.getPhotos().size() > 1)
        {
            String currentTitle = this.getjFrame().getTitle();
            String photoPostListProgressText = " (" + (photoNumberInPost + 1) + "/" + photoPost.getPhotos().size() + ")";
            this.getjFrame().setTitle(currentTitle + photoPostListProgressText);
        }
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
