package com.zlt.aps.lh.vo;

import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 
 * 模具变动发布记录 T_MOLD_PLAN_PUBLISH_RECORD
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-10-19 10:53:33
 */
@ApiModel(value = "模具变动发布记录值对象", description = "模具变动发布记录值对象 ")
@Data
public class MoldPlanPublishRecordVo extends ApsBaseEntity {
	private static final long serialVersionUID = 9185349147779192551L;
	/**
	 * 主键ID
	 */
	private Long id;
	/**
	 * 发布状态：0-未发布；1-已发布
	 */
	private String publishStatus;
	/**
	 * 发布日期
	 */
	private Date publishDate;

	/**
	 * 发布的数据版本
	 */
	private String dataVersion;
}
