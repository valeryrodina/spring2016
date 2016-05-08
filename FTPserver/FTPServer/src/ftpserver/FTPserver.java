package ftpserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class FTPserver implements Runnable
{
    protected String addr = "192.168.88.11";
    protected int SERVER_PORT = 21;    
    
    protected ServerSocket servSock = null;

    public FTPserver(){

    }

    public FTPserver(String addr, int server_port){
        this.addr = addr;
        System.out.println(this.addr);        
        this.SERVER_PORT = server_port;
    }

    @Override
    public void run()
    {
        openServSocket();
        while(true)
        {
            Socket clientSock = null;
            try
            {
                clientSock = this.servSock.accept();
            } catch (IOException e)
            {
                System.out.println("Catched exception while waiting for client connections on server");
            }
            new Thread(new servThread(clientSock, this.addr, this.SERVER_PORT)).start();
        }
    }

    private void openServSocket()
    {
        System.out.println("Opening server socket :\nSERVER PORT = " + this.SERVER_PORT);        
        try
        {
            this.servSock = new ServerSocket(this.SERVER_PORT);
        } catch (IOException e)
        {
            throw new RuntimeException("Error while opening port " + this.SERVER_PORT, e);
        }
    }
}
