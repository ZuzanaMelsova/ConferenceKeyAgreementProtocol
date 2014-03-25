import java.math.BigInteger;
import java.util.*;


public class ParticipantInfo {

    private int id;
    private BigInteger privateKeyX;
    private BigInteger publicKeyY;
    private List<BigInteger> coefficients;
    private List<BigInteger> publicValuesGeneratedFromCoefficients;
    private BigInteger privateKeyK;
    private BigInteger publicKeyR;
    private BigInteger evaluatedPrivatePolynomial;
    private Map<Integer, BigInteger> encryptedSubKeys;
    private Map<Integer, BigInteger> decryptedSubKeys;
    private int currentPeriod;
    private List<PublicData> publicDataList;
    private BigInteger g;
    private BigInteger p;
    private static BigInteger q;
    private int numOfPeriods;


    public ParticipantInfo(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public BigInteger getPrivateKeyX() {
        return privateKeyX;
    }

    public BigInteger getPublicKeyY() {
        return publicKeyY;
    }

    public List<BigInteger> getCoefficients() {
        return coefficients;
    }

    public List<BigInteger> getPublicValuesGeneratedFromCoefficients() {
        return publicValuesGeneratedFromCoefficients;
    }

    public BigInteger getPrivateKeyK() {
        return privateKeyK;
    }

    public BigInteger getPublicKeyR() {
        return publicKeyR;
    }

    public BigInteger getEvaluatedPrivatePolynomial() {
        return evaluatedPrivatePolynomial;
    }

    public Map<Integer, BigInteger> getEncryptedSubKeys() {
        return encryptedSubKeys;
    }

    public Map<Integer, BigInteger> getDecryptedSubKeys() {
        return decryptedSubKeys;
    }

    public List<PublicData> getPublicDataList() {
        return publicDataList;
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
        ParticipantInfo.q = q;
    }

    public int getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(int currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public void updatePeriod() {
        if (currentPeriod++ > numOfPeriods) {
            //stopKeyAgreement();
        }

    }


    public static BigInteger randomBigInt() {
        Random rnd = new Random();
        do {
            BigInteger i = new BigInteger(q.bitLength(), rnd);
            if (i.compareTo(q) <= 0)
                return i;
        } while (true);
    }

    public void generatePublicKeyY() {
        privateKeyX = new BigInteger(String.valueOf((randomBigInt())));
        publicKeyY = g.modPow(privateKeyX, p);
    }

    public void computePublicValuesGeneratedFromCoefficients() {
        coefficients = new ArrayList<BigInteger>();

        for (int i = 0; i <= numOfPeriods; i++) {
            coefficients.add(i, new BigInteger(String.valueOf(randomBigInt())));

        }
        publicValuesGeneratedFromCoefficients = new ArrayList<BigInteger>();
        for (int i = 0; i <= numOfPeriods; i++) {
            publicValuesGeneratedFromCoefficients.add(i, g.modPow(coefficients.get(i), p));

        }
    }

    /**
     * Uses Horner`s method to evaluate the polynomial
     */
    public void computeEvaluatedPrivatePolynomial() {

        evaluatedPrivatePolynomial = coefficients.get(numOfPeriods);

        for (int j = numOfPeriods - 1; j >= 0; j--) {
            evaluatedPrivatePolynomial = ((new BigInteger(String.valueOf(currentPeriod)).multiply(evaluatedPrivatePolynomial)).add(coefficients.get(j))).mod(q);
        }
    }

    public void computeEncryptedSubKeys() {
        privateKeyK = new BigInteger(String.valueOf(randomBigInt()));
        publicKeyR = g.modPow(privateKeyK, p);
        BigInteger exponent = q.subtract(privateKeyK);
        encryptedSubKeys = new HashMap<Integer, BigInteger>();

        for (int i = 0; i < publicDataList.size(); i++) {
            PublicData participant = publicDataList.get(i);
            encryptedSubKeys.put(participant.getId(), ((evaluatedPrivatePolynomial.mod(p)).multiply(participant.getPublicKeyY().modPow(exponent, p))).mod(p));
        }
    }

    public void computeDecryptedSubKeys() {
        decryptedSubKeys = new HashMap<Integer, BigInteger>();
        for (int i = 0; i < publicDataList.size(); i++) {
            PublicData participant = publicDataList.get(i);
            decryptedSubKeys.put(participant.getId(), ((participant.getPublicKeyR().modPow(privateKeyX, p)).multiply((publicKeyY.modPow(q, p)).modInverse(p))).mod(p));
        }
    }

    public Boolean verifySubKeys() {
        for (int i = 0; i < publicDataList.size(); i++) {
            PublicData participant = publicDataList.get(i);
            List<BigInteger> controlValues = participant.getPublicValuesGeneratedFromCoefficients();
            BigInteger product = BigInteger.ONE;
            for (int j = 0; j <= numOfPeriods; j++) {
                BigInteger exp = new BigInteger(String.valueOf(currentPeriod ^ j));
                product = ((controlValues.get(j).modPow(exp, p)).multiply(product)).mod(p);
            }
            if (g.modPow(decryptedSubKeys.get(participant.getId()), p) != product) {
                return false;
            }
        }
        return true;
    }

    public BigInteger computeCommonSecretKey() {
        BigInteger commonSecretKey = evaluatedPrivatePolynomial;
        for (BigInteger value : decryptedSubKeys.values()) {
            (commonSecretKey.add(value)).mod(q);
        }
        return commonSecretKey;
    }

    public void addParticipant(int id, BigInteger publicKeyY, List<BigInteger> publicValuesGeneratedFromCoefficients) {
        publicDataList.add(new PublicData(id, publicKeyY, publicValuesGeneratedFromCoefficients));

    }

    public void addParticipant(int id) {
        publicDataList.add(new PublicData(id));

    }

    public void removeParticipant(int id) {
        for (int i = 0; i < publicDataList.size(); i++) {
            if (publicDataList.get(i).getId() == id) {
                publicDataList.remove(i);
            }
        }
    }


}
