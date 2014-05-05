import com.google.protobuf.ByteString;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Zuzana Melsova
 *         This class implements a state machine for the protocol run.
 */
public class KeyAgreementEngine {

    static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KeyAgreementEngine.class);

    private State state;
    private ParticipantData participantData;
    private Sockets sockets;


    /**
     * Constructor
     * Define method messageReceived used in class Sockets.
     * @param state
     * @param participantData
     * @param sockets
     */
    public KeyAgreementEngine(State state, ParticipantData participantData, Sockets sockets) {
        this.state = state;
        this.participantData = participantData;
        this.sockets = sockets;
        this.sockets.setMessageListener(new MessageListener() {
            @Override
            public void messageReceived(Message.Msg message) {
                processEvent(new Event(EventType.MESSAGE, message));
            }
        });
    }

    /**
     * Implements a state machine for the protocol run.
     * @param event
     */
    public void processEvent(Event event) {
        switch (state) {

            case INITIAL: {
                switch (event.getType()) {

                    case STARTKEYAGREEMENT: {
                        // Initiate the protocol run by sending InitMsg to selected participants.

                        LOGGER.info("Starting key agreement protocol from port " + sockets.getMyPort());

                        // Set participants
                        sockets.getPorts().putAll(event.getParticipantIdAndPort());
                        for (Integer id : event.getParticipantIdAndPort().keySet()) {
                            participantData.getParticipants().put(id, new PublicData());
                        }

                        participantData.generateGroupParameters();

                        // Generate long-term public values
                        participantData.generatePublicKeyY();
                        participantData.computePublicPolynomial();

                        // Add my port to complete the list
                        event.getParticipantIdAndPort().put(participantData.getId(), sockets.getMyPort());

                        // Initiate the protocol by sending InitMsg message
                        sendInitMsg(event.getParticipantIdAndPort());

                        // Start the first part of the protocol, send KeyAgreementPart1 message
                        sendKeyAgreementPart1();
                        state = State.PART1SENT;

                        // Check if all KeyAgreementPart1 messages have been received.
                        processEvent(new Event(EventType.CHECK));
                        break;
                    }
                    case MESSAGE: {
                        switch (event.getMessage().getType()) {

                            case INITMSG: {
                                LOGGER.info("Protocol initialization started, sending message KeyAgreementPart1...");

                                saveDataFromInitMsg(event.getMessage());

                                // Generate long-term public values
                                participantData.generatePublicKeyY();
                                participantData.computePublicPolynomial();

                                // Start the first part of the protocol, send KeyAgreementPart1 message
                                sendKeyAgreementPart1();

                                state = State.PART1SENT;
                                // Check if all KeyAgreementPart1 messages have been received.
                                processEvent(new Event(EventType.CHECK));
                                break;
                            }
                            case KEYAGREEMENTPART1: {
                                // Initialization message has not arrived yet. Save data from KeyAgreementPart1 message for later use.
                                saveDataFromKeyAgreementPart1(event.getMessage());
                                break;
                            }
                            case ADDPARTICIPANT: {
                                // Has been added to existing group. Send Joining message.

                                saveDataFromAddParticipantMsg(event.getMessage());

                                // Generate long-term public values
                                participantData.generatePublicKeyY();
                                participantData.computePublicPolynomial();
                                sendJoiningMsg(sockets.getMyPort());

                                // Start the re-keying phase of the protocol, send KeyAgreementPart2 message.
                                participantData.evaluateSecretPolynomial();
                                participantData.encryptSubKeys();
                                sendKeyAgreementPart2();
                                state = State.PART2SENT;

                                // Check if all KeyAgreementPart2 messages have been received.
                                processEvent(new Event(EventType.CHECK));
                            }
                        }
                    }
                }
                break;
            }
            //
            case PART1SENT: {
                switch (event.getType()) {
                    case CHECK: {
                        // Check if all KeyAgreementPart1 messages have been received.
                        boolean continueToPart2 = true;
                        for (PublicData data : participantData.getParticipants().values()) {
                            if (!data.isPart1received()) {
                                continueToPart2 = false;
                                break;
                            }
                        }


                        if (continueToPart2) {
                            // Send second part of the key agreement data

                            LOGGER.info("Sending key agreement data part 2...");

                            participantData.evaluateSecretPolynomial();
                            participantData.encryptSubKeys();
                            sendKeyAgreementPart2();
                            state = State.PART2SENT;

                            // Check if all KeyAgreementPart2 messages have been received.
                            processEvent(new Event(EventType.CHECK));
                        }
                        break;
                    }
                    case MESSAGE: {
                        switch (event.getMessage().getType()) {
                            case KEYAGREEMENTPART1: {
                                // Process KeyAgreementPart1 message, check if all KeyAgreementPart1 messages have been received.
                                saveDataFromKeyAgreementPart1(event.getMessage());
                                processEvent(new Event(EventType.CHECK));
                            }
                            break;
                            case KEYAGREEMENTPART2: {
                                // Not all KeyAgreementPart1 messages have arrived yet. Save data from KeyAgreementPart2 message for later use.
                                saveDataFromKeyAgreementPart2(event.getMessage());
                            }
                        }
                    }
                }
            }
            break;
            case PART2SENT: {
                switch (event.getType()) {
                    case CHECK: {
                        // Check if all KeyAgreementPart2 messages have been received.
                        boolean part2received = true;
                        for (PublicData data : participantData.getParticipants().values()) {
                            if (data.getPeriod() != participantData.getCurrentPeriod()) {
                                part2received = false;
                                break;
                            }
                        }

                        if (part2received) {
                            // Compute secret key
                            participantData.decryptSubKeys();
                            if (participantData.verifySubKeys()) {
                                BigInteger key = participantData.computeConferenceKey();
                                System.out.println("CONFERENCE KEY: " + key);
                                LOGGER.info("Key agreement succeeded.");
                            } else {
                                LOGGER.error("Key verification failed, stopping key agreement protocol.");
                                stopKeyAgreement();
                            }
                            state = State.WAITFORNEWPERIOD;
                        }
                        break;
                    }
                    case MESSAGE: {
                        if (event.getMessage().getType().equals(Message.Msg.Type.KEYAGREEMENTPART2)) {
                            // Process KeyAgreementPart2 message, check if all KeyAgreementPart2 messages have been received.
                            saveDataFromKeyAgreementPart2(event.getMessage());
                            processEvent(new Event(EventType.CHECK));
                        }
                    }
                }
            }
            break;

            case WAITFORNEWPERIOD: {
                switch (event.getType()) {
                    case MESSAGE: {
                        switch (event.getMessage().getType()) {
                            case KEYAGREEMENTPART2: {
                                // New period started but neither Joining nor Leaving message has been received yet. Save data from KeyAgreementPart2 message for later use.
                                saveDataFromKeyAgreementPart2(event.getMessage());
                            }
                            break;

                            case JOINING: {
                                // New participant has been added. Start the re-keying phase.
                                participantData.updatePeriod();
                                if (participantData.getCurrentPeriod() == participantData.getNumOfPeriods()) {
                                    System.out.println("Last period");
                                } else if (participantData.getCurrentPeriod() > participantData.getNumOfPeriods()) {
                                    stopKeyAgreement();
                                }
                                saveDataFromJoiningMsg(event.getMessage());
                                participantData.evaluateSecretPolynomial();
                                participantData.encryptSubKeys();
                                //Send new KeyAgreementPart2 message.
                                sendKeyAgreementPart2();
                                state = State.PART2SENT;
                                processEvent(new Event(EventType.CHECK));

                            }
                            break;
                            case LEAVING: {
                                // The sender has left the group. Start the re-keying phase.
                                participantData.updatePeriod();
                                if (participantData.getCurrentPeriod() == participantData.getNumOfPeriods()) {
                                    System.out.println("Last period");
                                } else if (participantData.getCurrentPeriod() > participantData.getNumOfPeriods()) {
                                    stopKeyAgreement();
                                }
                                participantData.getParticipants().remove(event.getMessage().getSenderId());
                                sockets.getPorts().remove(event.getMessage().getSenderId());
                                participantData.evaluateSecretPolynomial();
                                participantData.encryptSubKeys();
                                //Send new KeyAgreementPart2 message.
                                sendKeyAgreementPart2();
                                state = State.PART2SENT;
                                processEvent(new Event(EventType.CHECK));
                            }
                        }
                    }
                    break;
                    case ADDPARTICIPANT: {
                        // Invite new participant, send to him AddParticipantMsg message.
                        sockets.getPorts().put(event.getParticipantId(), event.getPort());
                        sendAddParticipantMsg(event.getParticipantId());
                    }
                    break;
                    case LEAVE: {
                        // Send Leaving message to the other participants.
                        sendLeavingMsg();
                        stopKeyAgreement();
                    }
                }
            }
        }
    }


    private Message.Msg.KeyAgreementPart1.Builder preparePart1() {
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = Message.Msg.KeyAgreementPart1.newBuilder()
                .setPubKeyY(ByteString.copyFrom(participantData.getPublicKeyY().toByteArray()));
        for (int i = 0; i < participantData.getPublicPolynomial().size(); i++) {
            keyAgreementPart1.addPubPolynomial(ByteString.copyFrom(participantData.getPublicPolynomial().get(i).toByteArray()));
        }
        return keyAgreementPart1;
    }

    private Message.Msg.KeyAgreementPart2.Builder preparePart2() {
        Message.Msg.KeyAgreementPart2.Builder keyAgreementPart2 = Message.Msg.KeyAgreementPart2.newBuilder()
                .setPeriod(participantData.getCurrentPeriod())
                .setPubKeyR(ByteString.copyFrom(participantData.getPublicKeyR().toByteArray()));
        for (Map.Entry<Integer, BigInteger> entry : participantData.getEncryptedSubKeys().entrySet()) {
            Message.Msg.KeyAgreementPart2.EncryptedSubKey.Builder subKey = Message.Msg.KeyAgreementPart2.EncryptedSubKey.newBuilder();
            subKey.setId(entry.getKey());
            subKey.setSubKey(ByteString.copyFrom(entry.getValue().toByteArray()));
            keyAgreementPart2.addEncryptedSubKeys(subKey);
        }
        return keyAgreementPart2;
    }

    private Message.Msg.InitMsg.Builder prepareInitMsg(Map<Integer, Integer> idAndPortList) {
        Message.Msg.InitMsg.Builder init = Message.Msg.InitMsg.newBuilder()
                .setG(ByteString.copyFrom(participantData.getG().toByteArray()))
                .setP(ByteString.copyFrom(participantData.getP().toByteArray()))
                .setQ(ByteString.copyFrom(participantData.getQ().toByteArray()));
        for (Map.Entry<Integer, Integer> entry : idAndPortList.entrySet()) {
            Message.Msg.InitMsg.IdAndPort.Builder idAndPort = Message.Msg.InitMsg.IdAndPort.newBuilder()
                    .setId(entry.getKey())
                    .setPort(entry.getValue());
            init.addIdAndPort(idAndPort);
        }
        return init;
    }

    private void sendKeyAgreementPart1() {
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = preparePart1();
        sockets.sendMsgToEveryone(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.KEYAGREEMENTPART1)
                .setKeyAgreementPart1(keyAgreementPart1)
                .build());
    }

    private void sendKeyAgreementPart2() {
        Message.Msg.KeyAgreementPart2.Builder keyAgreementPart2 = preparePart2();
        sockets.sendMsgToEveryone(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.KEYAGREEMENTPART2)
                .setKeyAgreementPart2(keyAgreementPart2)
                .build());
    }

    private void sendInitMsg(Map<Integer, Integer> idAndPortList) {
        Message.Msg.InitMsg.Builder init = prepareInitMsg(idAndPortList);
        sockets.sendMsgToEveryone(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.INITMSG)
                .setInit(init)
                .build());
    }

    private void sendLeavingMsg() {
        sockets.sendMsgToEveryone(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.LEAVING)
                .build());
    }

    private void sendJoiningMsg(int port) {
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = preparePart1();
        Message.Msg.Joining.Builder joining = Message.Msg.Joining.newBuilder()
                .setPort(port)
                .setKeyAgreementPart1(keyAgreementPart1);
        sockets.sendMsgToEveryone(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.JOINING)
                .setJoining(joining)
                .build());
    }

    private void sendAddParticipantMsg(Integer id) {
        Map idsAndPorts = new HashMap();
        idsAndPorts.putAll(sockets.getPorts());
        idsAndPorts.put(participantData.getId(), sockets.getMyPort());
        Message.Msg.InitMsg.Builder init = prepareInitMsg(idsAndPorts);
        Message.Msg.AddParticipant.Builder addParticipant = Message.Msg.AddParticipant.newBuilder()
                .setNextPeriod(participantData.getCurrentPeriod() + 1)
                .setInit(init);
        for (Map.Entry<Integer, PublicData> participant : participantData.getParticipants().entrySet()) {
            Message.Msg.AddParticipant.PublicData.Builder publicData = Message.Msg.AddParticipant.PublicData.newBuilder()
                    .setId(participant.getKey())
                    .setPubKeyY(ByteString.copyFrom(participant.getValue().getPublicKeyY().toByteArray()));
            for (BigInteger coefficient : participant.getValue().getPublicPolynomial()) {
                publicData.addPubPolynomial(ByteString.copyFrom(coefficient.toByteArray()));
            }
            addParticipant.addPublicData(publicData);
        }
        Message.Msg.AddParticipant.PublicData.Builder pubData = Message.Msg.AddParticipant.PublicData.newBuilder()
                .setId(participantData.getId())
                .setPubKeyY(ByteString.copyFrom(participantData.getPublicKeyY().toByteArray()));
        for (BigInteger value : participantData.getPublicPolynomial()) {
            pubData.addPubPolynomial(ByteString.copyFrom(value.toByteArray()));
        }
        addParticipant.addPublicData(pubData);
        sockets.sendMsgTo(Message.Msg.newBuilder()
                .setSenderId(participantData.getId())
                .setType(Message.Msg.Type.ADDPARTICIPANT)
                .setAddParticipant(addParticipant)
                .build(), id);
    }


    private void saveDataFromKeyAgreementPart1(Message.Msg msg) {
        int id = msg.getSenderId();
        List<BigInteger> pubPolynomial = new ArrayList<BigInteger>();
        for (ByteString value : msg.getKeyAgreementPart1().getPubPolynomialList()) {
            pubPolynomial.add(new BigInteger(value.toByteArray()));
        }
        if (participantData.getParticipants().containsKey(id)) {
            participantData.getParticipants().get(id).setPublicKeyY(new BigInteger(msg.getKeyAgreementPart1().getPubKeyY().toByteArray()));
            participantData.getParticipants().get(id).setPublicPolynomial(pubPolynomial);
            participantData.getParticipants().get(id).setPart1received(true);
        } else {
            PublicData participant = new PublicData();
            participant.setPublicKeyY(new BigInteger(msg.getKeyAgreementPart1().getPubKeyY().toByteArray()));
            participant.setPublicPolynomial(pubPolynomial);
            participant.setPart1received(true);
            participantData.getParticipants().put(id, participant);
        }
    }


    private void saveDataFromKeyAgreementPart2(Message.Msg msg) {
        int id = msg.getSenderId();
        PublicData participant = participantData.getParticipants().get(id);
        participant.setPublicKeyR(new BigInteger(msg.getKeyAgreementPart2().getPubKeyR().toByteArray()));
        participant.setPeriod(msg.getKeyAgreementPart2().getPeriod());
        for (Message.Msg.KeyAgreementPart2.EncryptedSubKey subKey : msg.getKeyAgreementPart2().getEncryptedSubKeysList()) {
            if (subKey.getId() == this.participantData.getId()) {
                participant.setEncryptedSubKey(new BigInteger(subKey.getSubKey().toByteArray()));
            }
        }

    }

    private void saveDataFromJoiningMsg(Message.Msg msg) {
        int id = msg.getSenderId();
        PublicData participant = new PublicData();

        List<BigInteger> pubPolynomial = new ArrayList<BigInteger>();
        for (ByteString value : msg.getJoining().getKeyAgreementPart1().getPubPolynomialList()) {
            pubPolynomial.add(new BigInteger(value.toByteArray()));
        }
        participant.setPublicKeyY(new BigInteger(msg.getJoining().getKeyAgreementPart1().getPubKeyY().toByteArray()));
        participant.setPublicPolynomial(pubPolynomial);
        participant.setPart1received(true);
        participantData.getParticipants().put(id, participant);
        sockets.getPorts().put(id, msg.getJoining().getPort());
    }

    private void saveDataFromAddParticipantMsg(Message.Msg msg) {
        participantData.setP(new BigInteger(msg.getAddParticipant().getInit().getP().toByteArray()));
        participantData.setQ(new BigInteger(msg.getAddParticipant().getInit().getQ().toByteArray()));
        participantData.setG(new BigInteger(msg.getAddParticipant().getInit().getG().toByteArray()));
        participantData.setCurrentPeriod(msg.getAddParticipant().getNextPeriod());
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (Message.Msg.InitMsg.IdAndPort idAndPort : msg.getAddParticipant().getInit().getIdAndPortList()) {
            if (idAndPort.getId() != participantData.getId()) {
                map.put(idAndPort.getId(), idAndPort.getPort());
            }
        }
        sockets.setPorts(map);
        for (Message.Msg.AddParticipant.PublicData publicData : msg.getAddParticipant().getPublicDataList()) {
            List<BigInteger> pubPolynomial = new ArrayList<BigInteger>();
            for (ByteString value : publicData.getPubPolynomialList()) {
                pubPolynomial.add(new BigInteger(value.toByteArray()));
            }
            participantData.getParticipants().put(publicData.getId(), new PublicData(new BigInteger(publicData.getPubKeyY().toByteArray()), pubPolynomial));
        }
    }

    private void saveDataFromInitMsg(Message.Msg msg) {
        participantData.setG(new BigInteger(msg.getInit().getG().toByteArray()));
        participantData.setP(new BigInteger(msg.getInit().getP().toByteArray()));
        participantData.setQ(new BigInteger(msg.getInit().getQ().toByteArray()));
        Map<Integer, Integer> ports = new HashMap<Integer, Integer>();

        for (Message.Msg.InitMsg.IdAndPort participant : msg.getInit().getIdAndPortList()) {
            if (participant.getId() != participantData.getId()) {

                if (!participantData.getParticipants().containsKey(participant.getId())) {
                    participantData.getParticipants().put(participant.getId(), new PublicData());
                }
                ports.put(participant.getId(), participant.getPort());
            }
        }
        sockets.setPorts(ports);
    }

    /**
     * Stops key agreement protocol.
     */
    public void stopKeyAgreement() {
        state = State.INITIAL;
        participantData.getParticipants().clear();
        sockets.getPorts().clear();
    }

    public ParticipantData getParticipantData() {
        return participantData;
    }

    public Sockets getSockets() {
        return sockets;
    }
}
