import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.sql.PreparedStatement;

//TODO: User input

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
			
			//insert_entry(connect,"Payment",20171019,"loan_payable",10000,"cash",10000);
			list_accounts(connect);
			show_entry(connect);
			show_account(connect, "cash");
			//delete_all(connect);
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
	private void insert_entry(Connection con, String name, int date, String d1, int d1v, String c1, int c1v) throws Exception
	{
		try
		{
			if (d1v != c1v)
				System.out.println("Warning: Entry is not balanced");
	
			PreparedStatement s = con.prepareStatement("INSERT INTO journal_entry (`Name`,`Date`,`Dr1_account`,`Dr1_value`,`Cr1_account`,`Cr1_value`) VALUES (?,?,?,?,?,?);");
			s.setString(1, name);
			s.setInt(2, date);
			s.setString(3, d1);
			s.setInt(4, d1v);
			s.setString(5, c1);
			s.setInt(6, c1v);
		    s.executeUpdate();
		    
		    Scanner input = new Scanner(System.in);
		    String in_string;

		    ResultSet rs = null;
		    // If there is any account that doesn't exist, create it		    
		    if (!account_created(con,d1))
		    {
		    	System.out.println(d1 + " does not exist. Do you want to create it? [Y/N]");
		    	in_string = input.nextLine();
		    	if (in_string.equals("Y"))
		    	{
		    		System.out.println("Type of the account: ");
		    		in_string = input.nextLine();
		    		if (in_string.length() > 1)
		    			create_account(con,d1,in_string);
		    	}
		    }
		    
		    if (!account_created(con,c1))
		    {
		    	System.out.println(c1 + " does not exist. Do you want to create it? [Y/N]");
		    	in_string = input.nextLine();
		    	if (in_string.equals("Y"))
		    	{
		    		System.out.println("Type of the account: ");
		    		in_string = input.nextLine();
		    		if (in_string.length() > 1)
		    			create_account(con,c1,in_string);
		    	}
		    }
		    
		    int last_id;
		    
		    s = con.prepareStatement("");
		    rs = s.executeQuery("SELECT MAX(ID) FROM journal_entry;");
		    rs.next();
		    last_id = rs.getInt("MAX(ID)");
		    add_record(con,d1,last_id,"D",d1v);
		    add_record(con,c1,last_id,"C",c1v);
		    
		    input.close();
		    System.out.println("Successfully inserted");
		    
		    if (rs != null)
		    	rs.close();
		    if (s != null)
		    	s.close();
		    	
		}catch (Exception e)	{throw e;}
	}
	
	/*
	 * Create a new account (table)
	 */
	private void create_account(Connection con, String title, String type) throws SQLException
	{
		try
		{
			String sql = "CREATE TABLE " + title + "(`ID` INT AUTO_INCREMENT, `Trans_ID` INT NOT NULL, `Dr` INT, `Cr` INT, PRIMARY KEY (`ID`));";
			PreparedStatement s = con.prepareStatement(sql);
			s.executeUpdate();
		
			s = con.prepareStatement("INSERT INTO account_list (`Name`,`Type`) VALUES (?,?);");
			s.setString(1, title);
			s.setString(2, type);
			s.executeUpdate();
			
		    if (s != null)
		    	s.close();
		}catch (SQLException e) {throw e;}
	}
	
	/*
	 * Check if the account exists
	 */
	private boolean account_created(Connection con, String account) throws SQLException
	{
		try
		{
			PreparedStatement s = con.prepareStatement("SELECT * FROM account_list WHERE Name = ?;");
			s.setString(1, account);
			ResultSet rs = s.executeQuery();
			boolean result = rs.next();
			if (rs != null)
		    	rs.close();
		    if (s != null)
		    	s.close();
			return result;
			
		}catch (SQLException e) {return false;}
	}
	
	/*
	 * Add record to normal accounts, either debit or credit
	 */
	private boolean add_record(Connection con, String account, int trans_id, String drcr, int value) throws SQLException
	{
		try
		{
			String sql = "INSERT INTO " + account + " (`Trans_ID`,`Dr`,`Cr`) VALUES (?,?,?)";
			PreparedStatement s = con.prepareStatement(sql);
			
			s.setInt(1, trans_id);
			if (drcr.equals("D"))
			{
				s.setInt(2, value);
				s.setInt(3, 0);
			}
			else if (drcr.equals("C"))
			{
				s.setInt(2, 0);
				s.setInt(3, value);
			}else
				return false;
			
			s.executeUpdate();
			
		    if (s != null)
		    	s.close();
			return true;
		}catch (SQLException e) {throw e;}
	}
	
	/*
	 * Show all journal entries
	 */
	private void show_entry(Connection con) throws SQLException
	{
		try
		{
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM journal_entry;");
			System.out.println();
			System.out.println("Journal Entries");
			System.out.println();
			while (rs.next())
			{
				int id = rs.getInt("ID");
				String name = rs.getString("Name");
				int date = rs.getInt("Date");
				String dr1 = rs.getString("Dr1_account");
				int dr1v = rs.getInt("Dr1_value");
				String cr1 = rs.getString("Cr1_account");
				int cr1v = rs.getInt("Cr1_value");
				System.out.println("ID: " + id + "    Date: " + date + "    Name: " + name);
				System.out.printf("  Dr. %15s %10d\n",dr1,dr1v);
				System.out.printf("    Cr. %15s %10d\n",cr1,cr1v);
				System.out.println();
			}
			
			if (rs != null)
				rs.close();
			if (s != null)
				s.close();
		}catch (SQLException e){throw e;}
		
	}
	
	private void list_accounts(Connection con) throws SQLException
	{
		try
		{
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM account_list;");
			System.out.println();
			System.out.println("Account List");
			while (rs.next())
			{
				int id = rs.getInt("ID");
				String name = rs.getString("Name");
				String type = rs.getString("Type");
				System.out.printf("ID: %-4d  Name: %-20s    Type: %-10s", id, name, type);
				System.out.println();
			}
			
			if (rs != null)
				rs.close();
			if (s != null)
				s.close();
		}catch (SQLException e){throw e;}
		
	}
	
	/*
	 * Repeate string for n times
	 */
	private String repeat(String s, int t)
	{
		String result = "";
		for (int i = 0; i < t; i ++)
			result = result + s;
		return result;
	}
	
	/*
	 * Show record of selected account
	 */
	private void show_account(Connection con, String account) throws SQLException
	{
		try
		{
			int len = account.length();
			Statement s = con.createStatement();
			String sql = "SELECT * FROM " + account + ";";
			ResultSet rs = s.executeQuery(sql);
			
			System.out.println(repeat(" ",len * 3) + account);
			System.out.println(repeat("-",len * 8));
			
			while (rs.next())
			{
				int trans_id = rs.getInt("Trans_ID");
				int dr = rs.getInt("Dr");
				int cr = rs.getInt("Cr");
				String format_dr = "%" + 3 * len + "d | ID: %" + (3 * len - 4) + "d\n";
				String format_cr = "ID: %" + (3 * len - 4) + "d | %" + 3 * len + "d\n";
				if (cr == 0)
					System.out.printf(format_dr, dr, trans_id);
				else
					System.out.printf(format_cr, trans_id, cr);
			}
			System.out.println();
			
			if (rs != null)
				rs.close();
			if (s != null)
				s.close();
		}catch (SQLException e) {throw e;}
	}
	
	/*
	 * Delete all transaction record (Careful)
	 */
	private void delete_all(Connection con) throws SQLException
	{
		try
		{
		    Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM account_list;");
			while (rs.next())
			{
				Statement s1 = con.createStatement();
				String account = rs.getString("Name");
				String sql = "DROP TABLE " + account + ";";
				s1.executeUpdate(sql);
				s1.close();
			}
			
			s.executeUpdate("DELETE FROM journal_entry;");
			s.executeUpdate("ALTER TABLE journal_entry AUTO_INCREMENT = 1;");
			s.executeUpdate("DELETE FROM account_list;");
			s.executeUpdate("ALTER TABLE account_list AUTO_INCREMENT = 1;");
			
			if (rs != null)
				rs.close();
			
			if (s != null)
				s.close();
			
			System.out.println("All Clear!");
		}catch (SQLException e){throw e;}
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
