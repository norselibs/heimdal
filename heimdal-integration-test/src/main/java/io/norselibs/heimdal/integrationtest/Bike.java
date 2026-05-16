package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.HmExclude;
import io.norselibs.heimdal.HmLabel;
import io.norselibs.heimdal.HmMultiline;
import io.norselibs.heimdal.HmRequired;
import io.norselibs.heimdal.HmValidateOnBlur;

public class Bike {
    @HmRequired
    private String name;

    @HmRequired
    private BikeType bikeType;

    @HmLabel("Suspension Travel (mm)")
    @HmRequired
    @HmValidateOnBlur
    private int suspensionTravel;

    @HmMultiline
    @HmValidateOnBlur
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
