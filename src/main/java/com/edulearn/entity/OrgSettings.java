package com.edulearn.entity;

import com.edulearn.enums.OrgType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organisation_settings")
public class OrgSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String orgName;

    private String displayName;
    private String logoPath;

    private String legalEntityName;
    private String registrationNumber;
    private LocalDate registrationDate;
    private String registeredCountry;
    private String vatNumber;

    @Enumerated(EnumType.STRING)
    private OrgType orgType;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String county;
    private String postcode;
    private String country;

    @Column(name = "poc1_name")  private String poc1Name;
    @Column(name = "poc1_title") private String poc1Title;
    @Column(name = "poc1_email") private String poc1Email;
    @Column(name = "poc1_phone") private String poc1Phone;

    @Column(name = "poc2_name")  private String poc2Name;
    @Column(name = "poc2_title") private String poc2Title;
    @Column(name = "poc2_email") private String poc2Email;
    @Column(name = "poc2_phone") private String poc2Phone;

    private LocalDate academicYearStart;
    private LocalDate academicYearEnd;
    private String curriculum;
    private String accreditationNo;
    private String website;

    @Column(columnDefinition = "TEXT")
    private String aboutText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
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
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPoc1Name() { return poc1Name; }
    public void setPoc1Name(String poc1Name) { this.poc1Name = poc1Name; }
    public String getPoc1Title() { return poc1Title; }
    public void setPoc1Title(String poc1Title) { this.poc1Title = poc1Title; }
    public String getPoc1Email() { return poc1Email; }
    public void setPoc1Email(String poc1Email) { this.poc1Email = poc1Email; }
    public String getPoc1Phone() { return poc1Phone; }
    public void setPoc1Phone(String poc1Phone) { this.poc1Phone = poc1Phone; }
    public String getPoc2Name() { return poc2Name; }
    public void setPoc2Name(String poc2Name) { this.poc2Name = poc2Name; }
    public String getPoc2Title() { return poc2Title; }
    public void setPoc2Title(String poc2Title) { this.poc2Title = poc2Title; }
    public String getPoc2Email() { return poc2Email; }
    public void setPoc2Email(String poc2Email) { this.poc2Email = poc2Email; }
    public String getPoc2Phone() { return poc2Phone; }
    public void setPoc2Phone(String poc2Phone) { this.poc2Phone = poc2Phone; }
    public LocalDate getAcademicYearStart() { return academicYearStart; }
    public void setAcademicYearStart(LocalDate academicYearStart) { this.academicYearStart = academicYearStart; }
    public LocalDate getAcademicYearEnd() { return academicYearEnd; }
    public void setAcademicYearEnd(LocalDate academicYearEnd) { this.academicYearEnd = academicYearEnd; }
    public String getCurriculum() { return curriculum; }
    public void setCurriculum(String curriculum) { this.curriculum = curriculum; }
    public String getAccreditationNo() { return accreditationNo; }
    public void setAccreditationNo(String accreditationNo) { this.accreditationNo = accreditationNo; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getAboutText() { return aboutText; }
    public void setAboutText(String aboutText) { this.aboutText = aboutText; }
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
