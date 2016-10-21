package cn.edu.buaa.crypto.encryption.hibe.bb04.generators;

import cn.edu.buaa.crypto.utils.PairingUtils;
import cn.edu.buaa.crypto.encryption.hibe.bb04.params.HIBEBB04CiphertextParameters;
import cn.edu.buaa.crypto.encryption.hibe.bb04.params.HIBEBB04CiphertextGenerationParameters;
import cn.edu.buaa.crypto.encryption.hibe.bb04.params.HIBEBB04PublicKeyParameters;
import cn.edu.buaa.crypto.algebra.generators.PairingKeyEncapsulationPairGenerator;
import cn.edu.buaa.crypto.algebra.params.PairingKeyEncapsulationPair;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.CipherParameters;

/**
 * Created by Weiran Liu on 15-10-1.
 *
 * Encapsulation key / ciphertext pair generation generator for Boneh-Boyen HIBE scheme.
 */
public class HIBEBB04KeyEncapsulationPairGenerator implements PairingKeyEncapsulationPairGenerator {

    private HIBEBB04CiphertextGenerationParameters params;

    public void init(CipherParameters params) {
        this.params = (HIBEBB04CiphertextGenerationParameters)params;
    }

    public PairingKeyEncapsulationPair generateEncryptionPair() {
        HIBEBB04PublicKeyParameters publicKeyParameters = this.params.getPublicKeyParameters();
        Pairing pairing = PairingFactory.getPairing(publicKeyParameters.getParameters());
        String[] ids = this.params.getIds();
        Element[] elementIds = PairingUtils.MapToZr(pairing, ids);

        Element s = pairing.getZr().newRandomElement().getImmutable();
        Element sessionKey = pairing.pairing(publicKeyParameters.getG1(), publicKeyParameters.getG2()).powZn(s).getImmutable();
        byte[] byteArraySessionKey = sessionKey.toBytes();

        Element B = publicKeyParameters.getG().powZn(s).getImmutable();
        Element[] Cs = new Element[ids.length];
        for (int i=0; i<Cs.length; i++){
            Cs[i] = publicKeyParameters.getG1().powZn(elementIds[i]).mul(publicKeyParameters.getHsAt(i)).powZn(s).getImmutable();
        }
        return new PairingKeyEncapsulationPair(
                byteArraySessionKey,
                new HIBEBB04CiphertextParameters(publicKeyParameters.getParameters(), ids.length, B, Cs));
    }
}
