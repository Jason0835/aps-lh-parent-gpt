package com.zlt.aps.xwyy.engine.vo;

import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;

import lombok.Data;
/**
 * 自动排产记录值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-22 11:00:18
 * @Version 1.0
 */
@Data
public class XwyyScheduleRecordVo extends ApsBaseEntity {
	/**
	 * 成型批次号
	 */
	private String cxBatchNo;
	/**
	 * 批次号
	 */
	private String batchNo;
	/**
	 * 排产日期
	 */
	private Date scheduleDate;
	/**
	 * 状态
	 */
	private String status;
}
