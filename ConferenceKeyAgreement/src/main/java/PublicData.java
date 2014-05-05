import java.math.BigInteger;
import java.util.List;

/**
 * @author Zuzana Melsova
 * Data structure for saving information from other participants.
 */
public class PublicData {
    private int period; // latest period in which keyAgreementPart2 message was received from this participant
    private boolean part1received; // set on true if the keyAgreementPart1 message has been received from this participant
    private BigInteger publicKeyY;
    private List<BigInteger> publicPolynomial;
    private BigInteger publicKeyR;
    private BigInteger encryptedSubKey;


    public PublicData(BigInteger publicKeyY, List<BigInteger> publicPolynomial) {
        this.publicKeyY = publicKeyY;
        this.publicPolynomial = publicPolynomial;
    }
    public PublicData() {
    }

    public boolean isPart1received() {
        return part1received;
    }

    public void setPart1received(boolean part1received) {
        this.part1received = part1received;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public BigInteger getPublicKeyY() {
        return publicKeyY;
    }

    public void setPublicKeyY(BigInteger publicKeyY) {
        this.publicKeyY = publicKeyY;
    }

    public List<BigInteger> getPublicPolynomial() {
        return publicPolynomial;
    }

    public void setPublicPolynomial(List<BigInteger> publicPolynomial) {
        this.publicPolynomial = publicPolynomial;
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