import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.sql.PreparedStatement;

//TODO: Accounts alignment

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
			System.out.println("LesterX's T Account Tool V0.5");
			// Load the MySQL driver
			Class.forName("com.mysql.jdbc.Driver");
			
			System.out.println("Connecting to MySQL ...");
			Connection connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/t_account?useSSL=false","lester","623062");
			System.out.println("Connected!");
			
			// To read from user input
			Scanner input = new Scanner(System.in);
			
			System.out.println("Please enter command: (Note: for now only 1 debit vs. 1 credit record is allowed");
			System.out.println("  1. Show all journal entries");
			System.out.println("  2. Show all accounts");
			System.out.println("  3. Find a transaction record");
			System.out.println("  4. Add a transaction record");
			System.out.println("  5. Delete a transaction record");
			System.out.println("  6. Delete all transaction records");
			System.out.println("  7. Quit");
			
			while (true)
			{
				if (!input.hasNext())
					break;
				
				String in = input.nextLine();
				int in_num = Integer.parseInt(in);
				
				if (in_num == 7)
					break;
				
				switch(in_num)
				{
					case 1:
					{
						show_entry(connect);
						break;
					}
					case 2:
					{
						System.out.println("Do you want the detailed information for every account? [Y/N]");
						String answer = input.nextLine();
						if (answer.equals("Y") || answer.equals("y"))
							show_all_accounts(connect);
						else
							list_accounts(connect);
						break;
					}
					case 3:
					{
						System.out.println("Please enter the transaction number: (You can find transactions numbers in journal entries and account details)");
						int num = Integer.parseInt(input.nextLine());
						int max = max_id(connect);
						if (max == 0)
							System.out.println("There is no transaction record.");
						else if (num > 0 && num < max)
							find_trans(connect,num);
						else
							System.out.println("ID not found");
						
						break;
					}
					case 4:
					{
						System.out.println("Transaction name: ");
						String name = input.nextLine();
						System.out.println("Date: (in digits like: 20170101)");
						int date = Integer.parseInt(input.nextLine());
						System.out.println("Please enter in this form: 'Cash 1000'");
						System.out.println("Debit: ");
						String[] rec = input.nextLine().split(" ");
						String dr = rec[0];
						int dv = Integer.parseInt(rec[1]);
						System.out.println("Credit: ");
						rec = input.nextLine().split(" ");
						String cr = rec[0];
						int cv = Integer.parseInt(rec[1]);
						
						insert_entry(connect,name,date,dr,dv,cr,cv);
						break;
					}
					case 5:
					{
						System.out.println("Please enter the transaction number to DELETE: (You can find transactions numbers in journal entries and account details)");
						int num = Integer.parseInt(input.nextLine());
						int max = max_id(connect);
						if (max == 0)
							System.out.println("There is no transaction record.");
						else if (num > 0 && num < max)
							delete_trans(connect, num);
						else 
							System.out.println("ID not found");
						
						break;
					}
					case 6:
					{
						System.out.println("ARE YOU SURE YOU WANT TO DELETE ALL TRANSACTION RECORDS ??? [Y/N]");
						String enter = input.nextLine();
						if (enter.equals("Y") || enter.equals("y"))
						{
							System.out.println("YOU REALLY REALLY WANT TO DO THIS ???");
							enter = input.nextLine();
							if (enter.equals("Y") || enter.equals("y"))
							{
								System.out.println("YOU REALLY REALLY REALLY WANT TO DO THIS ???");
								enter = input.nextLine();
								if (enter.equals("Y") || enter.equals("y"))
									delete_all(connect);
							}
						}
						
						break;
					}
					default:
					{
						System.out.println("Invalid input");
						break;
					}
				}
				
				System.out.println("Please enter command: (Note: for now only 1 debit vs. 1 credit record is allowed");
				System.out.println("  1. Show all journal entries");
				System.out.println("  2. Show all accounts");
				System.out.println("  3. Find a transaction record");
				System.out.println("  4. Add a transaction record");
				System.out.println("  5. Delete a transaction record");
				System.out.println("  6. Delete all transaction records");
				System.out.println("  7. Quit");
			}
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
	 * SHow records of all accounts
	 */
	private void show_all_accounts(Connection con) throws SQLException
	{
		try
		{
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT Name FROM account_list;");
			while (rs.next())
			{
				String account = rs.getString("Name");
				show_account(con,account);
			}
			
			if (result_set != null)
				result_set.close();
			
			if (statement != null)
				statement.close();
		}catch (SQLException e) {throw e;}
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
	 * Find transaction information with transaction id
	 */
	private void find_trans(Connection con, int id) throws SQLException
	{
		try
		{
			String dr1 = "", cr1 = "";
			Statement s = con.createStatement();
			String sql = "SELECT * FROM journal_entry WHERE ID = " + id + ";";
			ResultSet rs = s.executeQuery(sql);
			if (rs.next())
			{
				String name = rs.getString("Name");
				int date = rs.getInt("Date");
				dr1 = rs.getString("Dr1_account");
				int dr1v = rs.getInt("Dr1_value");
				cr1 = rs.getString("Cr1_account");
				int cr1v = rs.getInt("Cr1_value");
				System.out.println("ID: " + id + "    Date: " + date + "    Name: " + name);
				System.out.printf("  Dr. %15s %10d\n",dr1,dr1v);
				System.out.printf("    Cr. %15s %10d\n",cr1,cr1v);
				System.out.println();
			}else
			{
				System.out.println("Transaction not found.");
				return;
			}
			
			if (result_set != null)
				result_set.close();
			
			if (statement != null)
				statement.close();
			
		}catch (SQLException e) {throw e;}
		
	}
	
	/*
	 * Delete transaction with transaction id
	 */
	private void delete_trans(Connection con, int id) throws SQLException
	{
		try
		{
			String dr1 = "", cr1 = "";
			PreparedStatement s = con.prepareStatement("SELECT * FROM journal_entry WHERE ID = ?;");
			s.setInt(1, id);
			ResultSet rs = s.executeQuery();
			if (rs.next())
			{
				dr1 = rs.getString("Dr1_account");
				cr1 = rs.getString("Cr1_account");
			}else
			{
				System.out.println("Transaction not found");
				return;
			}
			
			String sql = "DELETE FROM " + dr1 + " WHERE Trans_ID = " + id + ";"; 
			s.executeUpdate(sql);
			sql = "DELETE FROM " + cr1 + " WHERE Trans_ID = " + id + ";"; 
			s.executeUpdate(sql);
			System.out.println("Deleted");
			s = con.prepareStatement("DELETE FROM journal_entry WHERE ID = ?;");
			s.setInt(1, id);
			s.executeUpdate();
			
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
	 * Return the latest transaction number
	 */
	private int max_id(Connection con) throws SQLException 
	{
		try
		{
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT MAX(ID) FROM journal_entry;");
			int result = 0;
			if (rs.next())
				result = rs.getInt("MAX(ID)");
			else
				return 0;
			
			if (result_set != null)
				result_set.close();
			
			if (statement != null)
				statement.close();
			
			return result;
		}catch (SQLException e) {throw e;}
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
