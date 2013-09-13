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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import static tumblrviewer.MainViewGUI.SINGLE_VIEW_MODE;
import tumblrviewer.TumblrBackend.DisplayModes;

/**
 * This class adds blog JMenuItems to a JMenu and gets the profile picture, sets
 * the text and action listener to open a new blog.
 *
 * @author jonathan
 */
public class AddBlogMenuLink implements Runnable
{
    private static boolean LOAD_AVATAR_MENU_ICONS;

    private final TumblrBackend tumblrBackend;
    private final String blogName;
    private String menuItemText;
    private final JMenu menu;
    private final JFrame jFrame;
    private JFrame mainGUIJFrame;

    public AddBlogMenuLink(TumblrBackend tumblrBackend, String blogName, JMenu menu, JFrame jFrame)
    {
        this.tumblrBackend = tumblrBackend;
        this.blogName = blogName;
        this.menuItemText = blogName;
        this.menu = menu;
        this.jFrame = jFrame;
        
        LOAD_AVATAR_MENU_ICONS = MainViewGUI.prefs.getBoolean("LOAD_AVATAR_MENU_ICONS", true);
    }

    public void setMenuItemText(String menuItemText)
    {
        this.menuItemText = menuItemText;
    }

    public void setMainGUIJFrame(JFrame mainGUIJFrame)
    {
        this.mainGUIJFrame = mainGUIJFrame;
    }

    @Override
    public void run()
    {
        JMenuItem blogNameMenuItem;
        try
        {
            blogNameMenuItem = new JMenuItem(menuItemText);
        }
        catch (ClassCastException e)
        {
            return;
            //Just ignore this error - it's some kind of Nimbus internal thing
        }
        blogNameMenuItem.addActionListener(new RebloggedFromActionListener());
        menu.add(blogNameMenuItem);

        if (LOAD_AVATAR_MENU_ICONS)
        {
            AddBlogAvatarToAbstractButton addMenuItemBlogIcon = new AddBlogAvatarToAbstractButton(tumblrBackend, blogNameMenuItem, blogName, 16);
            addMenuItemBlogIcon.execute();
        }
    }

    private class RebloggedFromActionListener implements ActionListener
    {
        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            JComponent sourceButton = (JComponent) e.getSource();
            new MainViewGUI(DisplayModes.POSTS, blogName);
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose();
                if (mainGUIJFrame != null)
                {
                    mainGUIJFrame.dispose();
                }
            }
        }
    }
}
