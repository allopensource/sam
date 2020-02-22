package com.sam.sec.tasks;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sam.helper.TxStatusEnums;
import com.sam.sec.service.TaskService;

@Service
@ConditionalOnProperty(name = "saga-server.recovery.enabled", havingValue = "true")
public class RecoveryTask {
	private Logger logger = LogManager.getLogger(RecoveryTask.class);
	private String name = "recovery-task";
	@Value("${saga-server.recovery.age}")
	private Long txAge;
	@Autowired
	private TaskService taskService;

	@Scheduled(fixedDelayString = "${saga-server.recovery.pollingInterval}", initialDelay = 10000)
	public void performTask() {
		Date taskTime = new Date();
		logger.info("Executing task {} at {}", name, taskTime);
		try {
			taskService.recover(txAge, taskTime, TxStatusEnums.Root.STARTED, TxStatusEnums.Root.REPROCESSSTARTED,
					TxStatusEnums.Root.ROLLBACKSTARTED);
		} catch (Exception exception) {
			logger.error("Exception occuured in performing task {}", name, exception);
		}
	}
}
