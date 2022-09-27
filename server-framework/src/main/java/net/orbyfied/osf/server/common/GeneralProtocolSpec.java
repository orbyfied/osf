package net.orbyfied.osf.server.common;

import net.orbyfied.osf.server.ProtocolSpecification;
import net.orbyfied.osf.util.security.AsymmetricEncryptionProfile;
import net.orbyfied.osf.util.security.SymmetricEncryptionProfile;

public class GeneralProtocolSpec extends ProtocolSpecification {

    public static final GeneralProtocolSpec INSTANCE = new GeneralProtocolSpec();

    //////////////////////////////////////////////////

    @Override
    protected void create() {

    }

    //////////////////////////////////////////////////

    // encryption constants
    public static final int RSA_KEY_LENGTH = 1024;
    public static final int AES_KEY_LENGTH = 128;

    // encryption utility profiles
    public static final SymmetricEncryptionProfile EP_SYMMETRIC   = newSymmetricEncryptionProfile();
    public static final AsymmetricEncryptionProfile EP_ASYMMETRIC = newAsymmetricEncryptionProfile();

    /**
     * Creates a new asymmetric encryption profile following
     * the general protocol standard.
     * @return The profile.
     */
    public static AsymmetricEncryptionProfile newAsymmetricEncryptionProfile() {
        return new AsymmetricEncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", RSA_KEY_LENGTH);
    }

    /**
     * Creates a new symmetric encryption profile following
     * the general protocol standard.
     * @return The profile.
     */
    public static SymmetricEncryptionProfile newSymmetricEncryptionProfile() {
        return new SymmetricEncryptionProfile("AES", "ECB", "PKCS5Padding", "AES", AES_KEY_LENGTH);
    }

}
