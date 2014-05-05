import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Zuzana Melsova
 * Entry point for the key agreement protocol application.
 * Creates an instance of KeyAgreement and processes comand line comands.
 */
public class KeyAgreementApp {
    static Logger LOGGER = LoggerFactory.getLogger(KeyAgreementApp.class);
    private static int NUMBER_OF_PERIODS = 5;

    @Option(name = "-id", usage = "Participant id")
    private int id;

    @Option(name = "-port", usage = "Participant port")
    private int port;

    @Option(name = "-logFile", usage = "Output file for logger")
    private String logFile;


    public static void main(String[] args) {
        new KeyAgreementApp().doMain(args);
    }

    private static void updateLog4jConfiguration(String logFile) {
        Properties props = new Properties();
        try {
            InputStream configStream = KeyAgreementApp.class.getResourceAsStream("/log4j.properties");
            props.load(configStream);
            configStream.close();
        } catch (IOException e) {
            System.out.println("Error: Cannot load configuration file ");
        }
        props.setProperty("log4j.appender.file.File", logFile);
        LogManager.resetConfiguration();
        PropertyConfigurator.configure(props);
    }

    public void doMain(String[] args) {

        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());

            // Print list of available options
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        // Set logging output
        updateLog4jConfiguration(logFile);

        // Create KeyAgreement instance
        KeyAgreement keyAgreement = new KeyAgreement(id, port);
        keyAgreement.getStateEngine().getParticipantData().setNumOfPeriods(NUMBER_OF_PERIODS);

        // Process command line commands
        Scanner sc = new Scanner(System.in);
        while (true) {
            String line = sc.nextLine().trim();

            if (line.equals("exit")) {
                keyAgreement.getStateEngine().getSockets().stop();
                return;
            }

            if (line.equals("init")) {
                Map<Integer, Integer> map = new HashMap<Integer, Integer>();
                System.out.println("Type in <id port> of selected users:");
                while (!(line = sc.nextLine()).equals("start")) {
                    String[] idAndPort = line.split(" ");
                    if (idAndPort.length != 2) {
                        System.err.println("Error: Invalid arguments");
                    }
                    try {
                        map.put(Integer.parseInt(idAndPort[0]), Integer.parseInt(idAndPort[1]));
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid arguments:" + e);
                    }
                }
                keyAgreement.startKeyAgreement(map);
                continue;
            }

            if (line.equals("leave")) {
                keyAgreement.leaveConference();
                continue;
            }

            if (line.equals("add")) {
                System.out.println("Type in <id  port> of the new participant:");
                String[] idAndPort = sc.nextLine().split(" ");
                if (idAndPort.length != 2) {
                    System.err.println("Error: Invalid arguments");
                }
                try {
                    keyAgreement.addParticipant(Integer.parseInt(idAndPort[0]), Integer.parseInt(idAndPort[1]));
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid arguments:" + e);
                }
                continue;
            }
        }
    }
}
