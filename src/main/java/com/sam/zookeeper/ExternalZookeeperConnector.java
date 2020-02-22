package com.sam.zookeeper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "saga-server.inMemory", havingValue = "false")
public class ExternalZookeeperConnector extends AbstractZookeeperConnector {

	@Override
	public String getConnectString() {
		return super.applicationContext.getEnvironment().getProperty("zookeeper.address");
	}
}