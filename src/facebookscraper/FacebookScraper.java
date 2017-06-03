/* Library Requirements
 * -- MySQL JDBC Driver
 * -- facebook4j-core-2.5.2.jar
 */
package facebookscraper;

import facebook4j.Category;
import facebook4j.Comment;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.PagableList;
import facebook4j.Paging;
import facebook4j.Post;
import facebook4j.Reading;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Randy Jones
 */
public class FacebookScraper
{

    /**
     * @param args
     * @throws facebook4j.FacebookException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FacebookException, FileNotFoundException, IOException 
    {
        //get new facebook instance
        Facebook facebook = new FacebookFactory().getInstance();
        f4jProperties abc = new f4jProperties(facebook);
        
        //create array lists for posts and comments
        List<Post> fullPosts = new ArrayList<>();
        List<Comment> fullComments = new ArrayList<>();
        
        //parameters for feed
        String pageName = null; //set to null before "try"
        
        //gets all posts since date entered
        String sinceDate = JOptionPane.showInputDialog("Enter the Starting Date (YYYY-MM-DD):");
        if(sinceDate == null)
        {
            System.out.println("Canceled or error");
            System.exit(0);
        }
        else
        {
            System.out.println("Starting Date: "+sinceDate);
        }
        
        String untilDate = JOptionPane.showInputDialog("Enter the Ending Date (YYYY-MM-DD) [or leave blank for today]:");
        if(untilDate == null)
        {
            System.out.println("Until Current");
        }
        else
        {
            System.out.println("Ending Date: "+untilDate);
        }
        
        //creates sql connection object
        Connection conn = null;
        getConnect sqlCon = new getConnect();
        conn = sqlCon.start(conn);
        System.out.println("Getting page names from database...");
        //Queries DB to select pages to scrape
        Statement stmt = null;
        ArrayList<String> pageList = new ArrayList<>(); 
        try
        {
            String query = "SELECT pageReference FROM search_pages";
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) 
            {
                pageList.add(rs.getString("pageReference"));//adds each page to arraylist
            }
        }
        catch (SQLException e)
        {
             System.out.println(e.getMessage());
        }
        sqlCon.end(conn);
        
        //for each entry in arraylist, scrape all posts since date entered
        for(String name : pageList)
        {
            pageName=name;
            System.out.println("Parsing posts from: "+pageName);
            try 
            {
                //get posts from page until date specified
                //requires paging to see every post of a feed
                PagableList<Post> posts = facebook.getPosts(pageName, new Reading().fields("id,created_time,message,from").since(sinceDate).until(untilDate));
                Paging<Post> paging;
                do 
                {
                    for (Post post: posts)
                    {
                        fullPosts.add(post);//adds type 'Post' to fullPosts arraylist
                    }

                    paging = posts.getPaging();
                } while ((paging != null) && 
                        ((posts = facebook.fetchNext(paging)) != null));           
            } 
            catch (FacebookException ex) 
            {
                Logger.getLogger(Facebook.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        //get list of comments for each post
        System.out.println("Parsing all comments...");
        for (int i = 0; i < fullPosts.size(); i++) 
        {
            Post post = fullPosts.get(i);
            String postID = post.getId();
            try 
            { 
                // get comments from each post
                PagableList<Comment> comments = facebook.getPostComments(postID);
                Paging<Comment> paging;
                do {
                    for (Comment comment: comments)
                        fullComments.add(comment);//adds type 'Comment' to fullComments arraylist

                    paging = comments.getPaging();
                } while ((paging != null) && 
                        ((comments = facebook.fetchNext(paging)) != null));

            } 
            catch (FacebookException ex) 
            {
                Logger.getLogger(Facebook.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }

        conn = null;
        sqlCon = new getConnect();
        conn = sqlCon.start(conn);
        System.out.println("Writing posts to database...");
        //get post data for each post in list
        for (int i = 0; i < fullPosts.size(); i++) 
        {
            //extract data from each post
            Post postI = fullPosts.get(i);
            String postID = postI.getId();
            String pID = postID.split("_")[1];//post ID
            String date = postI.getCreatedTime().toString();
            String DateTime = getMonth(date);//DateTime
            Category usrCat = postI.getFrom();
            String userID = usrCat.getId();//User ID or page ID
            String userName = usrCat.getName();//User Name or Page Name
            String msg = postI.getMessage();//post message

            try
            {
                //mysql insert into posts statement
                String query = "INSERT INTO public_posts (pageID, pageName, postID, dateStamp, message)"
                + " values (?, ?, ?, ?, ?)";

                //create mysql prepared statement
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setString (1, userID);
                preparedStmt.setString (2, userName);
                preparedStmt.setString (3, pID);
                preparedStmt.setString (4, DateTime);
                preparedStmt.setString (5, msg);

                //execute the prepared statement
                preparedStmt.execute();
            }
            catch (SQLException s)
            {
                //nothing
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        System.out.println("Writing comments to database...");
        //get comment data for each comment in list
        for (int i = 0; i < fullComments.size(); i++) 
        {
            Comment cmt= fullComments.get(i);
            String commentID = cmt.getId();
            String pID = commentID.split("_")[0];//post ID
            String cmtID =  commentID.split("_")[1];//comment ID
            String cmtTime = cmt.getCreatedTime().toString();
            String DateTime = getMonth(cmtTime);//DateTime
            String userID = cmt.getFrom().getId();//User ID
            String userName = cmt.getFrom().getName();// User Name
            String cmtText = cmt.getMessage();//Comment message
            
            try
            {
                //mysql insert into comments statement
                String query = "INSERT INTO public_comments ( postID, cmtID,dateTime, userID, userName, cmtText)"
                + " values (?, ?, ?, ?, ?, ?)";

                //create mysql prepared statement
                PreparedStatement preparedStmt = conn.prepareStatement(query);
                preparedStmt.setString (1, pID);
                preparedStmt.setString (2, cmtID);
                preparedStmt.setString (3, DateTime);
                preparedStmt.setString (4, userID);
                preparedStmt.setString (5, userName);
                preparedStmt.setString (6, cmtText);

                //execute the prepared statement
                preparedStmt.execute();
            }
            catch (SQLException s)
            {
                //nothing
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        sqlCon.end(conn);
        facebook.shutdown();
        JOptionPane.showMessageDialog(null, "Scrape Complete");
    }
    
    //function to format datetime stamp
    //converts weird time and date format to standard 'yyyy-mm-dd hh:mm:ss'
    public static String getMonth(String date)
    {
        String time = date.substring(11, 19);
        String year = date.substring(date.length()-4);
        String day = date.substring(8,10);
        String month = date.substring(4, 7);
        String stamp = day+"-"+month+"-"+year;
        String dateTime=null;
        try 
        { 
            Calendar cal = Calendar.getInstance(); 
            cal.setTime(new SimpleDateFormat("dd-MMM-yyyy").parse(stamp)); 
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStamp = sdf.format(cal.getTime());
            dateTime = dateStamp+" "+time;
        } 
        catch (ParseException e) 
        { e.printStackTrace(); }
        return  dateTime;
    }
}