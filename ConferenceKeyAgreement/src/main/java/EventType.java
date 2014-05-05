/**
 * @author Zuzana Melsova
 */
public enum EventType {
    STARTKEYAGREEMENT, // Initiates the key agreement protocol
    MESSAGE, // Message was received.
    ADDPARTICIPANT, LEAVE, // Initiates the re-keying phase with new participant added/removed.
    CHECK // Starts control if all messages was already received for the current phase of the protocol.
}
