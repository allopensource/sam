package com.sam.zookeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.apache.zookeeper.ZooKeeper;

public abstract class AbstractZookeeperConnector
		implements ZooKeeperConnector, ApplicationListener<ApplicationStartedEvent> {
	private final CountDownLatch connectedSignal = new CountDownLatch(1);
	private Logger logger = LogManager.getLogger(AbstractZookeeperConnector.class);
	private ZooKeeper zoo = null;
	public ApplicationContext applicationContext;

	public ZooKeeper getZookeeper() {
		return zoo;
	}

	public abstract String getConnectString();

	private void connect(String host) throws InterruptedException, IOException {
		logger.info("Connecting zookeeper at {}", host);
		zoo = new ZooKeeper(host, 5000, new Watcher() {
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});
		connectedSignal.await(5000, TimeUnit.MILLISECONDS);
		if (connectedSignal.getCount() == 0l) {
			logger.info("Connected");
		} else {
			throw new IllegalStateException("Zookeeper failed to conncect within 5000ms");
		}
	}

	// Method to disconnect from zookeeper server
	private void close() {
		if (zoo != null)
			try {
				zoo.close();
			} catch (Exception e) {
				logger.error("Thread interrupted while closing connection !! Unkown Connection Status !!");
			}
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		applicationContext = event.getApplicationContext();
		try {
			this.connect(getConnectString());
		} catch (Exception e) {
			logger.error("Connection to zookeeper failed !!. Enable root level logs to see error.");
			logger.debug(e);
			this.close();
			System.exit(0);
		}
	}

}