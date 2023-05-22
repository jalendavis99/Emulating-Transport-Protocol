/*************************************
 * Filename:  Sender.java
 * Student-ID:
 * Date:
 *************************************/
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class Sender extends NetworkHost

{
    /*
     * Predefined Constant (static member variables):
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
     *  Message: Used to encapsulate the message coming from application layer
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
    // state information for the sender. 

    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!


    private int wSize;//used to store window size
    private int base;
    private int nextSN;// used to store next sequence number
    private LinkedList<Message> bufferList = new LinkedList<Message>(); //To store message which is waiting for sending because window does not have usable sequence number.
    private ArrayList<Packet> packetSentList = new ArrayList<Packet>(); //To store packet which has been sent.
    public Sender(int entityName,
                       EventList events,
                       double pLoss,
                       double pCorrupt,
                       int trace,
                       Random random)
    {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }

    // This routine will be called whenever the application layer at the sender
    // has a message to  send.  The job of your protocol is to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving application layer.
    protected void Output(Message message)
    {
        if (bufferList.size() <= 50) {
            if (nextSN < base + wSize) {
                //window has the usable sequence number, send the message.
                int seqNumber = nextSN;
                int ackNumber = 0;
                int checksum = 0xFF - onesCSum(message.getData(), seqNumber, ackNumber);
                Packet packetToSend = new Packet(seqNumber, ackNumber, checksum, message.getData());

                udtSend(packetToSend);
                //store the packet which has been sent to resend later if needed.
                packetSentList.add(packetToSend);
                if (base == nextSN) {
                    startTimer(40);
                }
                nextSN++;
            } else {
                //window does not have usable sequence number, store the message in buffer to send later.
                bufferList.add(message);
                System.out.println("buffer: no available sequence number, new message stored in buffer");
            }
        } else {
            //buffer has had 50 messages waiting for sending, so it refuses to send message.
            System.out.print("refuse send message!!");
        }
    }
    
    // This routine will be called whenever a packet sent from the receiver 
    // (i.e. as a result of udtSend() being done by a receiver procedure)
    // arrives at the sender.  "packet" is the (possibly corrupted) packet
    // that was sent from the receiver.
    protected void Input(Packet packet)
    {




        if (isCorrect(packet) && base <= packet.getAcknum()) {
            base = packet.getAcknum() + 1;
            if (base == nextSN) {
                stopTimer();
            } else {
                stopTimer();
                startTimer(40);
            }
        } else {
            System.out.println("input: Discard invalid packet");
        }

        //After receiving a packet, if window has usable sequence number and there are some messages in buffer, send them.
        while (bufferList.size() != 0 && nextSN < base + wSize) {
            Output(bufferList.poll());
        }
    }
    
    // This routine will be called when the senders's timer expires (thus 
    // generating a timer interrupt). You'll probably want to use this routine 
    // to control the retransmission of packets. See startTimer() and 
    // stopTimer(), above, for how the timer is started and stopped. 
    protected void TimerInterrupt()
    {
        startTimer(40);
        for (int i = base; i < nextSN; i++) {
            udtSend(packetSentList.get(i));
        }
    }
    
    // This routine will be called once, before any of your other sender-side 
    // routines are called. The method should be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the sender).
    protected void Init()
    {

        //set the window size, base, nextSequenceNumber
        wSize = 8;
        base = 0;
        nextSN = 0;
    }


    private static int onesCSum(String data, int sequence, int ack) {
        String sequenceString = Integer.toString(sequence);
        String ackString = Integer.toString(ack);
        String content = ackString + sequenceString + data;

        int onesComplementSum = 0;

        //value is used to show the adding result in the process of adding
        String value;

        //add the ack, sequence, data in 8-bit
        for (int i = 0; i < content.length(); i++) {
            onesComplementSum = onesComplementSum + (int) content.charAt(i);
            value = Integer.toHexString(onesComplementSum);

            //if carryout occurs, add the most significant bit needs to be added to the result
            if (value.length() > 2) {
                int carry = Integer.parseInt("" + value.charAt(0), 16);
                value = value.substring(1, 3);
                onesComplementSum = Integer.parseInt(value, 16);
                onesComplementSum += carry;
            }
        }

        return onesComplementSum;
    }

    private static boolean isCorrect(Packet packet) {
        //get the checksum of receiver's packet
        int recChecksum = packet.getChecksum();

        //get the ones complement sum of the packet received
        int onesComplementSum = onesCSum(packet.getPayload(), packet.getSeqnum(), packet.getAcknum());

        //check if their sum equals to 11111111
        return recChecksum + onesComplementSum == 0xFF;
    }


}
