/**
 * Created by zuzana on 3/17/14.
 */
public class Event {
    private EventType type;
    private Message.Msg message;

    public Event(EventType type) {
        this.type = type;
    }

    public Event(EventType type, Message.Msg message) {
        this.type = type;
        this.message = message;
    }

    public EventType getType() {
        return type;
    }

    public Message.Msg getMessage() {
        return message;
    }
}
