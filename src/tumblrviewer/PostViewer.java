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

import com.tumblr.jumblr.types.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import static tumblrviewer.MainViewGUI.SINGLE_VIEW_MODE;
import tumblrviewer.TumblrBackend.DisplayModes;

/**
 *
 * @author jonathan
 */
public abstract class PostViewer
{
    private JFrame jFrame;
    private Container panel;
    private final PhotoPost photoPost;
    private final MainViewGUI mainViewGUI;
    private final TumblrBackend tumblrBackend;
    private JMenu likedOrNotMenu;
    private JMenuItem likeItem;
    private JMenuItem unlikeItem;
    private JMenu rebloggedFromMenu;
    private JMenu notesMenu;
    private JMenuItem goToRebloggedFromItem;
    private JMenu reblogMenu;
    JLabel contentLabel;

    public PostViewer(MainViewGUI mainViewGUI, TumblrBackend tumblrBackend, PhotoPost photoPost)
    {
        this.mainViewGUI = mainViewGUI;
        this.tumblrBackend = tumblrBackend;
        this.photoPost = photoPost;

        createGUI();
    }

    private void createGUI()
    {
        jFrame = new JFrame(java.util.ResourceBundle.getBundle("en_gb").getString("IMAGE FROM ") + photoPost.getBlogName());

        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        panel = jFrame.getContentPane();
        panel.setLayout(new BorderLayout());

        contentLabel = new JLabel("Loading...");
        contentLabel.setHorizontalAlignment(JLabel.CENTER);

        contentLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                jFrame.dispose();
            }
        });

        JScrollPane jsp = new JScrollPane(contentLabel);

        jsp.getVerticalScrollBar().setUnitIncrement(25);
        jsp.setBorder(null);

        panel.add(jsp, BorderLayout.CENTER);
        jFrame.setJMenuBar(createMenuBar());

        jFrame.pack();
        jFrame.setVisible(true);
    }

    void loadFromTumblr()
    {
        Thread refreshControlsThread = new Thread(new RefreshControls(), "PostViewer Refresh Controls");
        loadMainContent(refreshControlsThread);
    }

    abstract void loadMainContent(Thread refreshControlsThread);

    private JMenuBar createMenuBar()
    {
        JMenuBar jMenuBar = new JMenuBar();

        likedOrNotMenu = new JMenu("...");
        likeItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("LIKE"));
        unlikeItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("UNLIKE"));

        likeItem.addActionListener(new LikeButtonActionListener());
        unlikeItem.addActionListener(new LikeButtonActionListener());

        likedOrNotMenu.add(likeItem);
        likedOrNotMenu.add(unlikeItem);
        likedOrNotMenu.setEnabled(false);

        reblogMenu = new JMenu(java.util.ResourceBundle.getBundle("en_gb").getString("REBLOG"));
        JMenuItem reblogThisItem = new JMenuItem(java.util.ResourceBundle.getBundle("en_gb").getString("REBLOG THIS"));

        reblogThisItem.addActionListener(new ReblogThisItemActionListener());

        reblogMenu.add(reblogThisItem);

        notesMenu = new JMenu("...");
        notesMenu.setEnabled(false);

        rebloggedFromMenu = new JMenu();

        goToRebloggedFromItem = new JMenuItem("");
        rebloggedFromMenu.add(goToRebloggedFromItem);

        jMenuBar.add(rebloggedFromMenu);
        jMenuBar.add(reblogMenu);
        jMenuBar.add(likedOrNotMenu);
        jMenuBar.add(notesMenu);

        return jMenuBar;
    }

    private class RefreshControls implements Runnable
    {
        private class SetLikeButton implements Runnable
        {
            @Override
            public void run()
            {
                //Liked or not
                if (tumblrBackend.isCurrentlyLiked(photoPost))
                {
                    likedOrNotMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("LIKED"));
                    likeItem.setEnabled(false);
                    unlikeItem.setEnabled(true);
                }
                else
                {
                    likedOrNotMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("LIKE"));
                    likeItem.setEnabled(true);
                    unlikeItem.setEnabled(false);
                }
                likedOrNotMenu.setEnabled(true);
            }
        }

        private class SetNotesInfo implements Runnable
        {
            private class SetUpNotesMenu extends SwingWorker<Note[], Object>
            {
                @Override
                public void done()
                {
                    try
                    {
                        Note[] notes = get();
                        if (notes == null)
                        {
                            throw new Exception("No notes!");
                        }
                        for (Note note : notes)
                        {
                            String typePastTense = note.getType();
                            switch (typePastTense)
                            {
                                case "reblog":
                                    typePastTense = "reblogged";
                                    break;
                                case "like":
                                    typePastTense = "liked";
                                    break;
                            }

                            AddBlogMenuLink addBlogMenuLinkThread = new AddBlogMenuLink(tumblrBackend, note.getBlogName(), notesMenu, jFrame);
                            addBlogMenuLinkThread.setMenuItemText(note.getBlogName() + " " + typePastTense + " this");
                            addBlogMenuLinkThread.setMainGUIJFrame(mainViewGUI.getJFrame());
                            new Thread(addBlogMenuLinkThread).start();
                        }
                        notesMenu.setEnabled(true);
                    }
                    catch (Exception ex)
                    {
                        notesMenu.setEnabled(false);
                        //There are no notes, so return (stops NullPointerException)
                    }
                }

                @Override
                protected Note[] doInBackground() throws Exception
                {
                    return tumblrBackend.getCurrentNotes(photoPost);
                }
            }

            @Override
            public void run()
            {
                //Start notes menu thread
                (new Thread(new SetUpNotesMenu())).start();

                //Rest of notes (the counter)
                int noteCount = tumblrBackend.getCurrentNoteCount(photoPost);
                notesMenu.setText(noteCount + " notes");
            }
        }

        private class SetReblogInfo extends SwingWorker<Map<String, Object>, Object>
        {
            @Override
            public Map<String, Object> doInBackground()
            {
                Map<String, Object> map = new HashMap();
                String rebloggedFrom = photoPost.getRebloggedFromName();
                map.put("rebloggedFrom", rebloggedFrom);
                map.put("standardAvatar", tumblrBackend.getAvatar(rebloggedFrom));
                map.put("bigAvatar", tumblrBackend.getAvatar(rebloggedFrom, 64));
                return map;
            }

            @Override
            protected void done()
            {
                try
                {
                    String rebloggedFrom = (String) get().get("rebloggedFrom");
                    if (rebloggedFrom != null)
                    {
                        goToRebloggedFromItem.addActionListener(new RebloggedFromActionListener(rebloggedFrom));
                        rebloggedFromMenu.setIcon((Icon) get().get("standardAvatar"));
                        rebloggedFromMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("REBLOGGED FROM ") + rebloggedFrom);
                        goToRebloggedFromItem.setText("Go to " + rebloggedFrom);
                        goToRebloggedFromItem.setIcon((Icon) get().get("bigAvatar"));
                    }
                    else
                    {
                        throw new Exception("Nothing to reblog");
                    }
                }
                catch (Exception ex)
                {
                    rebloggedFromMenu.setVisible(false);
                }
            }
        }

        @Override
        public void run()
        {
            (new Thread(new SetLikeButton())).start();
            notesMenu.removeAll();
            (new Thread(new SetNotesInfo())).start();
            (new Thread(new SetReblogInfo(), "Reblog Info load Thread")).start();
        }
    }

    private class RebloggedFromActionListener implements ActionListener
    {
        String rebloggedFrom;

        public RebloggedFromActionListener(String rebloggedFrom)
        {
            this.rebloggedFrom = rebloggedFrom;
        }

        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            JComponent sourceButton = (JComponent) e.getSource();
            sourceButton.setEnabled(false);
            new MainViewGUI(DisplayModes.POSTS, rebloggedFrom);
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose();
                mainViewGUI.getJFrame().dispose();
            }
        }
    }

    private class LikeButtonActionListener implements ActionListener, Runnable
    {
        private Thread loaderThread;
        JMenuItem source;

        @Override
        public void actionPerformed(ActionEvent e)
        {
            source = (JMenuItem) e.getSource();
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
            likedOrNotMenu.setEnabled(false);
            if (source.getText().equals(java.util.ResourceBundle.getBundle("en_gb").getString("UNLIKE")))
            {
                likedOrNotMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("UNLIKING"));
                photoPost.unlike();
            }
            else
            {
                likedOrNotMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("LIKING"));
                photoPost.like();
            }
            (new Thread(new RefreshControls())).start();
            loaderThread = null;
        }
    }

    private class ReblogThisItemActionListener implements ActionListener, Runnable
    {
        private JMenuItem source;
        private ActionEvent e;
        private Thread loaderThread;

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
            source = (JMenuItem) e.getSource();
            source.setEnabled(false);
            reblogMenu.setEnabled(false);
            reblogMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("REBLOGGING..."));
            tumblrBackend.reblogThis(photoPost);
            reblogMenu.setText(java.util.ResourceBundle.getBundle("en_gb").getString("REBLOGGED!"));
            loaderThread = null;
        }
    }
}
