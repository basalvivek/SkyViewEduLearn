package com.edulearn.dto.response;

import com.edulearn.entity.OrgSettings;
import com.edulearn.enums.OrgType;

import java.time.LocalDate;

public class OrgSettingsResponse {

    private String orgName;
    private String displayName;
    private String logoUrl;
    private String legalEntityName;
    private String registrationNumber;
    private LocalDate registrationDate;
    private String registeredCountry;
    private String vatNumber;
    private OrgType orgType;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String county;
    private String postcode;
    private String country;
    private String poc1Name;
    private String poc1Title;
    private String poc1Email;
    private String poc1Phone;
    private String poc2Name;
    private String poc2Title;
    private String poc2Email;
    private String poc2Phone;
    private LocalDate academicYearStart;
    private LocalDate academicYearEnd;
    private String curriculum;
    private String accreditationNo;
    private String website;
    private String aboutText;

    public static OrgSettingsResponse from(OrgSettings s) {
        OrgSettingsResponse r = new OrgSettingsResponse();
        r.orgName = s.getOrgName();
        r.displayName = s.getDisplayName();
        r.logoUrl = s.getLogoPath() != null ? "/" + s.getLogoPath() : null; // e.g. /uploads/org/logo.png
        r.legalEntityName = s.getLegalEntityName();
        r.registrationNumber = s.getRegistrationNumber();
        r.registrationDate = s.getRegistrationDate();
        r.registeredCountry = s.getRegisteredCountry();
        r.vatNumber = s.getVatNumber();
        r.orgType = s.getOrgType();
        r.addressLine1 = s.getAddressLine1();
        r.addressLine2 = s.getAddressLine2();
        r.city = s.getCity();
        r.county = s.getCounty();
        r.postcode = s.getPostcode();
        r.country = s.getCountry();
        r.poc1Name = s.getPoc1Name();
        r.poc1Title = s.getPoc1Title();
        r.poc1Email = s.getPoc1Email();
        r.poc1Phone = s.getPoc1Phone();
        r.poc2Name = s.getPoc2Name();
        r.poc2Title = s.getPoc2Title();
        r.poc2Email = s.getPoc2Email();
        r.poc2Phone = s.getPoc2Phone();
        r.academicYearStart = s.getAcademicYearStart();
        r.academicYearEnd = s.getAcademicYearEnd();
        r.curriculum = s.getCurriculum();
        r.accreditationNo = s.getAccreditationNo();
        r.website = s.getWebsite();
        r.aboutText = s.getAboutText();
        return r;
    }

    public String getOrgName() { return orgName; }
    public String getDisplayName() { return displayName; }
    public String getLogoUrl() { return logoUrl; }
    public String getLegalEntityName() { return legalEntityName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public String getRegisteredCountry() { return registeredCountry; }
    public String getVatNumber() { return vatNumber; }
    public OrgType getOrgType() { return orgType; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getCounty() { return county; }
    public String getPostcode() { return postcode; }
    public String getCountry() { return country; }
    public String getPoc1Name() { return poc1Name; }
    public String getPoc1Title() { return poc1Title; }
    public String getPoc1Email() { return poc1Email; }
    public String getPoc1Phone() { return poc1Phone; }
    public String getPoc2Name() { return poc2Name; }
    public String getPoc2Title() { return poc2Title; }
    public String getPoc2Email() { return poc2Email; }
    public String getPoc2Phone() { return poc2Phone; }
    public LocalDate getAcademicYearStart() { return academicYearStart; }
    public LocalDate getAcademicYearEnd() { return academicYearEnd; }
    public String getCurriculum() { return curriculum; }
    public String getAccreditationNo() { return accreditationNo; }
    public String getWebsite() { return website; }
    public String getAboutText() { return aboutText; }
}
