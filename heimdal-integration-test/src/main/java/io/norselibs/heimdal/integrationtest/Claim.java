package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.HmExclude;
import io.norselibs.heimdal.HmLabel;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Claim {
    @HmExclude private int id;

    // Claimant section
    @HmLabel("Full Name")  private String claimantName;
    @HmLabel("Policy Number") private String policyNumber;
    @HmLabel("Contact Email")  private String contactEmail;

    // Claim details
    @HmLabel("Claim Type")     private ClaimType claimType;
    @HmLabel("Incident Date")  private LocalDate incidentDate;

    // AUTO fields
    @HmLabel("Vehicle VIN")         private String vehicleVin;
    @HmLabel("Accident Location")   private String accidentLocation;

    // HOME fields
    @HmLabel("Property Address")    private String propertyAddress;
    @HmLabel("Damage Type")         private DamageType damageType;

    // HEALTH fields
    @HmLabel("Provider")            private String provider;
    @HmLabel("Diagnosis Code")      private String diagnosisCode;

    // TRAVEL fields
    @HmLabel("Destination")         private String destination;
    @HmLabel("Trip Start Date")     private LocalDate tripStartDate;

    // Common
    @HmLabel("Description")         private String description;
    @HmLabel("Estimated Amount")    private BigDecimal estimatedAmount;

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getClaimantName()                 { return claimantName; }
    public void setClaimantName(String v)           { this.claimantName = v; }
    public String getPolicyNumber()                 { return policyNumber; }
    public void setPolicyNumber(String v)           { this.policyNumber = v; }
    public String getContactEmail()                 { return contactEmail; }
    public void setContactEmail(String v)           { this.contactEmail = v; }
    public ClaimType getClaimType()                 { return claimType; }
    public void setClaimType(ClaimType v)           { this.claimType = v; }
    public LocalDate getIncidentDate()              { return incidentDate; }
    public void setIncidentDate(LocalDate v)        { this.incidentDate = v; }
    public String getVehicleVin()                   { return vehicleVin; }
    public void setVehicleVin(String v)             { this.vehicleVin = v; }
    public String getAccidentLocation()             { return accidentLocation; }
    public void setAccidentLocation(String v)       { this.accidentLocation = v; }
    public String getPropertyAddress()              { return propertyAddress; }
    public void setPropertyAddress(String v)        { this.propertyAddress = v; }
    public DamageType getDamageType()               { return damageType; }
    public void setDamageType(DamageType v)         { this.damageType = v; }
    public String getProvider()                     { return provider; }
    public void setProvider(String v)               { this.provider = v; }
    public String getDiagnosisCode()                { return diagnosisCode; }
    public void setDiagnosisCode(String v)          { this.diagnosisCode = v; }
    public String getDestination()                  { return destination; }
    public void setDestination(String v)            { this.destination = v; }
    public LocalDate getTripStartDate()             { return tripStartDate; }
    public void setTripStartDate(LocalDate v)       { this.tripStartDate = v; }
    public String getDescription()                  { return description; }
    public void setDescription(String v)            { this.description = v; }
    public BigDecimal getEstimatedAmount()          { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal v)    { this.estimatedAmount = v; }
}
