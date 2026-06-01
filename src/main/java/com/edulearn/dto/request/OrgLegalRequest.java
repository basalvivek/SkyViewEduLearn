package com.edulearn.dto.request;

import com.edulearn.enums.OrgType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class OrgLegalRequest {

    @NotBlank
    private String legalEntityName;

    @NotBlank
    private String registrationNumber;

    private LocalDate registrationDate;

    private String registeredCountry;

    private String vatNumber;

    private OrgType orgType;

    public String getLegalEntityName() { return legalEntityName; }
    public void setLegalEntityName(String legalEntityName) { this.legalEntityName = legalEntityName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }
    public String getRegisteredCountry() { return registeredCountry; }
    public void setRegisteredCountry(String registeredCountry) { this.registeredCountry = registeredCountry; }
    public String getVatNumber() { return vatNumber; }
    public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }
    public OrgType getOrgType() { return orgType; }
    public void setOrgType(OrgType orgType) { this.orgType = orgType; }
}
