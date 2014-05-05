import java.util.Map;

/**
 * @author Zuzana Melsova
 * Info about event that occurred in state engine implemented in class KeyAgreementEngine.
 */
public class Event {
    private EventType type; // Type of event
    private Message.Msg message; // Received message
    private Map<Integer, Integer> participantIdAndPort; // participants selected by initiator

    // id an port of the new participant, if someone was added
    private int participantId;
    private int port;


    public Event(EventType type) {
        this.type = type;
    }

    public Event(EventType type, int id, int port) {
        this.participantId = id;
        this.port = port;
        this.type = type;
    }

    public Event(EventType type, Message.Msg message) {
        this.type = type;
        this.message = message;
    }

    public Event(EventType type, Map<Integer, Integer> participantIdAndPort) {
        this.type = type;
        this.participantIdAndPort = participantIdAndPort;
    }

    public EventType getType() {
        return type;
    }

    public Message.Msg getMessage() {
        return message;
    }

    public int getPort() {
        return port;
    }

    public int getParticipantId() {
        return participantId;
    }

    public Map<Integer, Integer> getParticipantIdAndPort() {
        return participantIdAndPort;
    }

}
