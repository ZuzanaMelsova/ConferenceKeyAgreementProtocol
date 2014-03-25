
import com.google.protobuf.ByteString;

import java.math.BigInteger;
import java.util.*;

public class StateClass {
    private long period;
    private State state;
    private Timer timer;
    private boolean initiator;
    private ParticipantInfo participantInfo;
    private Sockets sockets;


    public Sockets getSockets() {
        return sockets;
    }

    public StateClass(State state, ParticipantInfo participantInfo, Sockets sockets) {
        this.state = state;
        this.participantInfo = participantInfo;
        this.sockets = sockets;
        this.sockets.setMessageListener(new MessageListener() {
            @Override
            public void MessageReceived(Message.Msg message) {
                processEvent(new Event(EventType.MESSAGE, message));
            }
        });

        this.initiator = false;
        period = 5 * 60 * 1000;
    }

    public void processEvent(Event event) {
        switch (state) {
            case INITIAL: {
                switch (event.getType()) {
                    case STARTKEYAGREEMENT: {
                        initiator = true;
                        // generate primes ...
                        //  sendInitMsg();
                        participantInfo.generatePublicKeyY();
                        participantInfo.computePublicValuesGeneratedFromCoefficients();
                        sendKeyAgreementPart1();
                        state = State.PART1SENT;
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                processEvent(new Event(EventType.TIMER));
                            }
                        }, period, period);
                    }
                    break;
                    case MESSAGE: {
                        Message.Msg.Type msgType = event.getMessage().getType();
                        if (msgType.equals(Message.Msg.Type.INITMSG)) {
                            saveDataFromInitMsg(event.getMessage());
                            participantInfo.generatePublicKeyY();
                            participantInfo.computePublicValuesGeneratedFromCoefficients();
                            sendKeyAgreementPart1();
                            state = State.PART1SENT;
                        }
                        if (msgType.equals(Message.Msg.Type.KEYAGREEMENTPART1)) {
                            saveDataFromKeyAgreementPart1(event.getMessage());
                        }
                        if (msgType.equals(Message.Msg.Type.ADDPARTICIPANT)) {
                            saveDataFromAddParticipantMsg(event.getMessage());
                            participantInfo.generatePublicKeyY();
                            participantInfo.computePublicValuesGeneratedFromCoefficients();
                            participantInfo.computeEvaluatedPrivatePolynomial();
                            participantInfo.computeEncryptedSubKeys();
                            sendJoiningMsg();
                            state = State.PART2SENT;
                        }
                    }
                    break;
              /*
               case ADDPARTICIPANT:
                   break;
               case LEAVE:
                   break;
               */
                }
            }

            case PART1SENT: {
                if (event.getType().equals(EventType.MESSAGE)) {
                    Message.Msg.Type msgType = event.getMessage().getType();
                    switch (msgType) {
                        case KEYAGREEMENTPART1: {
                            saveDataFromKeyAgreementPart1(event.getMessage());
                            //check if received part1 from all participants{
                            participantInfo.computeEvaluatedPrivatePolynomial();
                            participantInfo.computeEncryptedSubKeys();
                            sendKeyAgreementPart2();
                            //}
                        }
                        break;
                        case KEYAGREEMENTPART2: {
                            saveDataFromKeyAgreementPart2(event.getMessage());
                        }
                        break;
                    }
                }
            }
            break;
            case PART2SENT: {
                if (event.getType().equals(EventType.MESSAGE)) {
                    if (event.getMessage().getType().equals(Message.Msg.Type.KEYAGREEMENTPART2)) {
                        saveDataFromKeyAgreementPart2(event.getMessage());
                        //check if received part2 from all participants{
                        participantInfo.computeDecryptedSubKeys();
                        if (participantInfo.verifySubKeys()) {
                            //participantInfo.computeCommonSecretKey()
                        } else {//stopKeyAgreement(); state = State.INITIAL;
                        }
                        state = State.WAITFORNEWPERIOD;
                        // }
                    }
                }
            }
            break;
            case WAITFORNEWPERIOD: {
                switch (event.getType()) {

                    case MESSAGE: {
                        switch (event.getMessage().getType()) {
                            case KEYAGREEMENTPART2: {
                                saveDataFromKeyAgreementPart2(event.getMessage());

                            }
                            break;
                            case JOINING: {
                                participantInfo.addParticipant(event.getMessage().getSenderId());
                                saveDataFromJoiningMsg(event.getMessage());
                            }
                            break;
                            case LEAVING: {
                                participantInfo.removeParticipant(event.getMessage().getSenderId());
                            }
                            break;
                            case NEWPERIOD:
                                break;
                        }
                    }
                    break;
                    case TIMER: {
                        sendNewPeriodMsg();
                        participantInfo.updatePeriod();
                        participantInfo.computeEvaluatedPrivatePolynomial();
                        participantInfo.computeEncryptedSubKeys();
                        sendKeyAgreementPart2();
                        state = State.PART2SENT;
                    }
                    break;
                    case ADDPARTICIPANT: {
                        sendAddParticipantMsg();
                    }
                    break;
                    case LEAVE: {
                        sendLeavingMsg();

                    }
                    break;
                }
            }
            break;
        }
    }


    public void sendKeyAgreementPart1() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.KEYAGREEMENTPART1);
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = preparePart1();
        msg.setKeyAgreementPart1(keyAgreementPart1);
        msg.build();
        //sockets.sendMsg(msg);
    }

    public void sendKeyAgreementPart2() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.KEYAGREEMENTPART2);
        Message.Msg.KeyAgreementPart2.Builder keyAgreementPart2 = preparePart2();
        msg.setKeyAgreementPart2(keyAgreementPart2);
        msg.build();
        //sockets.sendMsg(msg);
    }

    public Message.Msg.KeyAgreementPart1.Builder preparePart1() {
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = Message.Msg.KeyAgreementPart1.newBuilder()
                .setPubKeyY(ByteString.copyFrom(participantInfo.getPublicKeyY().toByteArray()));
        for (int i = 0; i < participantInfo.getPublicValuesGeneratedFromCoefficients().size(); i++) {
            keyAgreementPart1.addPubValuesGeneratedFromCoefficients(ByteString.copyFrom(participantInfo.getPublicValuesGeneratedFromCoefficients().get(i).toByteArray()));
        }
        return keyAgreementPart1;
    }

    public Message.Msg.KeyAgreementPart2.Builder preparePart2() {
        Message.Msg.KeyAgreementPart2.Builder keyAgreementPart2 = Message.Msg.KeyAgreementPart2.newBuilder()
                .setPubKeyR(ByteString.copyFrom(participantInfo.getPublicKeyR().toByteArray()));
        for (Map.Entry<Integer, BigInteger> entry : participantInfo.getEncryptedSubKeys().entrySet()) {
            Message.Msg.KeyAgreementPart2.EncryptedSubKey.Builder subKey = Message.Msg.KeyAgreementPart2.EncryptedSubKey.newBuilder();
            subKey.setId(entry.getKey());
            subKey.setSubKey(ByteString.copyFrom(entry.getValue().toByteArray()));
            keyAgreementPart2.addEncryptedSubKeys(subKey);
        }
        return keyAgreementPart2;
    }


    public void sendInitMsg(List<Integer> idList) {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.INITMSG);
        Message.Msg.InitMsg.Builder init = Message.Msg.InitMsg.newBuilder()
                .setG(ByteString.copyFrom(participantInfo.getG().toByteArray()))
                .setP(ByteString.copyFrom(participantInfo.getP().toByteArray()))
                .setQ(ByteString.copyFrom(participantInfo.getQ().toByteArray()))
                .addAllId(idList);
        msg.setInit(init);
        msg.build();
    }

    public void sendLeavingMsg() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.LEAVING);
       // sockets.sendMsg(msg);
    }

    public void sendJoiningMsg() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.JOINING);
        Message.Msg.KeyAgreementPart2.Builder keyAgreementPart2 = preparePart2();
        msg.setKeyAgreementPart2(keyAgreementPart2);
        Message.Msg.KeyAgreementPart1.Builder keyAgreementPart1 = preparePart1();
        msg.setKeyAgreementPart1(keyAgreementPart1);
        msg.build();
        //sockets.sendMsg(msg);
    }

    public void sendAddParticipantMsg() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.ADDPARTICIPANT);
        Message.Msg.AddParticipant.Builder addParticipant = Message.Msg.AddParticipant.newBuilder()
                .setNextPeriod(participantInfo.getCurrentPeriod() + 1)
                .setG(ByteString.copyFrom(participantInfo.getG().toByteArray()))
                .setP(ByteString.copyFrom(participantInfo.getP().toByteArray()))
                .setQ(ByteString.copyFrom(participantInfo.getQ().toByteArray()));

        for (PublicData participant : participantInfo.getPublicDataList()) {
            Message.Msg.AddParticipant.PublicData.Builder publicData = Message.Msg.AddParticipant.PublicData.newBuilder()
                    .setId(participant.getId())
                    .setPubKeyY(ByteString.copyFrom(participant.getPublicKeyY().toByteArray()));
            for (BigInteger value : participant.getPublicValuesGeneratedFromCoefficients()) {
                publicData.addPubValuesGeneratedFromCoefficients(ByteString.copyFrom(participant.getPublicKeyY().toByteArray()));
            }
            addParticipant.addPublicData(publicData);
            msg.setAddParticipant(addParticipant);
        }
        msg.build();
        //sendToNewParticipant

    }

    public void sendNewPeriodMsg() {
        Message.Msg.Builder msg = Message.Msg.newBuilder()
                .setSenderId(participantInfo.getId())
                .setType(Message.Msg.Type.NEWPERIOD);
       // sockets.sendMsg(msg);
    }

    public void saveDataFromKeyAgreementPart1(Message.Msg msg) {
        int id = msg.getSenderId();
        PublicData participant = null;
        for (PublicData participantData : participantInfo.getPublicDataList()) {
            if (participantData.getId() == id) {
                participant = participantData;
                break;
            } else participant = new PublicData(id);
        }
        participant.setPublicKeyY(new BigInteger(msg.getKeyAgreementPart1().getPubKeyY().toByteArray()));

        List<BigInteger> valuesGeneratedFromCoefficients = null;
        for (ByteString value : msg.getKeyAgreementPart1().getPubValuesGeneratedFromCoefficientsList()) {
            valuesGeneratedFromCoefficients.add(new BigInteger(value.toByteArray()));
        }
        participant.setPublicValuesGeneratedFromCoefficients(valuesGeneratedFromCoefficients);
    }


    public void saveDataFromKeyAgreementPart2(Message.Msg msg) {
        int id = msg.getSenderId();
        PublicData participant = null;
        for (PublicData participantData : participantInfo.getPublicDataList()) {
            if (participantData.getId() == id) {
                participant = participantData;
                break;
            } else participant = new PublicData(id);
        }
        participant.setPublicKeyR(new BigInteger(msg.getKeyAgreementPart2().getPubKeyR().toByteArray()));
        for (Message.Msg.KeyAgreementPart2.EncryptedSubKey subKey : msg.getKeyAgreementPart2().getEncryptedSubKeysList()) {
            if (subKey.getId() == id) {
                participant.setEncryptedSubKey(new BigInteger(subKey.getSubKey().toByteArray()));
            }
        }

    }

    public void saveDataFromJoiningMsg(Message.Msg msg) {


    }

    public void saveDataFromAddParticipantMsg(Message.Msg msg) {

    }

    public void saveDataFromInitMsg(Message.Msg msg) {

    }
}
