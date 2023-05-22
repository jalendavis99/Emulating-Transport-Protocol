/*************************************
 * Filename:  Receiver.java
 * Names:
 * Student-IDs:
 * Date:
 *************************************/
import java.util.Random;

public class Receiver extends NetworkHost

{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *
     * Predefined Member Methods:
     *
     *  void startTimer(double increment):
     *       Starts a timer, which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this in the Sender class.
     *  void stopTimer():
     *       Stops the timer. You should only call this in the Sender class.
     *  void udtSend(Packet p)
     *       Sends the packet "p" into the network to arrive at other host
     *  void deliverData(String dataSent)
     *       Passes "dataSent" up to application layer. Only call this in the
     *       Receiver class.
     *  double getTime()
     *       Returns the current time of the simulator.  Might be useful for
     *       debugging.
     *  String getReceivedData()
     *       Returns a String with all data delivered to receiving process.
     *       Might be useful for debugging. You should only call this in the
     *       Sender class.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from application layer
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet, which is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here. They can hold
    // state information for the receiver.
    
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!


    // Add any necessary class variables here. They can hold


    // state information for the receiver.
    private int receiverSN;
    private int receiverAckN;
    private int expectedSN;
    private String receiverPL; //receiverPayload

    public Receiver(int entityName,
                       EventList events,
                       double pLoss,
                       double pCorrupt,
                       int trace,
                       Random random)
    {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }

    
    // This routine will be called whenever a packet from the sender
    // (i.e. as a result of a udtSend() being done by a Sender procedure)
    // arrives at the receiver. Argument "packet" is the (possibly corrupted)
    // packet that was sent from the sender.
    protected void Input(Packet packet)
    {

        int receiverChecksum;
        String senderPL; //sender payload

        Packet ackPacket;

        if (haveCorrectChecksum(packet) && isCorrectSeqNum(packet)) { // Check whether or not the packet have correct checksum and sequnce number
            //Packet is correct, set ACK number and increase expectedSeqNum
            senderPL = packet.getPayload();
            deliverData(senderPL);
            receiverAckN = packet.getSeqnum();
            expectedSN++;
        } else {
            //Packet is incorrect, print error message
            System.out.println("input: Receive Packet incorrect");
        }
        //Generate a ACK packet to acknowledge the packet from sender.
        receiverChecksum = 0XFF - onesComplementSum(receiverPL, receiverSN, receiverAckN);
        ackPacket = new Packet(receiverSN, receiverAckN, receiverChecksum, receiverPL);
        udtSend(ackPacket);

    }
    

    
    // This routine will be called once, before any of your other receiver-side
    // routines are called. The method should be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the receiver).
    protected void Init()
    {

        expectedSN = 0;
        receiverSN = 0;
        receiverAckN = -1;
        receiverPL = "";
    }



    private boolean haveCorrectChecksum(Packet packet) {
        int recChecksum = packet.getChecksum();//get sender's checksum in the packet
        int onesComplementSum = onesComplementSum(packet.getPayload(), packet.getAcknum(), packet.getSeqnum());// generate the one's complement sum by the information in packet.
        return recChecksum + onesComplementSum == 0xFF;//check whether or not the sum of checksum from the sender and the one's complement sum is 11111111.
    }

    //Check whether or not the datagram from sender have correct sequence number.

    private boolean isCorrectSeqNum(Packet packet) {
        int senderSeqNum = packet.getSeqnum();
        //true if Sequence number is correct
        return expectedSN == senderSeqNum;
    }

    //Generate one's complement sum by the data, sequence number and ACK number.
    //and return this generated one's complement sum
    private static int onesComplementSum(String data, int sequence, int ack) {
        String sequenceString = Integer.toString(sequence);
        String ackString = Integer.toString(ack);
        String content = ackString + sequenceString + data;

        int onesComplementSum = 0;

        String value;

        //add the ack, sequence, data in 8-bit
        for (int i = 0; i < content.length(); i++) {
            onesComplementSum = onesComplementSum + (int) content.charAt(i);
            value = Integer.toHexString(onesComplementSum);

            //if there is carryout
            if (value.length() > 2) {
                int carry = Integer.parseInt("" + value.charAt(0), 16);
                value = value.substring(1, 3);
                onesComplementSum = Integer.parseInt(value, 16);
                onesComplementSum += carry;
            }
        }

        return onesComplementSum;
    }

}
