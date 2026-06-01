package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @NotBlank @Size(max = 120)
    private String fullName;
    @Size(max = 100)  private String displayName;
    @Size(max = 100)  private String jobTitle;
    @Size(max = 100)  private String department;
    @Size(max = 500)  private String bio;
    @Size(max = 30)   private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String v) { this.jobTitle = v; }
    public String getDepartment() { return department; }
    public void setDepartment(String v) { this.department = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
}
