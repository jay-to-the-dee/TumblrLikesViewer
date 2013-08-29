/*
 * Copyright (C) 2013 Jonathan <jay-to-the-dee@users.noreply.github.com>
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

import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * A class simply used to load up blog icon avatars onto any kind of button
 * @author Jonathan <jay-to-the-dee@users.noreply.github.com>
 */
public class AddBlogAvatarToAbstractButton extends SwingWorker<ImageIcon, Object>
{
    private final TumblrBackend tumblrBackend;
    private final AbstractButton blogNameMenuItem;
    private final String blogName;
    private final int avatarSize;

    private final static int IMAGE_FETCH_TIMEOUT_SECONDS = 30;

    public AddBlogAvatarToAbstractButton(TumblrBackend tumblrBackend, AbstractButton blogNameMenuItem, String blogName, int avatarSize)
    {
        super();
        this.tumblrBackend = tumblrBackend;
        this.blogNameMenuItem = blogNameMenuItem;
        this.blogName = blogName;
        this.avatarSize = avatarSize;
    }

    @Override
    protected void done()
    {
        try
        {
            ImageIcon avatarIcon = get(IMAGE_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (avatarIcon == null)
            {
                throw new Exception("Avatar not loaded");
            }
            blogNameMenuItem.setIcon(avatarIcon);
        }
        catch (Exception e)
        {
            System.err.println(e);
        }
    }

    @Override
    protected ImageIcon doInBackground()
    {
        return tumblrBackend.getAvatar(blogName, avatarSize);
    }
}
