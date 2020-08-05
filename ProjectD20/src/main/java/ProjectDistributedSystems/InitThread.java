package ProjectDistributedSystems;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;

public class InitThread extends Thread/****/ {
    ObjectInputStream in;
    ObjectOutputStream out;
    RealMatrix c,p;
    int id;
    Master m;

    public InitThread(Socket connection, RealMatrix c,RealMatrix p,int id,Master m) {
        try {
            /*
             *
             *
             *
             */
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
            this.c=c;
            this.p=p;
            this.id=id;
            this.m=m;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {

            out.writeInt(id);
            out.flush();
            int processors=in.readInt();
            double memory=in.readDouble();
            MatrixUtils.serializeRealMatrix(c, out);
            out.flush();
            MatrixUtils.serializeRealMatrix(p, out);
            out.flush();
            Worker w=(Worker)in.readObject();
            w.setWorkerId(id);
            w.setAvailableProcessors(processors);
            w.setAvailableMemory(memory);
            m.getWorkers().add(w);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
