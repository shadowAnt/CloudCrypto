package cn.edu.buaa.crypto.utils;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.jpbc.PairingParametersGenerator;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import it.unisa.dia.gas.plaf.jpbc.pairing.a1.TypeA1CurveGenerator;
import it.unisa.dia.gas.plaf.jpbc.pairing.parameters.PropertiesParameters;
import it.unisa.dia.gas.plaf.jpbc.util.ElementUtils;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by Weiran Liu on 2016/8/24.
 *
 * Utilities for pairing-based cryptography.
 */
public class PairingUtils {
    public static final String PATH_a_160_512 = "params/a_160_512.properties";
    public static final String PATH_a_320_512 = "params/a_320_512.properties";
    public static final String PATH_a1_2_256 = "params/a1_2_256.properties";
    public static final String PATH_a1_3_256 = "params/a1_3_256.properties";
    public static final String PATH_a1_2_512 = "params/a1_2_512.properties";
    public static final String PATH_a1_3_512 = "params/a1_3_512.properties";

    public enum PairingGroupType {
        Zr, G1, G2, GT,
    }

//    public static final PairingParameters DEFAULT_TYPE_A_160_512_PAIRING_PARAMETER = PairingFactory.getPairingParameters()

    /**
     * Generate type A parameter for further used in paiaring-based cryptography.
     * @param rBitLength Bit length for the group Z_r
     * @param qBitLength Bit length for the group G and G_T
     * @return Type A pairing parameters
     */
    public static PropertiesParameters GenerateTypeAParameters(int rBitLength, int qBitLength) {
        PropertiesParameters parameters;
        Pairing pairing;
        Element g;
        // Generate curve parameters
        while (true) {
            parameters = generate_type_a_curve_params(rBitLength, qBitLength);
            pairing = PairingFactory.getPairing(parameters);
            g = pairing.getG1().newRandomElement().getImmutable();
            if (!pairing.pairing(g, g).isOne()) { break; }
        }
        return parameters;
    }

    public static PropertiesParameters GenerateTypeA1Parameters(int qBitLength) {
        PropertiesParameters parameters;
        Pairing pairing;
        Element generator;
        Element g;

        // Generate curve parameters
        while (true) {
            parameters = generate_type_a1_curve_params(qBitLength);
            pairing = PairingFactory.getPairing(parameters);
            generator = pairing.getG1().newRandomElement().getImmutable();
            g = ElementUtils.getGenerator(pairing, generator, parameters, 0, 3).getImmutable();
            if (!pairing.pairing(g, g).isOne()) { break; }
        }
        return parameters;
    }

    private static PropertiesParameters generate_type_a_curve_params(int rBitLength, int qBitLength) {
        PairingParametersGenerator parametersGenerator = new TypeACurveGenerator(rBitLength, qBitLength);
        return (PropertiesParameters) parametersGenerator.generate();
    }

    private static PropertiesParameters generate_type_a1_curve_params(int qBitLength) {
        PairingParametersGenerator parametersGenerator = new TypeA1CurveGenerator(3, qBitLength);
        return (PropertiesParameters) parametersGenerator.generate();
    }

    /**
     * A standard collision resistant hash function implementations used privately for Map.
     * The used hash function depends on the security parameter of the pairing-based cryptography system.
     * If the security parameter is less than 256, we choose SHA-256;
     * If the security parameter is greater than 256, but less than 384, we choose SHA-384;
     * If the security parameter is greater than 384, we choose SHA-512;
     * @param bitLength security parameter of the underlying cryptography system
     * @param message mmessage to be hashed
     * @return hash result
     */
    private static byte[] hash(int bitLength, byte[] message) {
        MessageDigest md = null;
        try {
            if (bitLength <= 256) {
                md = MessageDigest.getInstance("SHA-256");
            } else if (bitLength <= 384) {
                md = MessageDigest.getInstance("SHA-384");
            } else if (bitLength <= 512) {
                md = MessageDigest.getInstance("SHA-512");
            } else {
                md = MessageDigest.getInstance("SHA-512");
            }
        } catch (NoSuchAlgorithmException e) {
            //Impossible to get this exception
            e.printStackTrace();
        }
        md.update(message);
        return md.digest();
    }

    /**
     * Map a byte array to Element in Zr
     * @param pairing pairing of the underlying cryptography system
     * @param message message to be hashed
     * @return Zr Element mapping the message
     */
    public static Element MapToZr(Pairing pairing, byte[] message) {
        byte[] shaResult = hash(512, message);
        Element hash = pairing.getZr().newElement().setFromHash(shaResult, 0, shaResult.length).getImmutable();
        return hash;
    }

    public static Element MapToFirstHalfZr(Pairing pairing, byte[] message) {
        byte[] shaResult = hash(512, message);
        byte[] hash = pairing.getZr().newElement().setFromHash(shaResult, 0, shaResult.length).toBytes();
        hash[0] &= 0xEF;
        return pairing.getZr().newElementFromBytes(hash).getImmutable();
    }

    public static Element MapToSecondHalfZr(Pairing pairing, byte[] message) {
        byte[] shaResult = hash(512, message);
        byte[] hash = pairing.getZr().newElement().setFromHash(shaResult, 0, shaResult.length).toBytes();
        hash[0] |= 0x80;
        return pairing.getZr().newElementFromBytes(hash).getImmutable();
    }

    /**
     * Map a String to Element in Zr
     * @param pairing pairing of the underlying cryptography system
     * @param message message to be hashed
     * @return Zr Element mapping the message
     */
    public static Element MapToZr(Pairing pairing, String message) {
        if (message == null) {
            return null;
        }
        return MapToZr(pairing, message.getBytes());
    }

    public static Element MapToFirstHalfZr(Pairing pairing, String message) {
        if (message == null) {
            return null;
        }
        return MapToFirstHalfZr(pairing, message.getBytes());
    }

    /**
     * Map several byte arrays to Elements in Zr
     * @param pairing pairing of the underlying cryptography system
     * @param message messages to be hashed
     * @return Zr Element arrays mapping the messages
     */
    public static Element[] MapToZr(Pairing pairing, byte[][] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToZr(pairing, message[i]);
        }
        return elements;
    }

    public static Element[] MapToFirstHalfZr(Pairing pairing, byte[][] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToFirstHalfZr(pairing, message[i]);
        }
        return elements;
    }

    /**
     * Map a String array to Elements in Zr
     * @param pairing pairing of the underlying cryptography system
     * @param message messages to be hashed
     * @return Zr Element arrays mapping the messages
     */
    public static Element[] MapToZr(Pairing pairing, String[] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToZr(pairing, message[i]);
        }
        return elements;
    }

    public static Element[] MapToFirstHalfZr(Pairing pairing, String[] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToFirstHalfZr(pairing, message[i]);
        }
        return elements;
    }

    /**
     * Map a byte array to Element in G1
     * @param pairing pairing of the underlying cryptography system
     * @param message message to be hashed
     * @return G1 Element mapping the message
     */
    public static Element MapToG1(Pairing pairing, byte[] message) {
        byte[] shaResult = hash(512, message);
        Element hash = pairing.getG1().newElement().setFromHash(shaResult, 0, shaResult.length).getImmutable();
        return hash;
    }

    /**
     * Map several byte arrays to Elements in G1
     * @param pairing pairing of the underlying cryptography system
     * @param message messages to be hashed
     * @return G1 Element arrays mapping the messages
     */
    public static Element[] MapToG1(Pairing pairing, byte[][] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToG1(pairing, message[i]);
        }
        return elements;
    }

    /**
     * Map a byte array to Element in G2
     * @param pairing pairing of the underlying cryptography system
     * @param message message to be hashed
     * @return G2 Element mapping the message
     */
    public static Element MapToG2(Pairing pairing, byte[] message){
        byte[] shaResult = hash(512, message);
        Element hash = pairing.getG2().newElement().setFromHash(shaResult, 0, shaResult.length).getImmutable();
        return hash;
    }

    /**
     * Map several byte arrays to Elements in G2
     * @param pairing pairing of the underlying cryptography system
     * @param message messages to be hashed
     * @return G2 Element arrays mapping the messages
     */
    public static Element[] MapToG2(Pairing pairing, byte[][] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToG2(pairing, message[i]);
        }
        return elements;
    }

    /**
     * Map a byte array to Element in GT
     * @param pairing pairing of the underlying cryptography system
     * @param message message to be hashed
     * @return GT Element mapping the message
     */
    public static Element MapToGT(Pairing pairing, byte[] message){
        byte[] shaResult = hash(512, message);
        Element hash = pairing.getGT().newElement().setFromHash(shaResult, 0, shaResult.length).getImmutable();
        return hash;
    }

    /**
     * Map several byte arrays to Elements in GT
     * @param pairing pairing of the underlying cryptography system
     * @param message messages to be hashed
     * @return GT Element arrays mapping the messages
     */
    public static Element[] MapToGT(Pairing pairing, byte[][] message){
        Element[] elements = new Element[message.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = PairingUtils.MapToGT(pairing, message[i]);
        }
        return elements;
    }

    public static boolean isEqualElement(final Element thisElement, final Element thatElement) {
        if (thisElement == null && thatElement != null) {
            return false;
        }
        if (thisElement != null && thatElement == null) {
            return false;
        }
        if (thisElement == thatElement) {
            return true;
        }
        String stringThisElement = new String(Hex.encode(thisElement.toBytes()));
        String stringThatElement = new String(Hex.encode(thatElement.toBytes()));
        return (stringThisElement.equals(stringThatElement));
    }

    public static boolean isEqualElementArray(final Element[] thisElementArray, final Element[] thatElementArray) {
        if (thisElementArray == thatElementArray) {
            return true;
        }
        if (thisElementArray.length != thatElementArray.length) {
            return false;
        }
        for (int i=0; i<thisElementArray.length; i++){
            if (!(PairingUtils.isEqualElement(thisElementArray[i], thatElementArray[i]))){
                return false;
            }
        }
        return true;
    }

    public static boolean isEqualByteArrays(final byte[][] thisByteArrays, final byte[][] thatByteArrays) {
        if (thisByteArrays == thatByteArrays) {
            return true;
        }
        if (thisByteArrays.length != thatByteArrays.length) {
            return false;
        }
        for (int i=0; i<thisByteArrays.length; i++){
            if (!(Arrays.equals(thisByteArrays[i], thatByteArrays[i]))){
                return false;
            }
        }
        return true;
    }

    public static byte[][] GetElementArrayBytes(Element[] elementArray) {
        byte[][] byteArrays = new byte[elementArray.length][];
        for (int i = 0; i < byteArrays.length; i++) {
            if (elementArray[i] == null) {
                byteArrays[i] = null;
                continue;
            }
            byteArrays[i] = elementArray[i].toBytes();
        }
        return byteArrays;
    }

    public static Element[] GetElementArrayFromBytes(Pairing pairing, byte[][] byteArrays, PairingGroupType groupType) {
        Element[] elementArray = new Element[byteArrays.length];
        for (int i = 0; i < elementArray.length; i++) {
            if (byteArrays[i] == null) {
                elementArray[i] = null;
                continue;
            }
            switch (groupType) {
                case Zr: elementArray[i] = pairing.getZr().newElementFromBytes(byteArrays[i]);
                    break;
                case G1: elementArray[i] = pairing.getG1().newElementFromBytes(byteArrays[i]);
                    break;
                case G2: elementArray[i] = pairing.getG2().newElementFromBytes(byteArrays[i]);
                    break;
                case GT: elementArray[i] = pairing.getGT().newElementFromBytes(byteArrays[i]);
                    break;
                default:
                    throw new RuntimeException("Invalid pairing group type.");
            }
        }
        return elementArray;
    }
}