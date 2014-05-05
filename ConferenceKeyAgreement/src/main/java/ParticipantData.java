import java.math.BigInteger;
import java.util.*;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.probablePrime;

/**
 * @author Zuzana Melsova
 * Represents participant's protocol data.
 * Contains methods co compute conference key and other intermediate values.
 */

public class ParticipantData {

    private int id;

    // Safe prime of the form p = qr + 1, where q is a large prime.
    private BigInteger p;
    private static BigInteger q;

    // Generator with order q in finite field GF(p).
    private BigInteger g;

    // Total number of periods.
    private int numOfPeriods;

    private int currentPeriod = 1;

    // Private/public keys used to decrypt/encrypt sub-keys sent in re-keying phase.
    // Private key x is an element of Z_q*, public key y has form y = g^x mod p.
    private BigInteger privateKeyX;
    private BigInteger publicKeyY;

    // A randomly generated secret polynomial of degree t, where t is total number of periods.
    private List<BigInteger> secretPolynomial;

    // Polynomial evaluated at a value z, where z is current period.
    private BigInteger subKey;

    // List of values of the form g^c, where c is a coefficient of the secret polynomial.
    private List<BigInteger> publicPolynomial;

    // Private/public keys different for each period used to decrypt/encrypt sub-keys in combination with keys x and y.
    // Private key k is an element of Z_q^*, public key r has form r = g^k mod p.
    private BigInteger privateKeyK;
    private BigInteger publicKeyR;

    // Encrypted sub-keys s_i for each participant i of the form s_i = f(z) * (y_i)^(q-k), where f(z) is sub-key for period z.
    private Map<Integer, BigInteger> encryptedSubKeys;

    // Decrypted sub-keys from all participants.
    private Map<Integer, BigInteger> decryptedSubKeys;

    // List of all the other participants with information from them needed to compute the group key.
    private Map<Integer, PublicData> participants;

    //Conference key established by all the participants
    private BigInteger conferenceKey;


    public ParticipantData(int id) {
        this.id = id;
        participants = new HashMap<Integer, PublicData>();
    }

    public void updatePeriod() {
        currentPeriod++;
    }

    /**
     * Generates parameters p,q,g of the group for which Decisional Diffie-Hellman problem is assumed to be hard.
     */
    public void generateGroupParameters() {
        Random rnd = new Random();
        q = probablePrime(1024, rnd);
        BigInteger r = new BigInteger(2, rnd);
        do {
            r = r.add(ONE);
            p = q.multiply(r).add(ONE);
        } while (!(p.isProbablePrime(5)));

        do {
            BigInteger h = randomBigInt();
            g = h.modPow((p.subtract(ONE)).divide(q), p);
        } while (!(g.modPow(q, p)).equals(ONE));
    }

    /**
     * @return random integer x from Z_q^*
     */
    public static BigInteger randomBigInt() {
        Random rnd = new Random();
        do {
            BigInteger i = new BigInteger(q.bitLength(), rnd);
            if (i.compareTo(q) < 0 && i.compareTo(BigInteger.ZERO) != 0)
                return i;
        } while (true);
    }

    /**
     * Generates private key x and computes public key y of the form y = g^x mod p.
     */
    public void generatePublicKeyY() {
        privateKeyX = randomBigInt();
        publicKeyY = g.modPow(privateKeyX, p);
    }

    /**
     * Generates secret polynomial and computes values of the form g^c, where c is a coefficient of the secret polynomial
     * according to the Initial phase of the protocol.
     */
    public void computePublicPolynomial() {
        secretPolynomial = new ArrayList<BigInteger>();
        for (int i = 0; i <= numOfPeriods; i++) {
            secretPolynomial.add(i, randomBigInt());
        }
        publicPolynomial = new ArrayList<BigInteger>();
        for (int i = 0; i <= numOfPeriods; i++) {
            publicPolynomial.add(i, g.modPow(secretPolynomial.get(i), p));
        }
    }

    /**
     * Uses Horner`s method to evaluate the polynomial f(z), where z is current period.
     */
    public void evaluateSecretPolynomial() {
        subKey = new BigInteger(String.valueOf(secretPolynomial.get(numOfPeriods)));
        for (int j = numOfPeriods - 1; j >= 0; j--) {
            subKey = ((new BigInteger(String.valueOf(currentPeriod)).multiply(subKey)).add(secretPolynomial.get(j))).mod(q);
        }
    }

    /**
     * Encrypts the sub-key for each participant with his public key according to the Re-keying phase of the protocol.
     */
    public void encryptSubKeys() {
        privateKeyK = randomBigInt();
        publicKeyR = g.modPow(privateKeyK, p);
        BigInteger exponent = q.subtract(privateKeyK);
        encryptedSubKeys = new HashMap<Integer, BigInteger>();

        for (Map.Entry<Integer, PublicData> entry : participants.entrySet()) {
            encryptedSubKeys.put(entry.getKey(), ((subKey.mod(p)).multiply(entry.getValue().getPublicKeyY().modPow(exponent, p))).mod(p));
        }
    }

    /**
     * Decrypts sub-keys from all the other participants.
     */
    public void decryptSubKeys() {
        decryptedSubKeys = new HashMap<Integer, BigInteger>();
        for (Map.Entry<Integer, PublicData> entry : participants.entrySet()) {
            decryptedSubKeys.put(entry.getKey(), (entry.getValue().getEncryptedSubKey().multiply(entry.getValue().getPublicKeyR().modPow(privateKeyX, p))).mod(p));
        }
    }

    /**
     * Verifies the sub-keys according to the Key computation phase of the protocol.
     * @return true if the sub-keys are valid, false otherwise
     */
    public Boolean verifySubKeys() {
        for (Map.Entry<Integer, PublicData> participant : participants.entrySet()) {
            List<BigInteger> controlValues = new ArrayList<BigInteger>();
            controlValues.addAll(participant.getValue().getPublicPolynomial());
            BigInteger product = BigInteger.ONE;
            for (int j = 0; j <= numOfPeriods; j++) {
                int pow = (int) Math.pow(currentPeriod, j);
                BigInteger exponent = new BigInteger(String.valueOf(pow));
                product = ((controlValues.get(j).modPow(exponent, p)).multiply(product)).mod(p);
            }
            if (!(g.modPow(decryptedSubKeys.get(participant.getKey()), p).equals(product))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes a group key, which is sum of all the sub-keys.
     */
    public BigInteger computeConferenceKey() {
        conferenceKey = new BigInteger(String.valueOf(subKey));
        for (BigInteger value : decryptedSubKeys.values()) {
            conferenceKey = (conferenceKey.add(value)).mod(q);
        }
        return conferenceKey;
    }

    public int getNumOfPeriods() {
        return numOfPeriods;
    }

    public int getId() {
        return id;
    }

    public BigInteger getPublicKeyY() {
        return publicKeyY;
    }

    public List<BigInteger> getPublicPolynomial() {
        return publicPolynomial;
    }

    public BigInteger getPublicKeyR() {
        return publicKeyR;
    }

    public Map<Integer, BigInteger> getEncryptedSubKeys() {
        return encryptedSubKeys;
    }

    public Map<Integer, PublicData> getParticipants() {

        return participants;
    }

    public BigInteger getG() {
        return g;
    }

    public void setG(BigInteger g) {
        this.g = g;
    }

    public BigInteger getP() {
        return p;
    }

    public void setP(BigInteger p) {
        this.p = p;
    }

    public static BigInteger getQ() {
        return q;
    }

    public static void setQ(BigInteger q) {
        ParticipantData.q = q;
    }

    public int getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(int currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public void setNumOfPeriods(int numOfPeriods) {
        this.numOfPeriods = numOfPeriods;
    }
}
