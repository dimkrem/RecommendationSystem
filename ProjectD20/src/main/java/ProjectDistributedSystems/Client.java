package ProjectDistributedSystems;


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Client extends Thread {
    int id,kpois;
    double latitude,longitude;
    double range;
    Client(int id,int latitude,int longitude,int kpois,double range) {

        this.id = id;
        this.latitude=latitude;
        this.longitude=longitude;
        this.kpois=kpois;
        this.range=range;
    }

    public void run() {
        Socket querySocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {

            /* Create socket for contacting the server on port 4321*/
            querySocket = new Socket("127.0.0.1", 4321); // (ip,port)
            System.out.println("Connected");
            out = new ObjectOutputStream(querySocket.getOutputStream());
            in = new ObjectInputStream(querySocket.getInputStream());
            out.writeInt(id);
            out.flush();
            out.writeDouble(latitude);
            out.flush();
            out.writeDouble(longitude);
            out.flush();
            out.writeInt(kpois);
            out.flush();
            out.writeDouble(range);
            out.flush();
            ArrayList<Poi> a=(ArrayList<Poi>)in.readObject();
            int c=1;
            for(Poi as:a){
                if(c<=kpois){
                    as.printPoiData();
                    c++;
                }
            }

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close(); out.close();
                querySocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        Scanner sc=new Scanner(System.in);
        System.out.println("Hello,please insert id");
        int id=sc.nextInt();
        Client c=new Client(id,643,453,5,5);
        c.start();

    }

}