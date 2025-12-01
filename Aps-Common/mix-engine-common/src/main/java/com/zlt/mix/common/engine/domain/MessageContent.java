package com.zlt.mix.common.engine.domain;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;

/**
 * 消息体
 * 
 * @author hakimryan
 *
 */
public class MessageContent {
	/**
	 * 消息正文
	 */
	private StringBuffer content;

	private MessageContent() {
		this.content = new StringBuffer();
	}

	public static MessageContent newInstance() {
		return new MessageContent();
	}

	/**
	 * 添加消息到消息正文中
	 * 
	 * @param messageContent 消息正文
	 */
	public void addMessage(String message) {
		if (StringUtils.isEmpty(message)) {
			return;
		}
		if (this.content.length() > 0) {
			this.content.append("\n");
		}
		this.content.append(message);
	}

	/**
	 * 添加国际化消息到消息正文中
	 * 
	 * @param messageContent 消息正文
	 * @param messageKey     待添加消息的国际化key
	 * @param params         格式化参数
	 */
	public void addMessage(String messageKey, Object... params) {
		if (StringUtils.isEmpty(messageKey)) {
			return;
		}
		this.addMessage(getI18nMessage(messageKey, params));
	}

	/**
	 * 获取国际化信息，并通过格式化将参数设置到消息中
	 * 
	 * @param messageKey 消息国际化key
	 * @param params     格式化参数
	 * @return
	 */
	public static String getI18nMessage(String messageKey, Object... params) {
		return StringUtils.format(I18nUtil.getMessage(messageKey), params);
	}

	/**
	 * 是否有内容
	 * 
	 * @return
	 */
	public boolean hasContent() {
		return this.content.length() > 0;
	}
	
	/**
	 * 清空消息
	 */
	public void clean() {
		this.content.setLength(0);
	}

	@Override
	public String toString() {
		return this.content.toString();
	}
}
