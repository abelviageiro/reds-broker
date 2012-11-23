package it.polimi.elet.reds;

import java.net.BindException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.LinkedHashSet;
import java.util.Set;

import polimi.reds.NodeDescriptor;
import java.util.logging.*;

import polimi.reds.broker.overlay.AlreadyAddedNeighborException;
import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.HashReplyTable;
import polimi.reds.broker.routing.ImmediateForwardReplyManager;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;

/**
 * Class implementing a REDS broker
 * 
 * @author Nicola Calcavecchia <calcavecchia@gmail.com>
 * */
public class RedsBroker {

	private Integer port;
	private Overlay overlay;
	private ConsoleHandler consoleLogHandler;
	private Logger logger;
	private String otherBroker;

	public RedsBroker(Integer port, String otherBroker) {
		this.port = port;
		this.otherBroker = otherBroker;
	}

	public RedsBroker(int port) {
		this.port = port;
	}

	/**
	 * This method starts the REDS broker on the local machine using the
	 * specified port.
	 * 
	 */
	public void start() throws BindException {
		Transport transport = new TCPTransport(port);
		Set<Transport> transports = new LinkedHashSet<Transport>();
		transports.add(transport);

		TopologyManager topolMgr = new SimpleTopologyManager();
		overlay = new GenericOverlay(topolMgr, transports);

		RoutingStrategy routingStrategy = new SubscriptionForwardingRoutingStrategy();
		Reconfigurator reconf = new DeferredUnsubscriptionReconfigurator();
		GenericRouter router = new GenericRouter(overlay);
		SubscriptionTable subscriptionTable = new GenericTable();
		routingStrategy.setOverlay(overlay);
		reconf.setOverlay(overlay);
		ReplyManager replyMgr = new ImmediateForwardReplyManager();
		ReplyTable replyTbl = new HashReplyTable();
		replyMgr.setOverlay(overlay);
		router.setOverlay(overlay);
		router.setSubscriptionTable(subscriptionTable);
		router.setRoutingStrategy(routingStrategy);
		router.setReplyManager(replyMgr);
		router.setReplyTable(replyTbl);
		reconf.setRouter(router);
		replyMgr.setReplyTable(replyTbl);
		overlay.start();
		System.out.println("overlay id: " + overlay.getID());

		consoleLogHandler = new ConsoleHandler();
		logger = Logger.getLogger("polimi.reds");
		logger.addHandler(consoleLogHandler);
		setConsoleLevel(Level.SEVERE);

		logger.log(Level.INFO, "[REDS] Broker started at port " + port);

		addOtherBroker();

		String[] urls = overlay.getURLs();
		String totalString = "";
		for (String string : urls) {
			if (string == null) {
				continue;
			}
			totalString += (string + ", ");
		}

		logger.log(Level.INFO, totalString);

	}

	private void addOtherBroker() {
		if (otherBroker == null) {
			return;
		}

		try {
			overlay.addNeighbor(otherBroker);
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (AlreadyAddedNeighborException e) {
			e.printStackTrace();
		}
	}

	public void setConsoleLevel(Level level) {
		consoleLogHandler.setLevel(level);
	}

	/**
	 * This method stops the REDS broker on the local machine.
	 */
	public void stop() {

		while (overlay.getNeighbors().size() > 0) {
			overlay.removeNeighbor((NodeDescriptor) overlay.getNeighbors().toArray()[0]);
		}

		overlay.stop();
	}

}
