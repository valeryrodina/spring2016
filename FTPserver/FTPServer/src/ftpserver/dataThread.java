package ftpserver;

import java.io.*;
import java.net.*;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;


public class dataThread implements Runnable
{
    private servThread SERVER_THREAD;
    private int DATA_PORT = 20;
    private String DATA_HOST;
    private boolean isPasv = true;
    Socket dataSocket = null;
    ServerSocket servSocket = null;
   
    dataThread(servThread SRVTHREAD)
    {
        this.SERVER_THREAD = SRVTHREAD;
    }

    public void setDataPort( int port ){
        this.isPasv = true;
        this.DATA_PORT = port;
    }
    public void openPort() throws IOException{
         if (isPasv){
                servSocket = new ServerSocket(this.DATA_PORT);
                dataSocket = servSocket.accept();
                
                //if (dataSocket = null)
                 //System.out.println("error");  
                
            }else{
                dataSocket = new Socket(this.DATA_HOST, this.DATA_PORT);                
            }
    }
    
    @Override
    public void run()
    {

    }
    
    public void setDataPort(String host, int port)
    {
        this.DATA_PORT = port;
        this.DATA_HOST = host;
        this.isPasv = false;
        System.out.println("Connection settings for data transmittion:");
        System.out.println("\tHost = " + host + "\n\tPort = " + port);
    }

    public int sendList(String path)
    {
        //dataSocket = null;
        //servSocket = null;
        try
        {
            File dir = new File(path);
            String filenames[] = dir.list();
            int filesCount = (filenames == null ? 0 : filenames.length);

           
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(dataSocket.getOutputStream()), true);

            this.SERVER_THREAD.reply(150, "Creating data connection successfull - starting list transmittion");
            
            for(int i = 0; i < filesCount; i++)
            {
                String fName = filenames[i];
                File file = new File(dir, fName);
                this.listFile(file, writer);
                System.out.println("LIST processing: " + dir + "\\" + fName);
            }
            writer.flush();
            System.out.println("LIST [" + filesCount + " file(s)] transfered");
            this.SERVER_THREAD.reply(226, "Transfer complete");
        } catch (ConnectException e)
        {
            this.SERVER_THREAD.reply(425, "Can't open data connection. Pleasy try again");
            return 1;
        }catch (Exception e)
        {
            e.printStackTrace();
            this.SERVER_THREAD.reply(550, "No such directory");
            return 1;
        } finally
        {
            try
            {
                if(dataSocket != null)
                    dataSocket.close();
                if(servSocket != null)
                    servSocket.close();
            } catch (IOException e)
            {}
        }
        return 0;
    }
    public void receiveFile(String path)
    {
        FileOutputStream fos = null;
        //Socket dataSocket = null;
        //ServerSocket servSocket = null;
        try
        {
            
            /*if (isPasv){
                servSocket = new ServerSocket(this.DATA_PORT);
                dataSocket = servSocket.accept();
            }else{
                dataSocket = new Socket(this.DATA_HOST, this.DATA_PORT);
            }*/
            InputStream in = dataSocket.getInputStream();

            File f = new File(path);
            if(f.exists())
            {
                this.SERVER_THREAD.reply(550, "File already exist: " + path);
                return;
            }
            fos = new FileOutputStream(f);
            if(this.DATA_PORT == -1)
            {
                this.SERVER_THREAD.reply(500, "Send a PORT cmd first");
                return;
            }
            this.SERVER_THREAD.reply(150, "Starting to receive file " + path);
            //Здесь непосредственно прием файла
            byte buf[] = new byte[1024];
            int nread;
            while((nread = in.read(buf, 0, 1024)) > 0)
            {
                fos.write(buf, 0, nread);
            }
            in.close();

            this.SERVER_THREAD.reply(226, "Transfer completed successfuly");
        } catch(ConnectException ce)
        {
            this.SERVER_THREAD.reply(420, "Connection error");
            return;
        } catch (FileNotFoundException e)
        {
            this.SERVER_THREAD.reply(500, "File not exist");
            return;
        } catch (UnknownHostException e)
        {
            System.out.println("Host unknown");
            this.SERVER_THREAD.reply(500, "Host unknown");
            return;
        } catch (Exception e)
        {
            System.out.println("Unknown exception");
            this.SERVER_THREAD.reply(500, "exception unknown");
            return;
        } finally
        {
            try
            {
                if(fos != null)
                    fos.close();
                if(dataSocket != null)
                    dataSocket.close();
                if(servSocket != null)
                    servSocket.close();

            } catch(IOException e)
            {}
        }
    }
    public void sendFile(String path)
    {
        FileInputStream fis = null;
        //Socket dataSocket = null;
        //ServerSocket servSocket = null;
        try
        {            
            /*if (isPasv){
                servSocket = new ServerSocket(this.DATA_PORT);
                dataSocket = servSocket.accept();
            }else{
                dataSocket = new Socket(this.DATA_HOST, this.DATA_PORT);
            }*/
            OutputStream out = dataSocket.getOutputStream();
            
            File f = new File(path);
            if(!f.isFile())
            {
                this.SERVER_THREAD.reply(550, "Not a file");
                return;
            }
            fis = new FileInputStream(f);
            if(this.DATA_PORT == -1)
            {
                this.SERVER_THREAD.reply(500, "Send a PORT cmd first");
                return;
            }
            this.SERVER_THREAD.reply(150, "Starting to transfer file " + path);
            //Здесь непосредственно передача файла
            byte buf[] = new byte[1024];
            int nread;
            while((nread = fis.read(buf)) > 0)
            {
                out.write(buf, 0, nread);
            }
            fis.close();
            
            this.SERVER_THREAD.reply(226, "Transfer completed");
        } catch (FileNotFoundException e)
        {
            this.SERVER_THREAD.reply(500, "File not exist");
            return;
        } catch (UnknownHostException e)
        {
            System.out.println("Host unknown");
            this.SERVER_THREAD.reply(500, "Host unknown");
            return;
        } catch (Exception e)
        {
            System.out.println("Unknown exception");
            this.SERVER_THREAD.reply(500, "exception unknown");
            return;
        } finally
        {
            try
            {
                if(fis != null)
                    fis.close();
                if(dataSocket != null)
                    dataSocket.close();
                if(servSocket != null)
                    servSocket.close();
            } catch(IOException e)
            {
                //ignore
            }
        }
    }
    private void listFile(File f, PrintWriter writer)
    {
        Date date = new Date(f.lastModified());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm", Locale.ENGLISH);
        String dateStr = dateFormat.format(date);

        long size = f.length();
        String sizeStr = Long.toString(size);
        int sizePadLength = Math.max(13 - sizeStr.length(), 0);
        String sizeField = pad(sizePadLength) + sizeStr;
       
        if (f.isDirectory())
            writer.print("d");
        else
            writer.print("-");
        writer.print("rwxrwxrwx   1 ftp      ftp ");
        writer.print(sizeField);
        writer.print(" ");
        writer.print(dateStr);
        writer.print(" ");        
        writer.println(f.getName());
    }
    private static String pad(int length)
    {
        StringBuffer buf = new StringBuffer();
        for (int i =0; i < length; i++)
        {
            buf.append((char)' ');
        }
        return buf.toString();
    }
}
