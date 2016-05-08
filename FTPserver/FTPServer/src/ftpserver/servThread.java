package ftpserver;

import java.io.*;
import java.net.*;
import java.util.Stack;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.StringTokenizer;

public class servThread implements Runnable
{
    protected String addr;
    protected static int nextPort = 55000;

    private LinkedList<String> USERS = null;
    private LinkedList<String> PASSWORDS = null;

    protected Socket clientSock = null;

    private BufferedReader reader;
    private PrintWriter writer;

    private dataThread DATA_THREAD;

    private char type = 'I';

    private String username;
    private boolean isAuth = false;

    private final String baseDirectory = System.getProperty("user.dir") + "\\_files";
    private String currentDir = baseDirectory;

    public servThread(Socket clientSocket, String addr, int cPort)
    {
        try
        {
            this.clientSock = clientSocket;
            this.reader = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()), true);
            this.DATA_THREAD = new dataThread(this);

            this.addr = addr;

            BufferedReader BFreader = new BufferedReader(new FileReader("passwords.txt"));
            USERS = new LinkedList<String> ();
            PASSWORDS = new LinkedList<String> ();
            String line;
            StringTokenizer st;
            while((line = BFreader.readLine()) != null)
            {
                st = new StringTokenizer(line);
                if(st.countTokens() != 2)
                    continue;
                this.USERS.add(st.nextToken());
                this.PASSWORDS.add(st.nextToken());
                st = null;                
            }
        } catch (IOException e)
        {
            System.out.println("FTP server creation failed");
        }
    }

    @Override
    public void run()
    {
        System.out.println("Base directory is " + baseDirectory);
       
        reply(220, "Server ready");
        try
        {
            this.loop();
        } catch (Exception e)
        {
            System.out.println("Server control channel: commands loop failed");
            e.printStackTrace();
        } finally
        {
            try
            {
                this.clientSock.close();
                System.out.println("Client connection closed");
            } catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

 
    private void loop() throws IOException
    {
        String line, cmd;
        String fileFR = null;
        while((line = this.ReadAndPrintMsg("--> Client says : ")) != null)
        {
            StringTokenizer st = new StringTokenizer(line);
            cmd = st.nextToken();
            if (cmd.equalsIgnoreCase("user") && !this.isAuth)
            {
                int ans = this.setUser(st.nextToken());
                System.out.println("UserName: stringTokenizer = "+ans);
                if(ans==0)
                {
                    reply(331, "Username successfully changed-need password");
                } else if (ans == 1)
                {
                    reply(230, "Username successfully changed");
                }
            } else if (cmd.equalsIgnoreCase("pass") && !this.isAuth)
            {
                this.checkLogin();
                int result = this.checkPassword(st.nextToken().toString());
               
                if(result == 0)
                    reply(220, "Password successfully taken");
                else if (result == 1)
                    reply(531, "Incorrect password");
                else
                    reply(532, "No such user");
            } else if (cmd.equalsIgnoreCase("quit"))
            {
                reply(221, "Goodbye!!!");
                break;
            } else if (!this.isAuth)
            {
                reply(530, "Please login with USER and PASS.");
                continue;
            } else if (cmd.equalsIgnoreCase("rnfr"))
            {
                checkLogin();
                fileFR = st.nextToken();                
                reply(200, "RNFR accepted");                    
            } else if (cmd.equalsIgnoreCase("rnto"))
            {
                checkLogin();
                String fileTO = st.nextToken();
                File f = new File(this.currentDir + fileFR);
                if (!f.isDirectory()){
                    f.renameTo(new File(this.currentDir + fileTO));                     
                    reply(200, "RNTO accepted");
                } else {
                    reply(550, "RNTO it is directory!!!");
                }
                
            } else if (cmd.equalsIgnoreCase("type"))
            {
                checkLogin();
                String typeStr = st.nextToken();
                if(typeStr.length() != 1)
                {
                    reply(500, "Cant use this type");
                } else
                {
                    reply(200, "Type accepted");
                    this.type = typeStr.charAt(0);
                    System.out.println("Type accepted: " + this.type);
                }
            } else if (cmd.equalsIgnoreCase("syst"))
            {
                reply(210, "Windows 7");
            } else if (cmd.equalsIgnoreCase("noop"))
            {
                reply(200, "noop accepted");
            }else if (cmd.equalsIgnoreCase("pasv"))
            {
                checkLogin();
                String addrStr = this.addr;
                st = new StringTokenizer(addrStr, ".");
                String h1 = st.nextToken();
                String h2 = st.nextToken();
                String h3 = st.nextToken();
                String h4 = st.nextToken();
                int port = getNextPort();
                int p1 = port/256;
                int p2 = port%256;
                this.DATA_THREAD.setDataPort(p1*256+p2);
                reply(227, "Entering Passive Mode (" + 
                         h1 + "," + h2 + "," + h3 + "," + h4 + "," + p1 + "," + p2 + ")");
                System.out.println("Entering Passive Mode");
                this.DATA_THREAD.openPort();
            } else if (cmd.equalsIgnoreCase("cwd") || cmd.equalsIgnoreCase("xcwd"))
            {
                checkLogin();
                String newDir = this.getFullName(st);

                System.out.println("CWD cmd dir: " + newDir);
                if(newDir.length()==0)
                    newDir = this.baseDirectory;

                newDir = this.resolvePath(newDir);
                File f = new File(newDir);
                if(!f.exists()) {
                    reply(550, "No such directory: " + newDir);
                } else if(!f.isDirectory()) {
                    reply(550, "Not a directory: " + newDir);
                } else {
                    currentDir = newDir;
                    System.out.println("current dir: " + currentDir);
                    reply(250, "cwd command successfull");
                }
            } else if (cmd.equalsIgnoreCase("rmd") || cmd.equalsIgnoreCase("xrmd"))
            {
                checkLogin();
                String dirName = this.getFullName(st);
                String path = this.resolvePath(dirName);
                
                File dir = new File(path);
                if(!dir.exists())
                    reply(550, "Directory doesn't exist: " + dirName);
                else if (!dir.isDirectory())
                    reply(550, dirName + " is not directory");
                else if (!dir.delete())
                    reply(550, "Error deleting directory " + dirName);
                else{
                    reply(250, "Directory successfuly removed: " + path);
                    System.out.println("Directory deleted successfuly: " + path);
                }
            } else if (cmd.equalsIgnoreCase("dele"))
            {
                checkLogin();
                String fName = this.getFullName(st);
                String path = this.resolvePath(fName);

                File f = new File(path);

                if(!f.exists())
                    reply(550, "File doesn't exist: " + path);
                else if (!f.delete())
                    reply(550, "Error deleting file: " + path);
                else
                {
                    reply(250, "File deleted successfuly: " + path);
                    System.out.println("File deleted successfuly: " + path);
                }
            } else if (cmd.equalsIgnoreCase("mkd") || cmd.equalsIgnoreCase("xmkd"))
            {
                checkLogin();
                String dirName = this.getFullName(st);
                String path = this.resolvePath(dirName);

                File dir = new File(path);
                if(dir.exists())
                    reply(550, "Directory already exist: " + dirName);
                else if(!dir.mkdir())
                    reply(550, "Error creating directory "+ dirName);
                else{
                    this.reply(257, "Directory created: " + path );
                    System.out.println("Directory created: " + path);
                }
                
            } else if (cmd.equalsIgnoreCase("cdup"))
            {
                checkLogin();
                String newDir = this.resolvePath("..");
                System.out.println("resolvePath result: " + newDir);
                currentDir = newDir;
                reply(250, "cdup command successfull");
            } else if (cmd.equalsIgnoreCase("retr"))
            {
                checkLogin();
                String path = this.resolvePath(this.getFullName(st));
                this.DATA_THREAD.sendFile(path);
            } else if (cmd.equalsIgnoreCase("stor"))
            {
                checkLogin();

                String path = this.resolvePath(this.getFullName(st));
                this.DATA_THREAD.receiveFile(path);
            } else if (cmd.equalsIgnoreCase("port"))
            {
                checkLogin();
                String portStr = st.nextToken();
                st = new StringTokenizer(portStr, ",");
                String h1 = st.nextToken();
                String h2 = st.nextToken();
                String h3 = st.nextToken();
                String h4 = st.nextToken();
                int p1 = Integer.parseInt(st.nextToken());
                int p2 = Integer.parseInt(st.nextToken());

                String dataHost = h1 + "." + h2 + "." + h3 + "." + h4;
                int dataPort = (p1 << 8) | p2;

                this.DATA_THREAD.setDataPort(dataHost, dataPort);
                
                reply(200, "Port cmd succedeed");
                
            } else if (cmd.equalsIgnoreCase("list") || cmd.equalsIgnoreCase("nlst") )
            {
                checkLogin();
                String path = null;
                if(st.hasMoreTokens()){
                    path = st.nextToken();
                    if (path.charAt(0) == '-'){
                        path = currentDir;
                    }
                } else {
                    path = currentDir;
                }
                
                System.out.println("Sending list in : " + path);
                //reply(200, "Port cmd succedeed");
                this.DATA_THREAD.sendList(path);
            } else if (cmd.equalsIgnoreCase("pwd") || cmd.equalsIgnoreCase("xpwd"))
            {
                this.checkLogin();
                reply(257, "\"" + this.currentDir + "\"" + " is current directory");
                System.out.println("pwd cmd anser : " + "\"" + this.currentDir + "\"" + " is current directory");
            } else
            {
                System.out.println("cmd unknown: " + cmd);
                reply(500, "Command not supported: " + cmd);
            }
        }
    }
    String getFullName(StringTokenizer tok)
    {
        String elem=null, fullName=null;
        while(tok.hasMoreTokens())
        {
            elem = tok.nextToken().toString();
            if(fullName != null)
                fullName = fullName + " " + elem;
            else
                fullName = elem;
        }
        System.out.println("FullName function result: " + fullName);
        return fullName;
    }    

    private int setUser(String user)
    {

        this.username = user;
        if(user.equalsIgnoreCase("anonymous")){
            this.isAuth = true;
            return 1;
        }
        System.out.println("username successfully changed to " + user);
        return 0;
    }
    private int checkPassword(String pass)
    {
        Iterator<String> it_u = USERS.iterator();
        Iterator<String> it_p = PASSWORDS.iterator();
        while(it_u.hasNext())
        {
            String p = it_p.next();
            if(this.username.equalsIgnoreCase(it_u.next()))
            {
                //checkPassword
                if(pass.equals(p))
                {
                    System.out.println("password successfully changed");
                    this.isAuth = true;
                    return 0;
                }
                else
                {
                    System.out.println("password wrong");
                    return 1;
                }
            }
        }
        System.out.println("No such user: " + this.username);
        return 2;
    }
    private void checkLogin()
    {
        if(this.username == null)
        {
            reply(400, "Please login first");
        }
    }
    void reply(int code, String msg)
    {
        System.out.println("MSG to client <-- " + code + " " + msg);
        this.writer.println(code + " " + msg);
    }
    
    String ReadMsg()
    {
        String ans = null;
        try
        {
            ans = this.reader.readLine();
        } catch (IOException e)
        {
            System.out.println("Error while reading msg from client");
        }

        return ans;
    }
    String ReadAndPrintMsg(String prefix)
    {
        String ans = ReadMsg();
        System.out.println(prefix + " " + ans);
        return ans;
    }
    String resolvePath(String path)
    {
        if (path.length() == 1){
            if (path.charAt(0) == '\\' || path.charAt(0) == '/'){
                path = this.currentDir.charAt(0) + ":\\";
            } else
                path = this.currentDir + "\\" + path;        
        }else if (path.charAt(0) == '/'){
            path = this.baseDirectory;
            System.out.println("123: " + path );
        }else if (path.charAt(1) != ':')
            path = this.currentDir + "\\" + path;

        StringTokenizer pathSt = new StringTokenizer(path, "\\");
        Stack segments = new Stack();
        while(pathSt.hasMoreTokens())
        {
            String segment = pathSt.nextToken();
            if(segment.equalsIgnoreCase(".."))
            {
                if(segments.size()!=1)
                    segments.pop();
            } else if (segment.equalsIgnoreCase("."))
            {//Пропускаем
            } else
            {
                segments.push(segment);
            }
        }
        StringBuffer pathBuf = new StringBuffer();
        Enumeration segmentsEn = segments.elements();
        while (segmentsEn.hasMoreElements())
        {
            pathBuf.append(segmentsEn.nextElement());
            if (segmentsEn.hasMoreElements())
                pathBuf.append("\\");
        }

        return pathBuf.toString() + "\\";
    }

    public static int getNextPort(){
        if (nextPort != 65500){
            nextPort +=1;
        } else {
            nextPort = 55000;
        }
        return nextPort;
    }

}
