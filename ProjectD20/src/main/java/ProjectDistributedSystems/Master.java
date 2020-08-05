package ProjectDistributedSystems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class Master implements Serializable {
    private RealMatrix c,p,r,x,y;
    private int datarow,datacolumn;
    private double previouscost,cost,l;
    ServerSocket providerSocket;
    Socket connection = null;
    static int id;
    private ArrayList<Worker> workers=new ArrayList<Worker>();
    private static ArrayList<Client> clients=new ArrayList<Client>();

    //setters

    public void setDatacolumn(int datacolumn) {
        this.datacolumn = datacolumn;
    }

    public void setDatarow(int datarow) {
        this.datarow = datarow;
    }

    public void setR(RealMatrix r) {
        this.r = r;
    }

    public void setPreviouscost(double previouscost){
        this.previouscost=previouscost;
    }
    public void setCost(double cost){
        this.cost=cost;
    }
    public void setX(RealMatrix x){
        this.x=x;
    }

    public void setY(RealMatrix y) {
        this.y = y;
    }
    //getters
    public ArrayList<Worker> getWorkers() {
        return workers;
    }


    public int getDatacolumn() {
        return datacolumn;
    }

    public int getDatarow() {
        return datarow;
    }

    //calculations
    public void calculateCMatrix(RealMatrix r,int a) {
        RealMatrix c=MatrixUtils.createRealMatrix(r.getRowDimension(),r.getColumnDimension());
        for(int i=0;i<r.getRowDimension();i++){
            for(int j=0;j<r.getColumnDimension();j++){
                c.setEntry(i,j,1+a*r.getEntry(i,j));
            }
        }
        this.c=c;
    }

    public void calculatePMatrix(RealMatrix r) {
        RealMatrix p=MatrixUtils.createRealMatrix(r.getRowDimension(),r.getColumnDimension());
        for(int i=0;i<r.getRowDimension();i++){
            for(int j=0;j<r.getColumnDimension();j++){
                p.setEntry(i,j,(r.getEntry(i,j)==0)?0:1);
            }
        }
        this.p=p;
    }

    //score for user u and poi i
    public double calculateScore(int user, int poi,int k) {
        RealMatrix pui=(x.getRowMatrix(user).transpose()).multiply(y.getRowMatrix(user));
        return pui.getEntry(0,0);
    }

    //diafora metaksy 2 epaideyswn
    public double calculateError() {

        return previouscost-cost;

    }
    public double abs(double x){
        if (x<0) return -x;
        else return x;
    }
    public double calculateCost(double lambda,int k){


        double cost=0;
        for(int user=0;user<r.getRowDimension();user++){
            for(int poi=0;poi<r.getColumnDimension();poi++){
                double cost1=c.getEntry(user,poi);//cui
                double cost2=(p.getEntry(user,poi)-calculateScore(user,poi,k));//pui-xu^t*yi
                double cost3=cost2*cost2;//(pui-xu^t*yi)^2
                double cost4=cost1*cost3;//cui(pui-xu^t*yi)^2
                cost=cost+cost4;
            }
        }
        double s1=0;
        for(int user=0;user<x.getRowDimension();user++){
            double c=x.getRowMatrix(user).getFrobeniusNorm();
            double s2=c*c;
            s1=s1+s2;
        }
        double s3=0;
        for(int poi=0;poi<y.getRowDimension();poi++){
            double c=y.getRowMatrix(poi).getFrobeniusNorm();
            double s4=c*c;
            s3=s3+s4;
        }

        return cost+lambda*(s1+s3);
    }
    public ArrayList<Poi> calculateBestLocalPoisForUser(int u,int k) {
        ArrayList<Poi> a = new ArrayList<Poi>();
        double s=0;
        for (int user = 0; user < p.getRowDimension(); user++) {
            if (user == u) {
                for (int poi = 0; poi < p.getColumnDimension(); poi++) {
                    if (p.getEntry(user, poi) == 0) {
                        s=calculateScore(user,poi,k);
                        Poi p = new Poi(poi,s);
                        a.add(p);
                    }
                }
            }

        }

       bubbleSort(a);
        return a;
    }
    public  void bubbleSort(ArrayList<Poi> a) {
        int n = a.size();
        Poi temp = null;
        for(int i=0; i < n; i++){
            for(int j=1; j < (n-i); j++){
                if(a.get(j-1).getScore() < a.get(j).getScore()){
                    temp = a.get(j-1);
                    a.set(j-1,a.get(j));
                    a.set(j,temp);
                }
            }
        }
    }
    public  void connectPartsX(){
        for(int i=0;i<getWorkers().size();i++){
            for(int j=getWorkers().get(i).getStartx(),h=0;j<getWorkers().get(i).getFinishx()&&h<getWorkers().get(i).getXpart().getRowDimension();j++,h++){
                x.setRowMatrix(j,getWorkers().get(i).getXpart().getRowMatrix(h));
            }
        }
    }
    public  void connectPartsY(){
        for(int i=0;i<getWorkers().size();i++){
            for(int j=getWorkers().get(i).getStarty(),h=0;j<getWorkers().get(i).getFinishy()&&h<getWorkers().get(i).getYpart().getRowDimension();j++,h++){
                y.setRowMatrix(j,getWorkers().get(i).getYpart().getRowMatrix(h));
            }
        }
    }

    public void setLinesToProcessXmatrix()  {
        double sum=0;
        for(int i=0;i<getWorkers().size();i++) {
            sum = sum + getWorkers().get(i).getValue();
        }
        int[] lines=new int[getWorkers().size()];
        int a=getDatarow();
        for(int i=0;i<getWorkers().size();i++){
            double per=getWorkers().get(i).getValue()/sum;
            lines[i]=(int)(per*getDatarow());
            a=a-lines[i];

        }
        int start=0;
        for(int i=0;i<getWorkers().size();i++){
            if(i==getWorkers().size()-1) {
                getWorkers().get(i).setStartx(start);
                getWorkers().get(i).setFinishx(getDatarow());
                break;
            }
            getWorkers().get(i).setStartx(start);
            getWorkers().get(i).setFinishx(start+lines[i]);
            start=start+lines[i];
        }

    }

    public void setLinesToProcessYmatrix()  {
        double sum=0;
        for(int i=0;i<getWorkers().size();i++) {
            sum = sum + getWorkers().get(i).getValue();
        }
        int[] lines=new int[getWorkers().size()];
        int a=getDatarow();
        for(int i=0;i<getWorkers().size();i++){
            double per=getWorkers().get(i).getValue()/sum;
            lines[i]=(int)(per*getDatacolumn());
            a=a-lines[i];

        }
        int start=0;
        for(int i=0;i<getWorkers().size();i++){
            if(i==getWorkers().size()-1) {
                getWorkers().get(i).setStarty(start);
                getWorkers().get(i).setFinishy(getDatacolumn());
                break;
            }
            getWorkers().get(i).setStarty(start);
            getWorkers().get(i).setFinishy(start+lines[i]);
            start=start+lines[i];
        }

    }

//end



    //CONNECTIONS
    public  void initialize() {
        try {
            providerSocket = new ServerSocket(4321, 10);
            int co=0;
            /* Accept the connection */
            System.out.println("Master online...");
            int workersid=0;
            Thread t=null;
            while(co<3) {
                connection = providerSocket.accept();
                System.out.println("New Worker connected!");
                t=new InitThread(connection,c,p,workersid,this);
                t.start();
                t.join();
                ++co;
                ++workersid;
                if(co==3) break;
            }

        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    public  void DistributeXmatrix(RealMatrix x,RealMatrix y,int k) {
        try {
            providerSocket = new ServerSocket(4321, 10);
            /* Accept the connection */
            System.out.println("Master will distribute X and send Y whole.");
            Thread t=null;
            for(int i=0;i< getWorkers().size();i++) {
                connection = providerSocket.accept();
                System.out.println("Master is distributing X and sending y whole...");
                RealMatrix xpart=MatrixUtils.createRealMatrix(getWorkers().get(i).getFinishx()-getWorkers().get(i).getStartx(),k);
                for(int j=getWorkers().get(i).getStartx(),h=0;j<getWorkers().get(i).getFinishx()&&h<xpart.getRowDimension();j++,h++){
                    for(int p=0;p<xpart.getColumnDimension();p++){
                        xpart.setEntry(h,p,x.getEntry(j,p));
                    }
                }
                t=new DistXThread(connection,xpart,y,this);
                t.start();
                t.join();

            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    public  void DistributeYmatrix(RealMatrix x,RealMatrix y,int k) {
        try {
            providerSocket = new ServerSocket(4321, 10);
            /* Accept the connection */
            System.out.println("Master will distribute Y and send X whole.");
            Thread t=null;
            for(int i=0;i< getWorkers().size();i++) {
                connection = providerSocket.accept();
                System.out.println("Master is distributing Y and sending X whole to worker"+getWorkers().get(i).getWorkerId());
                RealMatrix ypart=MatrixUtils.createRealMatrix(getWorkers().get(i).getFinishy()-getWorkers().get(i).getStarty(),k);
                for(int j=getWorkers().get(i).getStarty(),h=0;j<getWorkers().get(i).getFinishy()&&h<ypart.getColumnDimension();j++,h++){
                    for(int p=0;p<ypart.getColumnDimension();p++){
                        ypart.setEntry(h,p,y.getEntry(j,p));
                    }
                }
                t=new DistYThread(connection,ypart,x,this);
                t.start();
                t.join();

            }
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void acceptXparts() {
        try {
            providerSocket = new ServerSocket(4321, 10);
            /* Accept the connection */
            System.out.println("Master  waiting processed x lines...");
            Thread t = null;
            Thread [] threads=new Thread[getWorkers().size()];
            for (int i = 0; i < workers.size(); i++) {
                connection = providerSocket.accept();
                t = new RecXpThread(connection, this);
                t.start();
                threads[i]=t;
            }
            for(int i=0;i<threads.length;i++) threads[i].join();
        }
            catch(IOException ioException){
                ioException.printStackTrace();
            } catch(InterruptedException e){
                e.printStackTrace();
            } finally{
                try {
                    providerSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        }
    public void acceptYparts() {
        try {
            providerSocket = new ServerSocket(4321, 10);
            /* Accept the connection */
            System.out.println("Master  waiting processed y lines...");
            Thread t = null;
            Thread [] threads=new Thread[getWorkers().size()];
            for (int i = 0; i < workers.size(); i++) {
                connection = providerSocket.accept();
                t = new RecYpThread(connection, this);
                t.start();
                threads[i]=t;
            }
            for(int i=0;i<threads.length;i++) threads[i].join();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        } catch(InterruptedException e){
            e.printStackTrace();
        } finally{
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }



    public static void main(String[] args) throws ClassNotFoundException, FileNotFoundException {
        //Creation of R Matrix which holds the real data
        Master m=new Master();
        m.setDatarow(835);
        m.setDatacolumn(1692);
        RealMatrix r=MatrixUtils.createRealMatrix(m.getDatarow(),m.getDatacolumn());
        for(int i=0 ; i<r.getRowDimension();i++){
            for(int j=0 ; j<r.getColumnDimension();j++){
                r.setEntry(i,j,0);
            }
        }

        String csv="C:/Users/visca/Desktop/ProjectD21/ProjectD20/src/main/java/ProjectDistributedSystems/input_matrix_non_zeros.csv";
        CSVReader reader=null;
        try {
            reader = new CSVReader(new FileReader(csv));
            String[] line;
            while ((line = reader.readNext()) != null) {
                r.setEntry(Integer.parseInt(line[0].trim()),Integer.parseInt(line[1].trim()),Integer.parseInt(line[2].trim()));
            }


        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //End
        //Initialization of X,Y
        int max=(r.getColumnDimension()>r.getRowDimension())?r.getColumnDimension():r.getRowDimension();
        int k=20;
        RealMatrix x=MatrixUtils.createRealMatrix(r.getRowDimension(),k);
        RealMatrix y=MatrixUtils.createRealMatrix(r.getColumnDimension(),k);
        RandomGenerator randomGenerator=new JDKRandomGenerator();
        randomGenerator.setSeed(1);
        for(int i=0 ; i<x.getRowDimension();i++){
            for(int j=0 ; j<x.getColumnDimension();j++){
                double val=randomGenerator.nextDouble();
                while(val==0||val==1) val=randomGenerator.nextDouble();
                x.setEntry(i,j,val);
            }
        }
        for(int i=0 ; i<y.getRowDimension();i++){
            for(int j=0 ; j<y.getColumnDimension();j++){
                double val=randomGenerator.nextDouble();
                while(val==0||val==1) val=randomGenerator.nextDouble();
                y.setEntry(i,j,val);
            }
        }
        //end
        //Begin setting of master
        m.setR(r);
        m.calculateCMatrix(r,40);
        m.calculatePMatrix(r);
        m.setX(x);
        m.setY(y);
        m.setL(0.1);
        m.initialize();
        for(int i=0;i<5;i++) {
            m.setLinesToProcessXmatrix();
            m.DistributeXmatrix(x, y, k);
            m.acceptXparts();
            m.connectPartsX();
            m.setLinesToProcessYmatrix();
            m.DistributeYmatrix(x,y,k);
            m.acceptYparts();
            m.connectPartsY();
            if(i>0){
                double cost=m.calculateCost(m.getL(),k);
                System.out.println("Cost function:"+cost);
                m.setPreviouscost(m.getCost());
                m.setCost(cost);
                System.out.println("Error:"+m.calculateError());
            }
            else{
                double cost=m.calculateCost(m.getL(),k);
                System.out.println("Cost function:"+cost);
                m.setPreviouscost(cost);
                m.setCost(cost);
            }

        }

        m.serveClient();

    }

    public double getCost() {
        return cost;
    }

    public void serveClient(){
        try {
            providerSocket = new ServerSocket(4321, 10);
            /* Accept the connection */
            System.out.println("Server online...");
            Thread t = null;
            while(true) {
                connection = providerSocket.accept();
                System.out.println("New client");
                t = new ClientHandler(connection,this);
                t.start();
                t.join();
            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        } catch(InterruptedException e){
            e.printStackTrace();
        } finally{
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public double getL() {
        return l;
    }

    public void setL(double l) {
        this.l = l;
    }
}

