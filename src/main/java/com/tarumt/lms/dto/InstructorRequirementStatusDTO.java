package com.tarumt.lms.dto;

import lombok.Data;

@Data
public class InstructorRequirementStatusDTO {
    private boolean hasDigitalSignature; // true if digital signature submitted
    private boolean hasCertificate;      // true if at least one certificate submitted
    private boolean latestCertificatePending; // true if the latest certificate is still pending
    private boolean canSubmit;           // optional: whether user can submit new certificate
}
