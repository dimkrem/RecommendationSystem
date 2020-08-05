package ProjectDistributedSystems;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;

public class DistXThread extends Thread/****/ {
    ObjectInputStream in;
    ObjectOutputStream out;
    RealMatrix xpart,y;
    Master m;

    public DistXThread(Socket connection, RealMatrix xpart,RealMatrix y,Master m) {
        try {
            /*
             *
             *
             *
             */
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
            this.xpart=xpart;
            this.y=y;
            this.m=m;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            MatrixUtils.serializeRealMatrix(xpart,out);
            out.flush();
            MatrixUtils.serializeRealMatrix(y,out);
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
