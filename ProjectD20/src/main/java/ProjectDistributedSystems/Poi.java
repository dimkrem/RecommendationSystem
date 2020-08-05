package ProjectDistributedSystems;

import java.io.Serializable;

public class Poi implements Serializable {
    private   int id;
    private String name,category;
    private double latitude,longitude;
    private double score;

    public Poi(int id, String name,String category, double latitude, double longitude){
        this.id=id;
        this.name=name;
        this.category=category;
        this.longitude=longitude;
        this.latitude=latitude;
    }
    public Poi(int id,double score){
        this.id=id;
        this.score=score;
    }
    public double getScore(){
        return  score;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void printPoiData(){
        System.out.printf("Poi recommended has :\n Id:%s \n Name:%s \n Latitude:%f \n Longitude:%f \n Category:%s \n Score:%f \n",getId(),getName(),getLatitude(),getLongitude(),getCategory(),getScore());
    }
}
