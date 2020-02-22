package com.sam.sec.tasks;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sam.helper.TxStatusEnums;
import com.sam.sec.service.TaskService;

@Service
@ConditionalOnProperty(name = "saga-server.cleanup.enabled", havingValue = "true")
public class CleanUpTask {
	private Logger logger = LogManager.getLogger(CleanUpTask.class);
	private String name = "cleanup-task";
	@Autowired
	private TaskService taskService;

	@Scheduled(fixedDelayString = "${saga-server.cleanup.pollingInterval}", initialDelay = 10000)
	public void performTask() {
		Date taskTime = new Date();
		logger.info("Executing task {} at {}", name, taskTime);
		try {
			taskService.cleanUp(TxStatusEnums.Root.FAILED, TxStatusEnums.Root.ROLLBACKED, TxStatusEnums.Root.SUCCESS);
		} catch (Exception exception) {
			logger.error("Exception occuured in performing task {}", name, exception);
		}
	}
}
