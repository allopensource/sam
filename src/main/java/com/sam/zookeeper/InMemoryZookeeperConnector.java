package com.sam.zookeeper;

import java.io.File;
import java.io.IOException;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "saga-server.inMemory", havingValue = "true")
public class InMemoryZookeeperConnector extends AbstractZookeeperConnector {
	private Logger logger = LogManager.getLogger(InMemoryZookeeperConnector.class);

	private static ServerCnxnFactory cnxnFactory = null;
	private static final int port = 2181;
	private static final String host = "localhost";
	private static final String connectString = host + ":" + port;
	private static final int tickTime = 2000;
	private static final int numConnections = 10;

	public void startLocalServer() {
		try {
			logger.info("Starting local zookeeper at :: {}", connectString);
			String dataDirectory = System.getProperty("java.io.tmpdir");
			File dir = new File(dataDirectory, "zookeeper").getAbsoluteFile();
			ZooKeeperServer server = new ZooKeeperServer(dir, dir, tickTime);
			cnxnFactory = ServerCnxnFactory.createFactory(port, numConnections);
			cnxnFactory.startup(server);
			logger.info("Zookeeper started");
		} catch (IOException | InterruptedException e) {
			logger.error("Zookeeper startup failed :: ", e);
		}
	}

	@PreDestroy
	public void stopLocalServer() {
		logger.info("PreDestroy :: Shutting down zookeeper!!");
		cnxnFactory.shutdown();
	}

	@Override
	public String getConnectString() {
		return connectString;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		this.startLocalServer();
		super.onApplicationEvent(event);
	}
}