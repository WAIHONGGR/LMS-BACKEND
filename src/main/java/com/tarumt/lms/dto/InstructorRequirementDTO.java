package com.tarumt.lms.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class InstructorRequirementDTO {
    private String digitalSignatureBase64; // base64 from canvas
    private List<MultipartFile> qualificationFiles; // can be one or multiple files
    private List<String> qualificationLevels;
    private List<String> fieldOfStudy;
}
