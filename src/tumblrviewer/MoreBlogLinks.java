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

import java.awt.*;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.*;
import static tumblrviewer.MainViewGUI.SINGLE_VIEW_MODE;
import tumblrviewer.TumblrBackend.DisplayModes;

/**
 *
 * @author Jonathan <jay-to-the-dee@users.noreply.github.com>
 */
public class MoreBlogLinks implements ActionListener
{
    final private JFrame jFrame;
    final private String menuItemText;
    final private Collection<String> userList;
    private JList list;
    private JDialog jDialog;

    final private static int BORDER_SIZE = 10;

    public MoreBlogLinks(JFrame jFrame, String menuItemText, Collection<String> userList)
    {
        this.jFrame = jFrame;
        this.menuItemText = menuItemText;
        this.userList = userList;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        jDialog = new JDialog(jFrame, menuItemText);
        jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        list = new JList(userList.toArray());

        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(320, 600));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        Container contentPane = jDialog.getContentPane();

        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        listPane.add(listScroller);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        JButton gotoBlogButton = new JButton("Go to blog");
        gotoBlogButton.addActionListener(new GotoBlogActionListener());
        buttonPane.add(gotoBlogButton);

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
            String blogToGoTo = (String) list.getSelectedValue();
            jDialog.dispose();  //Dispose of this dialog

            new MainViewGUI(DisplayModes.POSTS, blogToGoTo);
            if (SINGLE_VIEW_MODE)
            {
                jFrame.dispose(); //Dispose of previous blog
            }
        }
    }
}
