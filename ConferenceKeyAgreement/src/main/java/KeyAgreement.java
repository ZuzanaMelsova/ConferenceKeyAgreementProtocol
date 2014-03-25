import java.util.List;
import java.util.Map;

/**
 * The main class
 */
public class KeyAgreement {


    private StateClass stateEngine;


    public KeyAgreement(int id, int port) {
        stateEngine = new StateClass(State.INITIAL,
                new ParticipantInfo(id),
                new Sockets(port));
    }

    public void startKeyAgreement(Map<Integer, Integer> conferenceParticipants) {

        //stateEngine.processEvent(new Event(EventType.STARTKEYAGREEMENT));

    }

    public void leaveConference() {
        stateEngine.processEvent(new Event(EventType.LEAVE));

    }

    public void addParticipant() {
        stateEngine.processEvent(new Event(EventType.ADDPARTICIPANT));

    }

    public StateClass getStateEngine() {
        return stateEngine;
    }
}
