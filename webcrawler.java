package FCN;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class webcrawler{
	
	String neuID="";
	String password="";
	String host="cs5700f16.ccs.neu.edu";
	String cookie=null;
	Socket soc;
	Stack<String> linkList=new Stack<String>();
	Stack<String> linkLog=new Stack<String>();
	boolean regexFlag=false;
	/*
	 * The function is used to connect the server
	 * */
	public void connect()
	{
		try {
			soc=new Socket(InetAddress.getByName(host),80);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * The function is used to use the Get method to retrieve the information from the web page.
	 */
	private String getMethod(String location) throws IOException,NullPointerException
	{
		PrintWriter getmethod = null;
		try {
			getmethod = new PrintWriter(soc.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getmethod.print("GET "+location+" HTTP/1.1\n");
		if(cookie!=null)
			getmethod.print("Cookie: "+cookie+"\n");
		getmethod.print("Host: "+host+" \n\n");
		getmethod.flush();
		String response=null;
        response=writeDown();
		return response;	
	}
	
	/*
	 * Write strings into the Buffer.
	 */
	private String writeDown() throws IOException
	{
		String response=null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true)
		{
			response+=br.readLine()+"\n";
			if(!br.ready())
				break;
		}
		return response;
	}
	/*
	 * Use Post method to retrieve date from web page.
	 */
	private String postMethod(String location,String formData) throws IOException
	{
		String response=null;
		PrintWriter postmethod = null;
		try {
			postmethod = new PrintWriter(soc.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		postmethod.println("POST "+location+" HTTP/1.1");
		postmethod.println("Host: " +host);
		postmethod.println("Connection: keep-alive");
        postmethod.println("Cookie: " + cookie);
        postmethod.println("Content-Length: 109");
        postmethod.println("Content-Type: application/x-www-form-urlencoded; charset=utf-8");
        postmethod.println("Referer: http://cs5700f16.ccs.neu.edu/accounts/login/?next=/fakebook/\n");
        postmethod.println(formData);
		postmethod.flush();
        response=writeDown();
		return response;
	}
	/*
	 * login the server.
	 * Store the session information in the program.
	 * Deal with the 302 page error.
	 */
	public void login() throws IOException,NullPointerException
	{
		String response=getMethod("/accounts/login/");
		String csrftokenPattern="csrftoken=(\\w+);";
		String sessionPattern="sessionid=(\\w+);";
		String csrftoken=regularExpression(response,csrftokenPattern).group(1);
		String session=regularExpression(response,sessionPattern).group(1);
		cookie="csrftoken="+csrftoken+"; sessionid="+session;
		String formData="username="+neuID+"&password="+password+"&csrfmiddlewaretoken="+csrftoken+"&next=%2Ffakebook%2F";
		response=postMethod("/accounts/login/?next=/fakebook/",formData.trim());
		//Check whether login in successfully
		String messagePattern="HTTP/1.1 (\\d{3}+)";
		Matcher m = regularExpression(response,messagePattern);
		String message="";
		if(regexFlag)
		message = m.group(1);
		if(!message.equals("302"))
		{
          System.out.println("Login failed, please input again.");
		}
		session=regularExpression(response,sessionPattern).group(1);
		cookie="csrftoken="+csrftoken+"; sessionid="+session;
	}
	/*
	 * Implement regular expression function
	 */
	private Matcher regularExpression(String response, String pattern)
	{
		Pattern p = Pattern.compile(pattern);
        Matcher m= p.matcher(response);
        regexFlag=m.find();
        return m;  
	}
	/*
	 * Main crawler function.
	 * Search for the useful links.
	 * Search for the useful secret flags.
	 * Deal with some errors like 500, null, 301, 403 and 404.
	 */
	private void crawler() throws NullPointerException, IOException
	{
		String response=getMethod("/fakebook/");
		findLink(response);
		while(!linkList.isEmpty())
		{
			response=getMethod(linkList.peek());
			//To deal with bad response and reconnect to the server.
			String messagePattern="HTTP/1.1 (\\d{3}+)";
			Matcher m = regularExpression(response,messagePattern);
			String message="";
			if(regexFlag)
			message = m.group(1);
			if(message.equals("500")||message.equals(""))
			{
				connect();
				continue;
			}
			if(message.equals("301"))
			{
				continue;
			}
			if(message.equals("403")||message.equals("404"))
			{				
				continue;
			}
			linkList.pop();
			findLink(response);
			findSecretFlag(response);
		}
	}
	/*
	 * Write the pattern to compare with links.
	 */
	private void findLink(String response)
	{
		String linkPattern1="<a href=\"(/fakebook/[\\w]+/)\">";
		String linkPattern2="<a href=\"(/fakebook/[\\w]+/friends/\\w{1}/)\">";
		searchLink(linkPattern1,response);
		searchLink(linkPattern2,response);
	}
	/*
	 * Find out the link
	 * Store them into the stack
	 */
	private void searchLink(String linkPattern,String response)
	{
		Matcher m=regularExpression(response,linkPattern);
		//Avoid illegalStateException
		if(regexFlag)
		storeLink(m.group(1));
		while(m.find())
		{
			storeLink(m.group(1));
		}
	}
	/*
	 * Store links into stack and log stack
	 */
	 private void storeLink(String link)
	 {
		 if(!linkLog.contains(link))
		 {
			 linkList.push(link);
			 linkLog.push(link);	
		 }
	 }
	/*
	 * Find out the secret flag from the data
	 */
	private void findSecretFlag(String response)
	{
		String secretFlagPattern="<h2 class=\'secret_flag\' style=\"color:red\">FLAG: (\\w{64}+)</h2>";
		Matcher m=regularExpression(response,secretFlagPattern);
		//Avoid illegalstateexceptionException
		if(regexFlag)
		System.out.println(m.group(1));
		while(m.find())
		{
			System.out.println(m.group(1));
		}
	}
	/*
	 * Start the program.
	 * Validate inputs.
	 */
	public static void main(String[] args) throws IOException,NullPointerException
	{
		if (args.length!=2)
		{
			System.out.println("Please input the right inputs.");
		}
		webcrawler c=new webcrawler();
		c.neuID=args[0];
		c.password=args[1];
		c.connect();	
		c.login();
		c.crawler();
	}
}