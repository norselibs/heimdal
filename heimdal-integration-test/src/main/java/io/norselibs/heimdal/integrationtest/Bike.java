package io.norselibs.heimdal.integrationtest;

public class Bike {
    private String name;
    private BikeType bikeType;
    private int suspensionTravel;
    private String notes;

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public BikeType getBikeType()              { return bikeType; }
    public void setBikeType(BikeType t)        { this.bikeType = t; }

    public int getSuspensionTravel()           { return suspensionTravel; }
    public void setSuspensionTravel(int v)     { this.suspensionTravel = v; }

    public String getNotes()                   { return notes; }
    public void setNotes(String notes)         { this.notes = notes; }
}
