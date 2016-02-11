/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facebookscraper;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author Admin
 */
public class getConnect
{
    public getConnect()
    {
        
    }
    
    public Connection start(Connection conn) 
    {
        try
        {
            //database address and credentials
            String url = "jdbc:mysql://aeroaddiction.ddns.net/facebook";
            String user = "capstone";
            String pass = "UTTyler16";
            Class.forName ("com.mysql.jdbc.Driver");
            
            //connect to database
            conn = DriverManager.getConnection (url,user,pass);
            System.out.println ("Database connection established"); 
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return conn;
    }
    
    public void end(Connection conn) 
    {
        try 
        {
            //close DB connection after all queries
            conn.close ();
            System.out.println ("Database connection terminated");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}