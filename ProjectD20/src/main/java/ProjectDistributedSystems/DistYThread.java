package ProjectDistributedSystems;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;

public class DistYThread extends Thread/****/ {
    ObjectInputStream in;
    ObjectOutputStream out;
    RealMatrix ypart,x;
    Master m;

    public DistYThread(Socket connection, RealMatrix ypart,RealMatrix x,Master m) {
        try {
            /*
             *
             *
             *
             */
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
            this.ypart=ypart;
            this.x=x;
            this.m=m;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            MatrixUtils.serializeRealMatrix(ypart,out);
            out.flush();
            MatrixUtils.serializeRealMatrix(x,out);
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
