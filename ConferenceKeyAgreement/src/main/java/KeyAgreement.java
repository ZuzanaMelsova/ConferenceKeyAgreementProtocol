import java.util.Map;
/**
 * @author Zuzana Melsova
 * This class handles start of key agreement protocol and adding/removing participants.
 */
public class KeyAgreement {

    private KeyAgreementEngine stateEngine;

    /**
     * Creates an instance which represents a participant with given id and port.
     * @param id
     * @param port
     */
    public KeyAgreement(int id, int port) {
        stateEngine = new KeyAgreementEngine(State.INITIAL,
                new ParticipantData(id),
                new Sockets(port));
    }

    /**
     * Starts key agreement protocol with given participants.
     * @param conferenceParticipants - ports associated with participants IDs
     */
    public void startKeyAgreement(Map<Integer, Integer> conferenceParticipants) {
        stateEngine.processEvent(new Event(EventType.STARTKEYAGREEMENT, conferenceParticipants));
    }

    /**
     * Starts re-keying phase without the participant which is calling this method.
     */
    public void leaveConference() {
        stateEngine.processEvent(new Event(EventType.LEAVE));
    }

    /**
     * Starts re-keying phase with the new participant with given id and port.
     * @param id - the new member's id
     * @param port - the new member's port
     */
    public void addParticipant(int id, int port) {
        stateEngine.processEvent(new Event(EventType.ADDPARTICIPANT, id, port));
    }

    public KeyAgreementEngine getStateEngine() {
        return stateEngine;
    }
}
