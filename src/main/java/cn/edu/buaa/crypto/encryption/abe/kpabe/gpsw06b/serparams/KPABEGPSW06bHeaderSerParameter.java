package cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06b.serparams;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.utils.PairingUtils;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Weiran Liu on 2016/11/21.
 *
 * Goyal-Pandey-Sahai-Waters large-universe KP-ABE with random oracles header parameter.
 */
public class KPABEGPSW06bHeaderSerParameter extends PairingCipherSerParameter {
    private transient Element E2;
    private final byte[] byteArrayE2;

    private transient Map<String, Element> Es;
    private final Map<String, byte[]> byteArraysEs;

    public KPABEGPSW06bHeaderSerParameter(PairingParameters pairingParameters, Element E2, Map<String, Element> Es) {
        super(pairingParameters);

        this.E2 = E2.getImmutable();
        this.byteArrayE2 = this.E2.toBytes();

        this.Es = new HashMap<String, Element>();
        this.byteArraysEs = new HashMap<String, byte[]>();
        for (String attribute : Es.keySet()) {
            Element E = Es.get(attribute).duplicate().getImmutable();
            this.Es.put(attribute, E);
            this.byteArraysEs.put(attribute, E.toBytes());
        }
    }

    public Element getEsAt(String attribute) { return this.Es.get(attribute).duplicate(); }

    public Element getE2() { return this.E2.duplicate(); }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof KPABEGPSW06bHeaderSerParameter) {
            KPABEGPSW06bHeaderSerParameter that = (KPABEGPSW06bHeaderSerParameter)anObject;
            //Compare E2
            if (!PairingUtils.isEqualElement(this.E2, that.E2)){
                return false;
            }
            if (!Arrays.equals(this.byteArrayE2, that.byteArrayE2)) {
                return false;
            }
            //Compare Es
            if (!this.Es.equals(that.Es)){
                return false;
            }
            if (!PairingUtils.isEqualByteArrayMaps(this.byteArraysEs, that.byteArraysEs)) {
                return false;
            }
            //Compare Pairing Parameters
            return this.getParameters().toString().equals(that.getParameters().toString());
        }
        return false;
    }

    private void readObject(java.io.ObjectInputStream objectInputStream)
            throws java.io.IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Pairing pairing = PairingFactory.getPairing(this.getParameters());
        this.E2 = pairing.getG1().newElementFromBytes(this.byteArrayE2);
        this.Es = new HashMap<String, Element>();
        for (String attribute : this.byteArraysEs.keySet()) {
            this.Es.put(attribute, pairing.getG1().newElementFromBytes(this.byteArraysEs.get(attribute)).getImmutable());
        }
    }
}
