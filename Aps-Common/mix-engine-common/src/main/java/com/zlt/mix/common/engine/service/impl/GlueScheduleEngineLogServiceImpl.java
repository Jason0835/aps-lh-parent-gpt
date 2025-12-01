package com.zlt.mix.common.engine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.engine.domain.MessageContent;
import com.zlt.mix.common.engine.service.AutoScheduleLogService;
import com.zlt.mix.common.engine.service.GlueScheduleEngineLogService;

import lombok.extern.slf4j.Slf4j;

/**
 * 终炼母炼胶排产日志服务
 * 
 * @author hakimryan
 *
 */
@Service
@Slf4j
public class GlueScheduleEngineLogServiceImpl implements GlueScheduleEngineLogService {
	@Autowired
	private AutoScheduleLogService autoScheduleLogService;
	/**
	 * 日志内容，绑定线程变量
	 */
	private ThreadLocal<MessageContent> logDetail = new ThreadLocal<>();

	/**
	 * 记录日志
	 * 
	 * @param logMessage 日志信息
	 */
	@Override
	public void record(String logMessage) {
		try {
			if (StringUtils.isEmpty(logMessage)) {
				return;
			}
			MessageContent logContent = this.getMessageContent();
			log.info(logMessage);
			logContent.addMessage(logMessage);
		} catch (Exception e) { // 日志记录错误不要影响排产，仅记录错误信息
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 保存日志
	 * 
	 * @param batchNo 批次号
	 * @param title   日志标题
	 */
	@Override
	public void save(String batchNo, String title) {
		try {
			MessageContent logContent = this.getMessageContent();
			if (!logContent.hasContent()) {
				return;
			}
			autoScheduleLogService.insertGlueScheduleLog(batchNo, null, title, logContent.toString());
			logContent.clean();
		} catch (Exception e) { // 日志记录错误不要影响排产，仅记录错误信息
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 获取消息正文，没有则创建一个
	 * 
	 * @return
	 */
	private MessageContent getMessageContent() {
		MessageContent logContent = logDetail.get();
		if (logContent == null) {
			logContent = MessageContent.newInstance();
			logDetail.set(logContent);
		}
		return logContent;
	}
}
