import java.math.BigInteger;
import java.util.List;

public class PublicData {
    private int id;
    private BigInteger publicKeyY;
    private List<BigInteger> publicValuesGeneratedFromCoefficients;
    private BigInteger publicKeyR;
    private BigInteger encryptedSubKey;

    public PublicData(int id) {
        this.id = id;
    }

    public PublicData(int id, BigInteger publicKeyY, List<BigInteger> publicValuesGeneratedFromCoefficients) {
        this.id = id;
        this.publicKeyY = publicKeyY;
        this.publicValuesGeneratedFromCoefficients = publicValuesGeneratedFromCoefficients;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigInteger getPublicKeyY() {
        return publicKeyY;
    }

    public void setPublicKeyY(BigInteger publicKeyY) {
        this.publicKeyY = publicKeyY;
    }

    public List<BigInteger> getPublicValuesGeneratedFromCoefficients() {
        return publicValuesGeneratedFromCoefficients;
    }

    public void setPublicValuesGeneratedFromCoefficients(List<BigInteger> publicValuesGeneratedFromCoefficients) {
        this.publicValuesGeneratedFromCoefficients = publicValuesGeneratedFromCoefficients;
    }

    public BigInteger getPublicKeyR() {
        return publicKeyR;
    }

    public void setPublicKeyR(BigInteger publicKeyR) {
        this.publicKeyR = publicKeyR;
    }

    public BigInteger getEncryptedSubKey() {
        return encryptedSubKey;
    }

    public void setEncryptedSubKey(BigInteger encryptedSubKey) {
        this.encryptedSubKey = encryptedSubKey;
    }
}