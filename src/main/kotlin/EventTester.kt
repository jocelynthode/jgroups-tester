import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import org.apache.logging.log4j.LogManager
import org.jgroups.*
import java.util.*

/**
 * This Class connects to a cluster, waits for other peers to join and then sends a number of events
 *
 * This implementation uses the SEQUENCER Channel. This uses Total Order and UDP
 *
 * @author Jocelyn Thode
 */
class EventTester(val MAX_EVENTS_SENT: Int, val peerNumber: Int) : ReceiverAdapter() {

    val logger = LogManager.getLogger(this.javaClass)!!

    var channel = JChannel("sequencer.xml")
    val TOTAL_MESSAGES = peerNumber * MAX_EVENTS_SENT
    var deliveredMessages = 0

    fun start() {
        channel.receiver = this
        channel.connect("EventCluster")
        var eventsSent = 0
        logger.info(channel.address.toString())

        //Start test when everyone is here
        while (channel.view.size() < peerNumber) {
            Thread.sleep(10)
        }
        logger.info("View size: ${channel.view.size()}")
        while (eventsSent != MAX_EVENTS_SENT) {
            val msg = Message(null, null, "${UUID.randomUUID()}")
            channel.send(msg)
            eventsSent++
            logger.info("Sending: ${msg.`object`}")
            Thread.sleep(1000)
        }
    }

    override fun viewAccepted(newView: View) {
        logger.debug("** size: ${newView.size()} ** view: $newView")
    }

    override fun receive(msg: Message) {
        logger.info("Delivered: ${msg.`object`}")
        deliveredMessages++
        if (deliveredMessages >= TOTAL_MESSAGES) {
            logger.info("All events delivered !")
            System.exit(0)
        }

    }
}

fun main(args: Array<String>) {
    val parser = ArgumentParsers.newArgumentParser("EpTO tester")
    parser.defaultHelp(true)
    parser.addArgument("peerNumber").help("Peer number")
            .type(Integer.TYPE)
            .setDefault(35)
    parser.addArgument("-e", "--events").help("Number of events to send")
            .type(Integer.TYPE)
            .setDefault(12)

    try {
        val res = parser.parseArgs(args)
        println("pn: ${res.getInt("peerNumber")}")
        val eventTester = EventTester(res.getInt("events"), res.getInt("peerNumber"))
        eventTester.start()
        while (true) Thread.sleep(500)
    } catch (e: ArgumentParserException) {
        parser.handleError(e)
    }

}
