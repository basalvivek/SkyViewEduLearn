package com.edulearn.service;

import com.edulearn.dto.request.*;
import com.edulearn.dto.response.OrgSettingsResponse;
import com.edulearn.entity.OrgSettings;
import com.edulearn.entity.User;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.OrgSettingsRepository;
import com.edulearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class OrgSettingsService {

    private final OrgSettingsRepository repo;
    private final UserRepository userRepo;

    @Value("${app.upload.dir:src/main/resources/static/uploads/org}")
    private String uploadDir;

    public OrgSettingsService(OrgSettingsRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public OrgSettingsResponse getSettings() {
        return OrgSettingsResponse.from(getSingleton());
    }

    @Transactional
    public OrgSettingsResponse updateBranding(OrgBrandingRequest req, String actorEmail) {
        OrgSettings s = getSingleton();
        s.setOrgName(req.getOrgName());
        s.setDisplayName(req.getDisplayName());
        s.setUpdatedBy(findUser(actorEmail));
        return OrgSettingsResponse.from(repo.save(s));
    }

    @Transactional
    public OrgSettingsResponse updateLegal(OrgLegalRequest req, String actorEmail) {
        OrgSettings s = getSingleton();
        s.setLegalEntityName(req.getLegalEntityName());
        s.setRegistrationNumber(req.getRegistrationNumber());
        s.setRegistrationDate(req.getRegistrationDate());
        s.setRegisteredCountry(req.getRegisteredCountry());
        s.setVatNumber(req.getVatNumber());
        s.setOrgType(req.getOrgType());
        s.setUpdatedBy(findUser(actorEmail));
        return OrgSettingsResponse.from(repo.save(s));
    }

    @Transactional
    public OrgSettingsResponse updateAddress(OrgAddressRequest req, String actorEmail) {
        OrgSettings s = getSingleton();
        s.setAddressLine1(req.getAddressLine1());
        s.setAddressLine2(req.getAddressLine2());
        s.setCity(req.getCity());
        s.setCounty(req.getCounty());
        s.setPostcode(req.getPostcode());
        s.setCountry(req.getCountry());
        s.setUpdatedBy(findUser(actorEmail));
        return OrgSettingsResponse.from(repo.save(s));
    }

    @Transactional
    public OrgSettingsResponse updateContacts(OrgContactsRequest req, String actorEmail) {
        OrgSettings s = getSingleton();
        s.setPoc1Name(req.getPoc1Name());
        s.setPoc1Title(req.getPoc1Title());
        s.setPoc1Email(req.getPoc1Email());
        s.setPoc1Phone(req.getPoc1Phone());
        s.setPoc2Name(req.getPoc2Name());
        s.setPoc2Title(req.getPoc2Title());
        s.setPoc2Email(req.getPoc2Email());
        s.setPoc2Phone(req.getPoc2Phone());
        s.setUpdatedBy(findUser(actorEmail));
        return OrgSettingsResponse.from(repo.save(s));
    }

    @Transactional
    public OrgSettingsResponse updateAcademic(OrgAcademicRequest req, String actorEmail) {
        OrgSettings s = getSingleton();
        s.setAcademicYearStart(req.getAcademicYearStart());
        s.setAcademicYearEnd(req.getAcademicYearEnd());
        s.setCurriculum(req.getCurriculum());
        s.setAccreditationNo(req.getAccreditationNo());
        s.setWebsite(req.getWebsite());
        s.setAboutText(req.getAboutText());
        s.setUpdatedBy(findUser(actorEmail));
        return OrgSettingsResponse.from(repo.save(s));
    }

    @Transactional
    public String uploadLogo(MultipartFile file, String actorEmail) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String filename = "logo_" + System.currentTimeMillis() + getExtension(file.getOriginalFilename());
        Path dest = dir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        OrgSettings s = getSingleton();
        // stored as relative path within static — served as /uploads/org/filename
        s.setLogoPath("uploads/org/" + filename);
        s.setUpdatedBy(findUser(actorEmail));
        repo.save(s);
        return "/uploads/org/" + filename;
    }

    private OrgSettings getSingleton() {
        return repo.findTopByOrderByCreatedAtAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Organisation settings not found"));
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".png";
    }
}
