/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.pki;

import java.io.IOException;

public class CertificateException extends IOException {
    public static final byte BAD_EXTENSIONS = 1, CERTIFICATE_CHAIN_TOO_LONG = 2, EXPIRED = 3,
        UNAUTHORIZED_INTERMEDIATE_CA = 4, MISSING_SIGNATURE = 5, NOT_YET_VALID = 6, SITENAME_MISMATCH = 7,
        UNRECOGNIZED_ISSUER = 8, UNSUPPORTED_SIGALG = 9, INAPPROPRIATE_KEY_USAGE = 10, BROKEN_CHAIN = 11,
        ROOT_CA_EXPIRED = 12, UNSUPPORTED_PUBLIC_KEY_TYPE = 13, VERIFICATION_FAILED = 14;
    private final Certificate cert;
    private final byte reason;

    public CertificateException(Certificate certificate, byte status) { this(null, certificate, status); }
    public CertificateException(String message, Certificate certificate, byte status) {
        super(message);
        cert = certificate;
        reason = status;
    }
    public Certificate getCertificate() { return cert; }
    public byte getReason() { return reason; }
}
