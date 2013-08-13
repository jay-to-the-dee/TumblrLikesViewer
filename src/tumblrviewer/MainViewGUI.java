package tumblrviewer;

import com.tumblr.jumblr.types.*;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.event.*;
import tumblrviewer.TumblrBackend.DisplayModes;

/**
 * This class creates the GUI that is the main view that is seen when scrolling 
 * through a blog's posts, likes or dashboard.
 * @author jonathan
 */
public class MainViewGUI
{
    static final boolean SINGLE_VIEW_MODE = true;
    private static final boolean AUTO_LOAD_AT_PAGE_END = true;
    /* End of constants*/
    private final TumblrBackend tumblrBackend;
    private final JFrame jFrame;
    private final Container panel;
    private final Container imageDisplay;
    private int imagesLoadedCounter = 0;
    private JMenu followingOrNotMenu;
    LinkedHashMap<DisplayModes, JRadioButtonMenuItem> modeItems;
    private JMenu modeSelectMenu;
    private JMenuItem followBlogMenuItem;
    private JMenuItem unfollowBlogMenuItem;
    private JMenu currentUserOptionsMenu;
    private JMenuItem currentUserGoToMenu;
    private JMenuItem avatarIconViewMenuItem;
    private JMenu currentUserFollowingMenu;
    private JMenu currentUserFollowersMenu;

    /**
     * Creates a new GUI for scrolling through a blog. More than one instance 
     * can be run at the same time. However every instance of this class will 
     * also make a new TumblrBackend class within it.
     * @param currentDisplayMode - whether to show posts, likes or dashboard
     * @param blogToView the blog name to view - if null will resolve to the 
     * current signed in user's home blog
    */
    public MainViewGUI(DisplayModes currentDisplayMode, String blogToView)
    {
        jFrame = new JFrame("Tumblr");
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        tumblrBackend = new TumblrBackend(this, currentDisplayMode, blogToView);

        panel = jFrame.getContentPane();
        panel.setLayout(new BorderLayout());

        imageDisplay = new Container();
        imageDisplay.setLayout(new BoxLayout(imageDisplay, BoxLayout.Y_AXIS));

        JScrollPane jsp = new JScrollPane(imageDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setWheelScrollingEnabled(true);
        jsp.getVerticalScrollBar().setUnitIncrement(50);
        jsp.setBorder(null);

        JButton moreButton = new JButton(java.util.ResourceBundle.getBundle("en_gb").getString("LOAD MORE"));
        moreButton.setMnemonic('L');
        moreButton.addActionListener(new LoadMoreActionListener(tumblrBackend, moreButton));

        if (AUTO_LOAD_AT_PAGE_END)
        {
            jsp.getViewport().addChangeListener(new checkAtBottomChangeListener(moreButton));
        }
        else
        {
            panel.add(moreButton, BorderLayout.PAGE_END);
        }
        panel.add(jsp, BorderLayout.CENTER);

        jFrame.setJMenuBar(createMenuBar());

        jFrame.setVisible(true);
        jFrame.setMinimumSize(new Dimension(TumblrBackend.PHOTO_PREFERRED_SIZE + 25, TumblrBackend.PHOTO_PREFERRED_SIZE));

        moreButton.doClick();
    }

    /**
     * Adds an image to the main view by passing a URL
     *
     * @param previewImageUrl The url with reference to the image
     * @param photoPost The photoPost object that we are going to add
     * @param photoNumberInPost The number post in the photoPost object that
     * previewImageUrl refers to
     */
    public void addImage(final String previewImageUrl, final PhotoPost photoPost, final int photoNumberInPost)
    {
        ImageIcon icon;
        boolean isZeroTimed;
        try
        {
            URL url = new URL(previewImageUrl);
            icon = new ImageIcon(url);
            isZeroTimed = GIFzeroTimedWorkaround.isZeroTimedGif(url);
        }
        catch (MalformedURLException e)
        {
            return; //No point in completing rest of method
        }

        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(icon);

        if (isZeroTimed)
        {
            icon.setImageObserver(new GIFzeroTimedWorkaround.ImageObserverWorkaround(imageLabel));
        }

        /*label.setText(tooltipText);
         label.setVerticalTextPosition(JLabel.BOTTOM);
         label.setHorizontalTextPosition(JLabel.CENTER);*/
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final String blogName = photoPost.getBlogName();
        if (!blogName.isEmpty())
        {
            imageLabel.setToolTipText(blogName);
        }

        imageLabel.addMouseListener(new imageLabelMouseAdapter(this, photoPost, photoNumberInPost));

        imageDisplay.add(imageLabel);
        imagesLoadedCounter++;
        jFrame.setTitle(tumblrBackend.guiTitle + " (" + imagesLoadedCounter + ")");
        panel.revalidate();
    }

    private static void setGUITheme()
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            /*try
             {
             for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
             {
             if ("Nimbus".equals(info.getName()))
             {
             UIManager.setLookAndFeel(info.getClassName());
             break;
             }
             }
             }
             catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e2)
             {
             }
             /**/
        }
    }

    /**
     * The JFrame that holds this GUI instance
     * @return the JFrame object
     */
    public JFrame getJFrame()
    {
        return jFrame;
    }

    private class LoadMoreActionListener implements ActionListener, Runnable
    {
        private final TumblrBackend tumblrBackend;
        private final JButton button;
        private Thread loaderThread;

        public LoadMoreActionListener(TumblrBackend tumblrBackend, JButton button)
        {
            this.tumblrBackend = tumblrBackend;
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (loaderThread != null)
            {
                return;
            }
            loaderThread = new Thread((Runnable) this, "Load More Posts Thread");
            loaderThread.start();
            button.setEnabled(false);
            button.setText(java.util.ResourceBundle.getBundle("en_gb").getString("LOADING"));
        }

        @Override
        public void run()
        {
            tumblrBackend.tumblrLoadMore();
            button.setEnabled(true);
            button.setText(java.util.ResourceBundle.getBundle("en_gb").getString("LOAD MORE"));
            loaderThread = null;
        }
    }

    private static class checkAtBottomChangeListener implements ChangeListener
    {
        private final JButton moreButton;

        public checkAtBottomChangeListener(JButton moreButton)
        {
            this.moreButton = moreButton;
        }

        @Override
        public void stateChanged(ChangeEvent e)
        {
            JViewport source = (JViewport) e.getSource();
            int bottomOfCurrentScrollPosition = (int) source.getViewPosition().getY() + (int) source.getExtentSize().getHeight();
            int bottomOfCompleteView = source.getView().getHeight();

            if ((bottomOfCurrentScrollPosition - bottomOfCompleteView) >= 0)
            {
                //We have reached the bottom!
                moreButton.doClick();
            }
        }
    }

    /**
     * Starts off the main program by opening an instance with the current 
     * signed in user's blog posts.
     * @param args - no arguments accepted currently
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args)
    {
        setGUITheme();
        new MainViewGUI(DisplayModes.POSTS, null);
    }

    private JMenuBar createMenuBar()
    {
        JMenuBar jMenuBar = new JMenuBar();

        modeSelectMenu = new JMenu("...");
        modeSelectMenu.setEnabled(false);
        ButtonGroup modeSelectGroup = new ButtonGroup();
        modeItems = new LinkedHashMap<>();
        modeItems.put(DisplayModes.POSTS, new JRadioButtonMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("POSTS")));
        modeItems.put(DisplayModes.LIKES, new JRadioButtonMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("LIKES")));
        modeItems.put(DisplayModes.DASHBOARD, new JRadioButtonMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("DASHBOARD")));

        for (JRadioButtonMenuItem modeItem : modeItems.values())
        {
            modeSelectGroup.add(modeItem);
            modeSelectMenu.add(modeItem);
            modeItem.addActionListener(new ModeSelectActionListener());
            modeItem.setEnabled(false);
        }
        avatarIconViewMenuItem = new JMenuItem();
        avatarIconViewMenuItem.setVisible(false);
        avatarIconViewMenuItem.addActionListener(new avatarIconViewMenuItemActionListener());
        modeSelectMenu.add(avatarIconViewMenuItem);

        followingOrNotMenu = new JMenu("...");
        followingOrNotMenu.setEnabled(false);

        followBlogMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("FOLLOW"));
        unfollowBlogMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("UNFOLLOW"));

        followBlogMenuItem.addActionListener(new FollowOrUnfollowMenuItemActionListener());
        unfollowBlogMenuItem.addActionListener(new FollowOrUnfollowMenuItemActionListener());

        followingOrNotMenu.add(followBlogMenuItem);
        followingOrNotMenu.add(unfollowBlogMenuItem);

        currentUserOptionsMenu = new JMenu(java.util.ResourceBundle.getBundle("en_gb").getString("GO TO"));
        currentUserOptionsMenu.setEnabled(false);

        currentUserGoToMenu = new JMenuItem("..."); //Will be replaced by users home blog
        currentUserGoToMenu.addActionListener(new currentUserGoToMenuItemActionListener());

        currentUserFollowingMenu = new JMenu(java.util.ResourceBundle.getBundle("en_gb").getString("FOLLOWING"));
        currentUserFollowingMenu.setEnabled(false);
        currentUserFollowersMenu = new JMenu(java.util.ResourceBundle.getBundle("en_gb").getString("FOLLOWERS"));
        currentUserFollowersMenu.setEnabled(false);

        JMenuItem currentUserEnterBlogNameMenuItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("ENTER BLOG NAME"));
        currentUserEnterBlogNameMenuItem.addActionListener(new EnterBlogNameMenuItemActionListener());

        currentUserOptionsMenu.add(currentUserGoToMenu);
        currentUserOptionsMenu.add(currentUserEnterBlogNameMenuItem);
        currentUserOptionsMenu.add(currentUserFollowingMenu);
        currentUserOptionsMenu.add(currentUserFollowersMenu);

        jMenuBar.add(modeSelectMenu);
        jMenuBar.add(currentUserOptionsMenu);
        jMenuBar.add(followingOrNotMenu);

        (new Thread(new RefreshControls(), "MainViewGUI Refresh Controls")).start();

        return jMenuBar;

    }

    private class RefreshControls implements Runnable
    {
        @Override
        public void run()
        {
            (new Thread(new SetModeMenuItems(), "Mode Menu Setup")).start();
            (new Thread(new SetUpFollowingOrNotMenu(), "Following or Not Menu Setup")).start();
            (new Thread(new SetUpCurrentUserOptionsMenu(), "Current User Options Setup")).start();

            (new Thread(new SetUpCurrentUserFollowingMenu(), "Following Menu Setup")).start();
            (new Thread(new SetUpCurrentUserFollowersMenu(), "Followers Menu Setup")).start();
        }

        private class SetModeMenuItems implements Runnable
        {
            @Override
            public void run()
            {
                modeSelectMenu.setText(tumblrBackend.getCurrentViewingBlog());
                modeSelectMenu.setEnabled(true);

                //Update modeMenuItem to whatever we are currently viewing as
                JMenuItem toBeSelectedMenuItem = modeItems.get(tumblrBackend.getCurrentDisplayMode());
                toBeSelectedMenuItem.setSelected(true);

                modeItems.get(DisplayModes.POSTS).setEnabled(true);
                if (tumblrBackend.isCurrentUsersBlog())
                {
                    modeItems.get(DisplayModes.DASHBOARD).setEnabled(true);
                }

                if (tumblrBackend.canViewLikes()) //Put this last as it involves a backend request
                {
                    modeItems.get(DisplayModes.LIKES).setEnabled(true);
                }

                //Add avatar to mode selection menu
                modeSelectMenu.setIcon(tumblrBackend.getAvatar(tumblrBackend.getCurrentViewingBlog()));

                //Add avatar to end of menu
                avatarIconViewMenuItem.setIcon(tumblrBackend.getAvatar(tumblrBackend.getCurrentViewingBlog(), 64));
                avatarIconViewMenuItem.setText(tumblrBackend.getCurrentViewingBlog());
                avatarIconViewMenuItem.setVisible(true);
            }
        }

        private class SetUpFollowingOrNotMenu implements Runnable
        {
            @Override
            public void run()
            {
                if (tumblrBackend.isCurrentUsersBlog())
                {
                    followingOrNotMenu.setText(null);
                }
                else
                {
                    boolean isCurrentlyFollowing = tumblrBackend.isFollowing(tumblrBackend.getCurrentViewingBlog());
                    followingOrNotMenu.setEnabled(true);
                    if (isCurrentlyFollowing)
                    {
                        followingOrNotMenu.setText("Following");
                        followBlogMenuItem.setEnabled(false);
                        unfollowBlogMenuItem.setEnabled(true);
                    }
                    else
                    {
                        followingOrNotMenu.setText("Follow");
                        followBlogMenuItem.setEnabled(true);
                        unfollowBlogMenuItem.setEnabled(false);
                    }
                }
            }
        }

        private class SetUpCurrentUserOptionsMenu implements Runnable
        {
            @Override
            public void run()
            {
                if (tumblrBackend.isCurrentUsersBlog())
                {
                    currentUserGoToMenu.setEnabled(false);
                    currentUserGoToMenu.setVisible(false);
                }

                String currentUsersName = tumblrBackend.getCurrentUsersName();
                currentUserGoToMenu.setText(currentUsersName);
                currentUserGoToMenu.setIcon(tumblrBackend.getAvatar(currentUsersName, 64));
                currentUserOptionsMenu.setEnabled(true);
            }
        }

        private class SetUpCurrentUserFollowingMenu implements Runnable
        {
            int menuMax = 25;

            @Override
            public void run()
            {
                for (String blogName : tumblrBackend.getAllUserFollowing())
                {
                    AddBlogMenuLink addBlogMenuLinkThread = new AddBlogMenuLink(tumblrBackend, blogName, currentUserFollowingMenu, jFrame);
                    new Thread(addBlogMenuLinkThread).start();
                }
                currentUserFollowingMenu.setEnabled(true);
            }
        }

        private class SetUpCurrentUserFollowersMenu implements Runnable
        {
            @Override
            public void run()
            {
                for (String blogName : tumblrBackend.getAllUserFollowers())
                {
                    AddBlogMenuLink addBlogMenuLinkThread = new AddBlogMenuLink(tumblrBackend, blogName, currentUserFollowersMenu, jFrame);
                    new Thread(addBlogMenuLinkThread).start();
                }
                currentUserFollowersMenu.setEnabled(true);
            }
        }
    }

    private class ModeSelectActionListener implements ActionListener
    {
        private DisplayModes justSelectedDisplayMode;

        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            //Find what DisplayMode was selected
            for (Entry<DisplayModes, JRadioButtonMenuItem> modeItem : modeItems.entrySet())
            {
                if (modeItem.getValue() == e.getSource())
                {
                    justSelectedDisplayMode = modeItem.getKey();
                }
            }
            new MainViewGUI(justSelectedDisplayMode, tumblrBackend.getCurrentViewingBlog());
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose();
            }
        }
    }

    private class FollowOrUnfollowMenuItemActionListener implements ActionListener, Runnable
    {
        private Thread loaderThread;
        private ActionEvent e;

        @Override
        public void actionPerformed(ActionEvent e)
        {
            this.e = e;
            if (loaderThread != null)
            {
                return;
            }
            loaderThread = new Thread((Runnable) this);
            loaderThread.start();
        }

        @Override
        public void run()
        {
            followingOrNotMenu.setEnabled(false);
            if (e.getSource() == unfollowBlogMenuItem)
            {
                followingOrNotMenu.setText("Unfollowing...");
                tumblrBackend.unfollowBlog(tumblrBackend.getCurrentViewingBlog());
            }
            else
            {
                followingOrNotMenu.setText("Following...");
                tumblrBackend.followBlog(tumblrBackend.getCurrentViewingBlog());
            }
            (new Thread(new RefreshControls(), "MainViewGUI Refresh Controls")).start();
            loaderThread = null;
        }
    }

    private class imageLabelMouseAdapter extends MouseAdapter implements Runnable
    {
        private final PhotoPost photoPost;
        private final int photoNumberInPost;
        private final MainViewGUI mainViewGUI;
        private Thread loaderThread;

        public imageLabelMouseAdapter(MainViewGUI mainViewGUI, PhotoPost photoPost, int photoNumberInPost)
        {
            this.photoPost = photoPost;
            this.photoNumberInPost = photoNumberInPost;
            this.mainViewGUI = mainViewGUI;
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (loaderThread != null)
            {
                return;
            }
            loaderThread = new Thread((Runnable) this);
            loaderThread.start();
        }

        @Override
        public void run()
        {
            PhotoPostViewer photoPostViewer = new PhotoPostViewer(mainViewGUI, tumblrBackend, photoPost, photoNumberInPost);
            loaderThread = null;
        }
    }

    private class currentUserGoToMenuItemActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JComponent sourceButton = (JComponent) e.getSource();
            sourceButton.setEnabled(false);

            MainViewGUI mainViewGUI = new MainViewGUI(DisplayModes.POSTS, tumblrBackend.getCurrentUsersName());
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose();
            }
        }
    }

    private class avatarIconViewMenuItemActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            final JFrame frame = new JFrame("Avatar viewer");

            JLabel jLabel = new JLabel();
            jLabel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    frame.dispose();
                }
            });
            jLabel.setIcon(tumblrBackend.getAvatar(tumblrBackend.getCurrentViewingBlog(), 512));

            frame.add(jLabel);
            frame.pack();
            frame.setResizable(false);
            frame.setVisible(true);
        }
    }

    private class EnterBlogNameMenuItemActionListener implements ActionListener
    {
        String customBlogName;

        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            customBlogName = JOptionPane.showInputDialog(jFrame, "Enter the name of the Tumblr blog you wish to view:", java.util.ResourceBundle.getBundle("en_gb").getString("ENTER BLOG NAME"), JOptionPane.QUESTION_MESSAGE);

            if (customBlogName == null || customBlogName.isEmpty())
            {
                return;
            }

            if (!tumblrBackend.blogExists(customBlogName))
            {
                JOptionPane.showMessageDialog(jFrame, "Blog \"" + customBlogName + "\" does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new MainViewGUI(DisplayModes.POSTS, customBlogName);
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose();
            }
        }
    }
}
