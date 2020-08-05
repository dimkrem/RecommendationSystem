package ProjectDistributedSystems;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Worker  implements Serializable  {
    private RealMatrix cu,ci,x,y,c,p,xpart,ypart,pu,pi;
    private int availableProcessors;
    private double availableMemory;
    private int workerId,startx,starty,finishx,finishy;
    private double value,l;



    private boolean xstage,ystage;
    //setters

    public void setP(RealMatrix p) {
        this.p = p;
    }

    public void setC(RealMatrix c) {
        this.c = c;
    }



    public void setY(RealMatrix y) {
        this.y = y;
    }

    public void setAvailableMemory() {
        this.availableMemory = Runtime.getRuntime().freeMemory()/10000000;

    }
    public void setAvailableMemory(double availableMemory) {
        this.availableMemory = availableMemory;

    }
    public double getValue(){
        this.value=getAvailableProcessors()*getAvailableMemory();
        return this.value;
    }
    public void setAvailableProcessors() {
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setYstage(boolean ystage) {
        this.ystage = ystage;
    }
    //getters

    public double getAvailableMemory() {
        return availableMemory;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public int getWorkerId() {
        return workerId;
    }
    //calculations
    //XX^T size m*m
    public RealMatrix precalculateXX(RealMatrix x) {
        return x.transpose().multiply(x);
    }
    //YY^T size n*n
    public RealMatrix precalculateYY(RealMatrix y) {
        return y.transpose().multiply(y);
    }
    //Ci size m*m
    public void calculateCiMatrix(int poi, RealMatrix c) {

        double[]diagonal=new double[c.getRowDimension()];
        for(int i=0 ; i<diagonal.length;i++){
            diagonal[i]=c.getEntry(i,poi);
        }
        RealMatrix ci=MatrixUtils.createRealDiagonalMatrix(diagonal);
        this.ci=ci;
    }
    // Cu size n*n
    public void calculateCuMatrix(int user, RealMatrix c) {
        double[]diagonal=new double[c.getColumnDimension()];
        for(int j=0 ; j<diagonal.length;j++){
            diagonal[j]=c.getEntry(user,j);
        }
        RealMatrix cu=MatrixUtils.createRealDiagonalMatrix(diagonal);
        this.cu=cu;
    }
    public  void calculatePu(int user){
        double [] data=new double[c.getColumnDimension()];
        for(int i=0;i<data.length;i++) data[i]=p.getEntry(user,i);
       RealMatrix pu= MatrixUtils.createColumnRealMatrix(data);
        this.pu=pu;
    }
    public void calculatePi(int poi){
        double [] data=new double[c.getRowDimension()];
        for(int i=0;i<data.length;i++) data[i]=p.getEntry(i,poi);
        RealMatrix pi=MatrixUtils.createColumnRealMatrix(data);
        this.pi=pi;
    }
    //x_u size k*1
    public RealMatrix calculate_x_u(double l,int user) {
        calculatePu(user);//
        calculateCuMatrix(user,c);//
        RealMatrix first=precalculateYY(y);//
        RealMatrix second=y.transpose();//Y^T
        RealMatrix third=cu.subtract(MatrixUtils.createRealIdentityMatrix(cu.getColumnDimension()));// Cu-I//
        RealMatrix fourth=second.multiply(third);//Y^T(Cu-I)
        RealMatrix fifth=fourth.multiply(y);////Y^T(Cu-I)Y
        RealMatrix sixth=MatrixUtils.createRealIdentityMatrix(y.getColumnDimension()).scalarMultiply(l);//lI//
        RealMatrix seventh=first.add(fifth);
        RealMatrix eighth=seventh.add(sixth);//20*20
        RealMatrix ninth=new LUDecomposition(eighth).getSolver().getInverse();//(Y^TY +Y^T(Cu-I)Y+lI)^-1
        RealMatrix tenth=ninth.multiply(y.transpose());
        RealMatrix eleventh=tenth.multiply(cu);
        RealMatrix last=eleventh.multiply(pu);
        return last;

    }
    //y_i size 1*k
    public RealMatrix calculate_y_i(double l,int poi) {
        calculatePi(poi);
        calculateCiMatrix(poi,c);
        RealMatrix first=precalculateXX(x);
        RealMatrix second=x.transpose();
        RealMatrix third=ci.subtract(MatrixUtils.createRealIdentityMatrix(ci.getColumnDimension()));// Cu-I
        RealMatrix fourth=second.multiply(third);
        RealMatrix fifth=fourth.multiply(x);
        RealMatrix sixth=MatrixUtils.createRealIdentityMatrix(x.getColumnDimension()).scalarMultiply(l);//lI
        RealMatrix seventh=first.add(fifth);
        RealMatrix eighth=seventh.add(sixth);
        RealMatrix ninth=new LUDecomposition(eighth).getSolver().getInverse();
        RealMatrix tenth=ninth.multiply(x.transpose());
        RealMatrix eleventh=tenth.multiply(ci);
        RealMatrix last=eleventh.multiply(pi);
        return last;
    }
    //connections
    public void initialize() throws IOException {
        Socket  requestSocket = new Socket("127.0.0.1", 4321); // (ip,port)
        ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
        try {

            int id=in.readInt();
            System.out.printf("Worker id:%d\n",id);
            out.writeInt(getAvailableProcessors());
            out.flush();
            out.writeDouble(getAvailableMemory());
            out.flush();
            MatrixUtils.deserializeRealMatrix(this,"c",in);
            MatrixUtils.deserializeRealMatrix(this,"p",in);
            out.writeObject(this);
            out.flush();
            this.setWorkerId(id);
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if(requestSocket!=null) requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    public void acceptXmatrixFromMasterAndY() throws IOException {
        Socket requestSocket = new Socket("127.0.0.1", 4321); // (ip,port)
        ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
        try {
            MatrixUtils.deserializeRealMatrix(this,"xpart",in);
            MatrixUtils.deserializeRealMatrix(this,"y",in);
            System.out.println("Xpart and Y received");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close(); out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }
    public void acceptYmatrixFromMasterAndX() throws IOException {
        Socket requestSocket = new Socket("127.0.0.1", 4321); // (ip,port)
        ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
        try {
            MatrixUtils.deserializeRealMatrix(this,"ypart",in);
            MatrixUtils.deserializeRealMatrix(this,"x",in);
            System.out.println("Ypart and X received");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close(); out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }
    public void SendXpart(){
        try{
            Socket requestSocket = new Socket("127.0.0.1", 4321); // (ip,port)
            ObjectOutputStream  out = new ObjectOutputStream(requestSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                RealMatrix xproc=MatrixUtils.createRealMatrix(xpart.getRowDimension(),xpart.getColumnDimension());
                for(int user=0;user<getXpart().getRowDimension();user++){
                  RealMatrix xu=calculate_x_u(l,user).transpose();
                   xproc.setRowMatrix(user,xu);
                }
                xpart=xproc;
                out.writeInt(getWorkerId());
                out.flush();
                MatrixUtils.serializeRealMatrix(xpart,out);
                out.flush();
                System.out.println("X sent");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    public void SendYpart(){
        try{
            Socket requestSocket = new Socket("127.0.0.1", 4321); // (ip,port)
            ObjectOutputStream  out = new ObjectOutputStream(requestSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
            RealMatrix yproc=MatrixUtils.createRealMatrix(ypart.getRowDimension(),ypart.getColumnDimension());
            for(int poi=0;poi<getYpart().getRowDimension();poi++){
                RealMatrix yi=calculate_y_i(l,poi).transpose();
                yproc.setRowMatrix(poi,yi);
            }
            ypart=yproc;
            out.writeInt(getWorkerId());
            out.flush();
            MatrixUtils.serializeRealMatrix(ypart,out);
            out.flush();
            System.out.println("Y sent");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


    public static void main(String args[]) throws InterruptedException, IOException {
        Worker w=new Worker();
        w.setAvailableMemory();
        w.setAvailableProcessors();
        w.initialize();
        Thread.sleep(20000);
        for(int i=0;i<5;i++) {

            w.acceptXmatrixFromMasterAndY();
            Thread.sleep(20000);
             w.SendXpart();
           Thread.sleep(20000);

            w.acceptYmatrixFromMasterAndX();
            Thread.sleep(20000);
            w.SendYpart();
            Thread.sleep(20000);
        }

    }

    public int getStartx() {
        return startx;
    }

    public int getStarty() {
        return starty;
    }

    public int getFinishx() {
        return finishx;
    }

    public int getFinishy() {
        return finishy;
    }

    public void setFinishx(int finishx) {
        this.finishx = finishx;
    }

    public void setFinishy(int finishy) {
        this.finishy = finishy;
    }

    public void setStartx(int startx) {
        this.startx = startx;
    }

    public void setStarty(int starty) {
        this.starty = starty;
    }

    public void setXpart(RealMatrix xpart) {
        this.xpart = xpart;
    }

    public RealMatrix getXpart() {
        return xpart;
    }

    public RealMatrix getYpart() {
        return ypart;
    }

    public void setYpart(RealMatrix ypart) {
        this.ypart = ypart;
    }
}
