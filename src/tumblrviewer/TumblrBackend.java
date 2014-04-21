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

import com.tumblr.jumblr.*;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 * This class serves as an interface between the Jumblr library and the GUI by
 * providing methods required for the rest of the programs's GUI interface. It
 * gets the data on other classes behalf.
 *
 * @author jonathan
 */
public class TumblrBackend
{
    private static int POSTS_LOADED_PER_UPDATE;
    private static boolean FULLSIZE_PHOTOS;
    static int PHOTO_PREFERRED_SIZE; // (100/250/400/500) (Default 500)
    final DisplayModes currentDisplayMode;
    /* End of constants*/
    private final MainViewGUI gui;
    private int currentUpdateIndex = 0;
    private JumblrClient client;
    private User user;
    final String guiTitle;
    private final String currentlyViewingBlog;
    static private HashSet<String> allUserFollowing;
    static private HashSet<String> allUserFollowers;

    public enum DisplayModes
    {
        POSTS, LIKES, DASHBOARD
    };

    public String getCurrentViewingBlog()
    {
        return currentlyViewingBlog;
    }

    public DisplayModes getCurrentDisplayMode()
    {
        return currentDisplayMode;
    }

    public TumblrBackend(MainViewGUI gui, DisplayModes currentDisplayMode, String currentlyViewingBlog)
    {
        this.gui = gui;
        this.currentDisplayMode = currentDisplayMode;

        POSTS_LOADED_PER_UPDATE = MainViewGUI.prefs.getInt("POSTS_LOADED_PER_UPDATE", 20);
        FULLSIZE_PHOTOS = MainViewGUI.prefs.getBoolean("FULLSIZE_PHOTOS", false);
        PHOTO_PREFERRED_SIZE = MainViewGUI.prefs.getInt("PHOTO_PREFERRED_SIZE", 500);

        try
        {
            client = new JumblrClient(java.util.ResourceBundle.getBundle("Keys").getString("consumer_key"), java.util.ResourceBundle.getBundle("Keys").getString("consumer_secret"));
            client.setToken(java.util.ResourceBundle.getBundle("Keys").getString("oauth_token"), java.util.ResourceBundle.getBundle("Keys").getString("oauth_token_secret"));
            user = client.user();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(gui.getJFrame(), e, "Connection Exception", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        if (currentlyViewingBlog == null)
        {
            this.currentlyViewingBlog = user.getName();
        }
        else
        {
            this.currentlyViewingBlog = currentlyViewingBlog;
        }

        guiTitle = java.util.ResourceBundle.getBundle("en_gb").getString(currentDisplayMode.name() + " FOR ") + this.currentlyViewingBlog;
        gui.getJFrame().setTitle(guiTitle);
    }

    public void reblogThis(Post post)
    {
        try
        {
            //client.postReblog(user.getName(), post.getId(), post.getReblogKey());
            post.reblog(user.getName());
        }
        catch (Exception e)
        {
        }
    }

    public void tumblrLoadMore()
    {
        tumblrUpdate(currentUpdateIndex);
        currentUpdateIndex++;
    }

    private void tumblrUpdate(int offsetIndex)
    {
        Map<String, Object> params = new HashMap<>();
        /*if (currentDisplayMode == DisplayModes.DASHBOARD && lastPostId != null)
         {
         params.put("since_id", lastPostId);
         }
         else
         {*/
        params.put("offset", offsetIndex * POSTS_LOADED_PER_UPDATE);
        /*}*/
        params.put("limit", POSTS_LOADED_PER_UPDATE);
        params.put("reblog_info", true);
        List<Post> posts = null;
        try
        {
            switch (currentDisplayMode)
            {
                case POSTS:
                    posts = client.blogPosts(currentlyViewingBlog, params);
                    break;
                case LIKES:
                    if (isCurrentUsersBlog())
                    {
                        posts = client.userLikes(params); //Use different method because current user might have set likes to private
                }
                else
                {
                    posts = client.blogLikes(currentlyViewingBlog, params);
                }
                    break;
                case DASHBOARD:
                    posts = client.userDashboard(params);
                    break;
            }
        }
        catch (Exception e)
        {
            System.err.println("Couldn't retrieve ANY posts!"); //NOI18N
            return;
        }

        for (Post post : posts)
        {
            try
            {
                PhotoPost photoPost = PhotoPost.class.cast(post);
                List<Photo> photosInPost = photoPost.getPhotos();
                for (Photo photo : photosInPost)
                {

                    List<PhotoSize> photoSizes = photo.getSizes();
                    for (PhotoSize photoSize : photoSizes)
                    {
                        if (!FULLSIZE_PHOTOS) //The first one is the fullsize photo so this won't run if FULLSIZE_PHOTOS is set to true
                        {
                            if (!photoSize.getUrl().contains("_" + PHOTO_PREFERRED_SIZE + "."))
                            {
                                continue; //Wrong size so contine
                            }
                        }
                        gui.addImage(photoSize.getUrl(), photoPost, photosInPost.indexOf(photo));
                        break; //Only add one of the image sizes - we have now done this
                    }
                }
            }
            catch (Exception e)
            {
                //e.printStackTrace();
                //Fail silently so that non-photo posts simply get ignored when the cast fails
            }
        }
    }

    public boolean isCurrentlyLiked(Post post)
    {
        String blogName = post.getBlogName();
        Long postId = post.getId();

        return client.blogPost(blogName, postId).isLiked();
    }

    public Long getCurrentNoteCount(Post post)
    {
        String blogName = post.getBlogName();
        Long postId = post.getId();

        Map<String, Object> params = new HashMap<>();
        params.put("id", postId);
        params.put("reblog_info", true);
        params.put("notes_info", true);

        java.util.List<Post> posts = client.blogPosts(blogName, params);
        PhotoPost detailedPhotoPost = PhotoPost.class.cast(posts.get(0));

        return detailedPhotoPost.getNoteCount();
    }

    public Note[] getCurrentNotes(Post post)
    {
        String blogName = post.getBlogName();
        Long postId = post.getId();

        Map<String, Object> params = new HashMap<>();
        params.put("id", postId);
        params.put("reblog_info", true);
        params.put("notes_info", true);

        java.util.List<Post> posts = client.blogPosts(blogName, params);
        PhotoPost detailedPhotoPost = PhotoPost.class.cast(posts.get(0));

        return (Note[]) detailedPhotoPost.getNotes().toArray();
    }

    public ImageIcon getAvatar(String user, int avatarSize)
    {
        //String avatarUrl = "http://api.tumblr.com/v2" + JumblrClient.blogPath(user, "/avatar/" + avatarSize);

        String avatarUrl = client.blogAvatar(user, avatarSize);
       
        try
        {
            URL url = new URL(avatarUrl);
            BufferedImage img;
            img = ImageIO.read(url);

            return new ImageIcon(img);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public boolean canViewLikes()
    {
        if (isCurrentUsersBlog() && !client.userLikes().isEmpty())
        {
            return true;
        }
        try
        {
            if (!client.blogInfo(currentlyViewingBlog).likedPosts().isEmpty())
            {
                return true;
            }
        }
        catch (JumblrException e)
        {
            return false;
        }
        return false;
    }

    public boolean isCurrentUsersBlog()
    {
        return client.user().getName().equals(currentlyViewingBlog);
    }

    public String getCurrentUsersName()
    {
        return client.user().getName();
    }

    public synchronized boolean isFollowing(String user)
    {
        return allUserFollowing.contains(user);
    }

    public synchronized AbstractCollection<String> getAllUserFollowing()
    {
        if (allUserFollowing == null)
        {
            allUserFollowing = getAllUserFollowingCollection();
        }

        return allUserFollowing;
    }

    public synchronized AbstractCollection<String> getAllUserFollowers()
    {
        if (allUserFollowers == null)
        {
            allUserFollowers = getAllUserFollowersCollection();
        }
        return allUserFollowers;
    }

    private synchronized LinkedHashSet<String> getAllUserFollowingCollection()
    {
        LinkedHashSet<String> followingList = new LinkedHashSet<>();
        final int recordsLoadedPerPage = 20;
        int totalCount = user.getFollowingCount();

        for (int i = 0; i < (totalCount / recordsLoadedPerPage) + 1; i++)
        {
            Map<String, Object> params = new HashMap<>();
            params.put("offset", (i * recordsLoadedPerPage));
            params.put("limit", recordsLoadedPerPage);
            List<Blog> usersFollowing = client.userFollowing(params);

            for (Blog userFollowing : usersFollowing)
            {
                followingList.add(userFollowing.getName());
            }
        }
        return followingList;
    }

    private synchronized LinkedHashSet<String> getAllUserFollowersCollection()
    {
        Blog blog = user.getBlogs().get(0); //TODO: Account for multiple blogs
        LinkedHashSet<String> followersList = new LinkedHashSet<>();
        final int recordsLoadedPerPage = 20;
        int totalCount = blog.getFollowersCount();

        for (int i = 0; i < (totalCount / recordsLoadedPerPage) + 1; i++)
        {
            Map<String, Object> params = new HashMap<>();
            params.put("offset", (i * recordsLoadedPerPage));
            params.put("limit", recordsLoadedPerPage);
            List<User> usersFollowers = blog.followers(params);

            for (User userFollower : usersFollowers)
            {
                followersList.add(userFollower.getName());
            }
        }
        return followersList;
    }

    public void followBlog(String blog)
    {
        client.follow(blog);
        LinkedHashSet<String> newAllUserFollowingSet = new LinkedHashSet<>(); //new list created so we can add new following blog to start of list
        newAllUserFollowingSet.add(blog); //Update our internal list
        newAllUserFollowingSet.addAll(allUserFollowing);
        allUserFollowing = newAllUserFollowingSet;
    }

    public void unfollowBlog(String blog)
    {
        client.unfollow(blog);
        allUserFollowing.remove(blog); //Update our internal list
    }

    public boolean blogExists(String blogName)
    {
        try
        {
            client.blogPosts(blogName);
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    public Blog getBlogInfo(String blogName)
    {
        return client.blogInfo(blogName);
    }

    public int getLikesTotalForBlog(String blogName)
    {
        if (isCurrentUsersBlog())
        {
            return user.getLikeCount();
        }
        else
        {
            return getBlogInfo(blogName).getLikeCount();
        }
    }

    public int getPostsTotalForBlog(String blogName)
    {
        return getBlogInfo(blogName).getPostCount();
    }
}
