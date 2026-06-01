package com.edulearn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OrgContactsRequest {

    @NotBlank
    private String poc1Name;
    private String poc1Title;
    @NotBlank
    @Email
    private String poc1Email;
    private String poc1Phone;

    private String poc2Name;
    private String poc2Title;
    private String poc2Email;
    private String poc2Phone;

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
}
