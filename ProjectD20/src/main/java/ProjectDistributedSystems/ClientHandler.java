package ProjectDistributedSystems;


import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler extends Thread/****/ {
    ObjectInputStream in;
    ObjectOutputStream out;
    Master m;

    public ClientHandler(Socket connection,Master m) {
        try {

            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
            this.m=m;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            int k=20;
            int id=in.readInt();
            double lat=in.readDouble();
            double lon=in.readDouble();
            int k1=in.readInt();
            ArrayList<Poi> p;
            p=m.calculateBestLocalPoisForUser(id,k);
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
