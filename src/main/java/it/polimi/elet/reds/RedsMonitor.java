package it.polimi.elet.reds;

import it.polimi.elet.selflet.message.RedsAllMessageFilter;
import it.polimi.elet.selflet.message.RedsMessage;
import it.polimi.elet.selflet.message.SelfLetMsg;

import java.awt.Dimension;
import java.net.ConnectException;
import java.util.Date;
import javax.swing.JFrame;

import polimi.reds.DispatchingService;
import polimi.reds.Message;
import polimi.reds.TCPDispatchingService;

/**
 * @author Nicola Calcavecchia <calcavecchia@gmail.com>
 * */
public class RedsMonitor extends JFrame {

	private static final long serialVersionUID = 1L;

	private DispatchingService dispatchingService = null;
	private DispatcherThread dispThread = null;

	public static void main(String[] args) {

		if (args == null || args.length < 2) {
			System.err.println("Usage:\n\tit.polimi.elet.reds.RedsMonitor address port");
			System.exit(0);
		}

		String address = args[0];
		int port = new Integer(args[1]).intValue();

		RedsMonitor mon = new RedsMonitor();
		mon.start(address, port);

		System.out.println("Monitor started on " + address + ":" + port);
	}

	private void start(String address, int port) {
		initialize();
		connect(address, port);
	}

	private void initialize() {

		this.setSize(new Dimension(294, 73));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Reds Monitor");
		this.setVisible(true);
	}

	public void connect(String address, Integer port) {

		if (dispThread != null && dispThread.isAlive()) {
			dispThread.stopThread();
			dispThread = null;
		}

		dispatchingService = new TCPDispatchingService(address, port);

		try {
			dispatchingService.open();
		} catch (ConnectException e) {
		}

		// The filter does not block any message
		dispatchingService.subscribe(new RedsAllMessageFilter());

		// Start the dispatcher thread, which converts messages into events
		dispThread = new DispatcherThread(dispatchingService);
		dispThread.start();
	}

	public void disconnect() {

		dispatchingService.close();

		if (dispThread != null && dispThread.isAlive()) {
			dispThread.stopThread();
			dispThread = null;
		}
	}

	/* ********************** */
	/* Private thread classes */
	/* ********************** */

	/**
	 * An inner thread class, which monitors the received messages and produces
	 * events if a message of a bound type is received.
	 * 
	 */
	private class DispatcherThread extends Thread {

		private DispatchingService ds;
		private boolean stop;

		/**
		 * Constructs a DispatcherThread with the given parameters.
		 * 
		 * @param ds
		 *            the REDS dispatching service
		 * @param bindings
		 *            the bindings between message types and event types
		 */
		public DispatcherThread(DispatchingService ds) {

			this.ds = ds;
			this.stop = false;
			this.setDaemon(true);
		}

		/**
		 * The method which monitors the messages and produces events.
		 */
		public void run() {

			int count = 0;

			while (!stop) {

				while (ds.hasMoreReplies()) {
					RedsMessage rawMsg = (RedsMessage) ds.getNextReply();
					SelfLetMsg msg = rawMsg.getMessage();
					count++;
					Date now = new Date();
					System.out.println("[" + now + "] Reply #" + count + " from " + msg.getFrom() + " to " + msg.getTo() + " type " + msg.getType() + " param "
							+ msg.getContent());
				}

				while (ds.hasMoreMessages()) {
					Message rawMsg = ds.getNextMessage();
					if (rawMsg instanceof RedsMessage) {
//						RedsMessage redsMessage = (RedsMessage) rawMsg;
//						SelfLetMsg msg = redsMessage.getMessage();
						count++;
//						Date now = new Date();
						// System.out.println("[" + now + "] Msg #" + count
						// + " from " + msg.getFrom() + " to "
						// + msg.getTo() + " type " + msg.getType()
						// + " param " + msg.getContent());
					} else {
						System.out.println("Received " + rawMsg.getID());
					}
				}

				// Wait
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}

			}

		}

		/**
		 * Stops the thread by setting the stop parameter.
		 */
		public void stopThread() {

			this.stop = true;
		}
	}

}
