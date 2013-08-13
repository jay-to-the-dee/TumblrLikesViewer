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
    private final static boolean LOAD_AVATAR_MENU_ICONS = true;

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
            AddMenuItemBlogIcon addMenuItemBlogIcon = new AddMenuItemBlogIcon(blogNameMenuItem);
            (new Thread(addMenuItemBlogIcon, "Blog Icon MenuItem - " + blogName)).start();
        }
    }

    private class RebloggedFromActionListener implements ActionListener
    {
        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            JComponent sourceButton = (JComponent) e.getSource();
            sourceButton.setEnabled(false);
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

    private class AddMenuItemBlogIcon implements Runnable
    {
        JMenuItem blogNameMenuItem;

        public AddMenuItemBlogIcon(JMenuItem blogNameMenuItem)
        {
            this.blogNameMenuItem = blogNameMenuItem;
        }

        @Override
        public void run()
        {
            blogNameMenuItem.setIcon(tumblrBackend.getAvatar(blogName));
        }
    }
}
