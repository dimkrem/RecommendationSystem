package ProjectDistributedSystems;


import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;

public class RecYpThread extends Thread/****/ {
    ObjectInputStream in;
    ObjectOutputStream out;
    RealMatrix ypart;
    Master m;

    public RecYpThread(Socket connection,Master m) {
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
            int id=in.readInt();
            MatrixUtils.deserializeRealMatrix(this,"ypart",in);
            for(int i=0;i<m.getWorkers().size();i++){
                if(m.getWorkers().get(i).getWorkerId()==id) {
                    m.getWorkers().get(i).setYpart(ypart);
                }
            }


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
