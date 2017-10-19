import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.sql.PreparedStatement;

public class Connector 
{
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement prepared_statement = null;
	
	private ResultSet result_set = null;
	
	/*
	 *  Connect to database
	 */
	public void read_database() throws Exception
	{
		try
		{
			// Load the MySQL driver
			Class.forName("com.mysql.jdbc.Driver");
		
			Connection connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/t_account?useSSL=false","lester","623062");
			
			//insert_entry(connect,prepared_statement,result_set,"Loan",20171018,"cash",10000,"loan_payable",10000);
			System.out.println(account_created(connect,prepared_statement,"cash"));
		}
		catch (Exception e)
		{
			throw e;
		}finally
		{
			close();
		}
	}
	
	/*
	 * Insert a journal entry
	 */
	private void insert_entry(Connection con, PreparedStatement s, ResultSet rs, String name, int date, String d1, int d1v, String c1, int c1v) throws Exception
	{
		try
		{
			if (d1v != c1v)
				System.out.println("Warning: Entry is not balanced");
			
			s = con.prepareStatement("INSERT INTO journal_entry (`Name`,`Date`,`Dr1_account`,`Dr1_value`,`Cr1_account`,`Cr1_value`) VALUES (?,?,?,?,?,?);");
			s.setString(1, "Collection");
			s.setInt(2, 20171011);
			s.setString(3, "cash");
			s.setInt(4, 100);
			s.setString(5, "account_receivable");
			s.setInt(6, 100);
		    s.executeUpdate();
		   
		    Scanner input = new Scanner(System.in);
		    String in_string;

		    // If there is any account that doesn't exist, create it		    
		    if (!account_created(con,s,d1))
		    {
		    	System.out.println(d1 + " does not exist. Do you want to create it? [Y/N]");
		    	in_string = input.nextLine();
		    	if (in_string.equals("Y"))
		    	{
		    		System.out.println("Type of the account: ");
		    		in_string = input.nextLine();
		    		if (in_string.length() > 1)
		    			create_account(con,s,d1,in_string);
		    	}
		    }
		    
		    if (!account_created(con,s,c1))
		    {
		    	System.out.println(c1 + " does not exist. Do you want to create it? [Y/N]");
		    	in_string = input.nextLine();
		    	if (in_string.equals("Y"))
		    	{
		    		System.out.println("Type of the account: ");
		    		in_string = input.nextLine();
		    		if (in_string.length() > 1)
		    			create_account(con,s,c1,in_string);
		    	}
		    }
		    
		    int last_id;
		    
		    rs = s.executeQuery("SELECT MAX(ID) FROM journal_entry;");
		    last_id = rs.getInt("ID");
		    add_record(con,s,d1,last_id,"D",d1v);
		    add_record(con,s,c1,last_id,"C",c1v);
		    
		    input.close();
		}catch (Exception e)	{throw e;}
	}
	
	/*
	 * Create a new account (table)
	 */
	private void create_account(Connection con, PreparedStatement s, String title, String type) throws SQLException
	{
		try
		{
			s = con.prepareStatement("CREATE TABLE `?` (`ID` INT AUTO_INCREMENT, `Trans_ID` INT NOT NULL, `Dr` INT, `Cr` INT, PRIMARY KEY (`ID`));");
			s.setString(1, title);
			s.executeUpdate();
		
			s = con.prepareStatement("INSERT INTO account_list (`Name`,`Type`) VALUES (?,?);");
			s.setString(1, title);
			s.setString(2, type);
			s.executeUpdate();
		}catch (SQLException e) {throw e;}
	}
	
	/*
	 * Check if the account exists
	 */
	private boolean account_created(Connection con, PreparedStatement s, String account) throws SQLException
	{
		try
		{
			s = con.prepareStatement("SELECT * FROM ?;");
			s.setString(1, account);
			s.executeQuery();
			return true;
		}catch (SQLException e) {return false;}
	}
	
	/*
	 * Add record to normal accounts, either debit or credit
	 */
	private boolean add_record(Connection con, PreparedStatement s, String account, int trans_id, String drcr, int value) throws SQLException
	{
		try
		{
			s = con.prepareStatement("INSERT INTO ? (`Name`,`Trans_ID`,`Dr`,`Cr`) VALUES (?,?,?,?)");
			s.setString(1, account);
			s.setInt(2, trans_id);
			if (drcr.equals("D"))
			{
				s.setInt(3, value);
				s.setInt(4, 0);
			}
			else if (drcr.equals("C"))
			{
				s.setInt(3, 0);
				s.setInt(4, value);
			}else
				return false;
			
			s.executeUpdate();
			return true;
		}catch (SQLException e) {throw e;}
	}
	
	/*
	 * Output result set
	 */
	private void write_result_set(ResultSet result_set) throws SQLException
	{
		while (result_set.next())
		{
			// Can use name or number to get columns
			int id = result_set.getInt("ID");
			String name = result_set.getString("Name");
			String type = result_set.getString("Type");
			System.out.println("\nID: " + id + "  Name: " + name + "  Type: "  + type);
		}
	}
	
	/*
	 * Close connection
	 */
	private void close()
	{
		try
		{
			if (result_set != null)
				result_set.close();
			
			if (statement != null)
				statement.close();
			
			if (connect != null)
				connect.close();
		}catch (Exception e) {}
	}
}
