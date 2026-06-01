package com.edulearn.dto.request;

import java.time.LocalDate;

public class OrgAcademicRequest {

    private LocalDate academicYearStart;
    private LocalDate academicYearEnd;
    private String curriculum;
    private String accreditationNo;
    private String website;
    private String aboutText;

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
}
