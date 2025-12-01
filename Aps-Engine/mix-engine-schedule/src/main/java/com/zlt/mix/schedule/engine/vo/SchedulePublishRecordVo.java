package com.zlt.mix.schedule.engine.vo;

import java.util.Date;

import com.zlt.mix.common.core.domain.ZltBaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排程发布记录
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulePublishRecordVo extends ZltBaseEntity {
	private static final long serialVersionUID = -7440515791142727532L;
	private Long id;
	/**
	 * 排产日期
	 */
	private Date scheduleDate;
	/**
	 * 发布类型(0--终炼母炼排程，1-硫化辅料排程)
	 */
	private String scheduleType;
}
