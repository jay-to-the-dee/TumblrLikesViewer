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

import com.tumblr.jumblr.types.Note;
import java.awt.*;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import static tumblrviewer.MainViewGUI.SINGLE_VIEW_MODE;
import tumblrviewer.TumblrBackend.DisplayModes;

/**
 * This is the dialog window which offers the user the user a big
 * list of blogs to follow.
 *
 * @author Jonathan <jay-to-the-dee@users.noreply.github.com>
 */
public class MoreBlogLinks implements ActionListener
{
    final private JFrame jFrame;
    final private String menuItemText;
    private final Collection<Note> notes;
    private Collection<String> userList;
    private final TumblrBackend tumblrBackend;
    private JList list;
    private JDialog jDialog;
    private JButton gotoBlogButton;
    final private LinkedHashSet<BlogListing> blogListings;

    final private static int BORDER_SIZE = 10;

    public MoreBlogLinks(JFrame jFrame, String menuItemText, Collection<Note> notes, Collection<String> userList, TumblrBackend tumblrBackend)
    {
        this.jFrame = jFrame;
        this.menuItemText = menuItemText;
        this.notes = notes;
        this.userList = userList;
        this.tumblrBackend = tumblrBackend;

        if (userList == null && notes != null)
        {
            //We're in notes mode!
            this.blogListings = new LinkedHashSet<>();
            for (Note note : notes)
            {
                String typePastTense = tumblrBackend.getNoteTypePastTense(note.getType());
                String noteString = note.getBlogName() + " " + typePastTense + " this";
                this.blogListings.add(new BlogListing(note.getBlogName(), noteString));
            }
        }
        else
        {
            this.blogListings = new LinkedHashSet<>();
            for (String user : userList)
            {
                this.blogListings.add(new BlogListing(user, user));
            }
        }
    }

    private class BlogListing
    {
        final String blogName;
        final String blogListingText;

        public BlogListing(String blogName, String blogListingText)
        {
            this.blogName = blogName;
            this.blogListingText = blogListingText;
        }

        public String getBlogName()
        {
            return blogName;
        }

        @Override
        public String toString()
        {
            return blogListingText;
        }

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        jDialog = new JDialog(jFrame, menuItemText);
        jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        list = new JList(blogListings.toArray());
        list.addListSelectionListener(new BlogLinksListSelectionListener());

        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(400, 600));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        Container contentPane = jDialog.getContentPane();

        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        listPane.add(listScroller);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        gotoBlogButton = new JButton("Go to selected blog");
        gotoBlogButton.setMnemonic('G');
        gotoBlogButton.setEnabled(false);
        gotoBlogButton.addActionListener(new GotoBlogActionListener());
        buttonPane.add(gotoBlogButton);

        //Start here as this is where we left off on our +N more blogs menu
        list.setSelectedIndex(MainViewGUI.MAXIMUM_BLOG_LINKS_PER_MENU);
        list.ensureIndexIsVisible(list.getSelectedIndex());

        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        jDialog.pack();
        jDialog.setVisible(true);
    }

    private class GotoBlogActionListener implements ActionListener
    {
        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public void actionPerformed(ActionEvent e)
        {
            BlogListing blogListing = (BlogListing) list.getSelectedValue();
            String blogToGoTo =  blogListing.getBlogName();
            jDialog.dispose();  //Dispose of this dialog

            new MainViewGUI(DisplayModes.POSTS, blogToGoTo);
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose(); //Dispose of previous blog
            }
        }
    }

    private class BlogLinksListSelectionListener implements ListSelectionListener
    {
        String previouslyViewedBlogName = "";
        private AddBlogTitleToButton loadBlogTitleWorker;
        private AddBlogAvatarToAbstractButton loadBlogIconWorker;

        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            JList jList = (JList) e.getSource();
            BlogListing blogListing = (BlogListing) list.getSelectedValue();
            String blogName =  blogListing.getBlogName();
            String newText = "<html><p>Go to <b>" + blogName + "</b></p><br><p><br></p></html>";

            if (previouslyViewedBlogName.equals(blogName))
            {
                return; //Do not update if same as before
            }
            else
            {
                previouslyViewedBlogName = blogName;
            }

            gotoBlogButton.setEnabled(true);
            gotoBlogButton.setText(newText);

            if (loadBlogIconWorker != null)
            {
                loadBlogIconWorker.cancel(true);
            }
            gotoBlogButton.setIcon(MainViewGUI.loading64ImageIcon);
            loadBlogIconWorker = new AddBlogAvatarToAbstractButton(tumblrBackend, gotoBlogButton, blogName, 64);
            loadBlogIconWorker.execute();

            if (loadBlogTitleWorker != null)
            {
                loadBlogTitleWorker.cancel(true);
            }
            loadBlogTitleWorker = new AddBlogTitleToButton(blogName);
            loadBlogTitleWorker.execute();
        }

        private class AddBlogTitleToButton extends SwingWorker<String, Object>
        {
            final String blogName;

            public AddBlogTitleToButton(String blogName)
            {
                this.blogName = blogName;
            }

            @Override
            protected void done()
            {
                try
                {
                    String blogTitle = get();
                    if (blogTitle.isEmpty())
                    {
                        blogTitle = "<br>";
                    }
                    String newText = "<html><p>Go to <b>" + blogName + "</b></p><br><p>" + blogTitle + "</p></html>";

                    gotoBlogButton.setText(newText);
                }
                catch (CancellationException | InterruptedException | ExecutionException ex)
                {
                }
            }

            @Override
            protected String doInBackground() throws Exception
            {
                return tumblrBackend.getBlogInfo(blogName).getTitle();
            }
        }

    }

}
