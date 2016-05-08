package ftpserver;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

public class Main
{
    public static void main(String[] args)
    {
        FTPserver server;

        if (args.length == 2){
            try{
                server = new FTPserver( args[0], Integer.parseInt(args[1]) );
            } catch ( NumberFormatException e ){
                System.out.println("Bad arguments!!!");
                server = new FTPserver();
            }
        }
        else{
            server = new FTPserver();
        }

        Thread ftpThread = new Thread(server);
        ftpThread.start();
        
        boolean cont = true;
        while(cont)
        {
            try
            {
                Thread.sleep(1000);
                try
                {
                    if(System.in.read() == 'q') 
                    {
                        System.out.println("Quit command catched");

                        ftpThread.interrupt();
                        cont = false;
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            } catch (InterruptedException e)
            {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        System.out.println("Server finished");
        System.exit(0);
    }

}