package org.bouncycastle.crypto.agreement;

import java.math.BigInteger;

import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
// BEGIN android-added
import org.bouncycastle.math.ec.ECCurve;
// END android-added
import org.bouncycastle.math.ec.ECPoint;

/**
 * P1363 7.2.1 ECSVDP-DH
 *
 * ECSVDP-DH is Elliptic Curve Secret Value Derivation Primitive,
 * Diffie-Hellman version. It is based on the work of [DH76], [Mil86],
 * and [Kob87]. This primitive derives a shared secret value from one
 * party's private key and another party's public key, where both have
 * the same set of EC domain parameters. If two parties correctly
 * execute this primitive, they will produce the same output. This
 * primitive can be invoked by a scheme to derive a shared secret key;
 * specifically, it may be used with the schemes ECKAS-DH1 and
 * DL/ECKAS-DH2. It assumes that the input keys are valid (see also
 * Section 7.2.2).
 */
public class ECDHBasicAgreement
    implements BasicAgreement
{
    private ECPrivateKeyParameters key;

    public void init(
        CipherParameters key)
    {
        this.key = (ECPrivateKeyParameters)key;
    }

    public int getFieldSize()
    {
        return (key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }

    public BigInteger calculateAgreement(
        CipherParameters pubKey)
    {
        // BEGIN android-changed
        ECPoint peerPoint = ((ECPublicKeyParameters) pubKey).getQ();
        ECCurve myCurve = key.getParameters().getCurve();
        if (peerPoint.isInfinity()) {
          throw new IllegalStateException("Infinity is not a valid public key for ECDH");
        }
        try {
          myCurve.validatePoint(peerPoint.getXCoord().toBigInteger(),
              peerPoint.getYCoord().toBigInteger());
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException("The peer public key must be on the curve for ECDH");
        }
        // Explicitly construct a public key using the private key's curve.
        ECPoint pubPoint = myCurve.createPoint(peerPoint.getXCoord().toBigInteger(),
            peerPoint.getYCoord().toBigInteger());
        ECPoint P = pubPoint.multiply(key.getD()).normalize();
        // END android-changed

        if (P.isInfinity())
        {
            throw new IllegalStateException("Infinity is not a valid agreement value for ECDH");
        }

        return P.getAffineXCoord().toBigInteger();
    }
}
