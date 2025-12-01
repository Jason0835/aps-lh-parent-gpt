package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 终炼/母炼日计划排程值对象
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueScheduleResultVo extends GlueScheduleResult {
	private static final long serialVersionUID = -7324278269985232356L;
	/**
	 * 配方
	 */
	private MesPmtRecipeVo pmtRecipe;
	/**
	 * 物料大类
	 */
	private String majorType;
	/**
	 * 需求量（包含安全库存）
	 */
	private Double requireQty;
	/**
	 * 本排程计划预计生产的量
	 */
	private BigDecimal planQty;
	/**
	 * 已生产量
	 */
	private BigDecimal productedQty;
	/**
	 * 生产状态
	 */
	private String productState;
	/**
	 * 排产优先级
	 */
	private BigDecimal priority;
	/**
	 * 可排产班次
	 */
	private Integer startShiftClass;

	/**
	 * 上级胶料名称
	 */
	private String upGlue;

	/**
	 * ID序列，查询用
	 */
	private List<Long> idList;

	/**
	 * 排产记录
	 */
	private GlueScheduleResult glueScheduleResult;

	/**
	 * 机台顺序
	 */
	private Integer machineOrder;

	/**
	 * 工单号序列，查询用
	 */
	private List<String> orderNoList;

	/**
	 * 是否通过分解产生的计划
	 */
	private boolean decomposeFlag;

	/**
	 * 原料库存最早到期时间
	 */
	private Date stockValidTime;

	/**
	 * 来源工单号，目前用于转机台保存来源的工单号
	 */
	private String sourceOrderNo;
	
	/**
	 * 超单班限制的量，用于后续计算在每班限制量之外需要额外安排的计划量
	 */
	private BigDecimal overLimitQty;
	/**
	 * 中班发布状态，1=发布成功；2=发布失败
	 */
	private String midPublishStatus;
	/**
	 * 夜班发布状态，1=发布成功；2=发布失败
	 */
	private String nightPublishStatus;
	/**
	 * 白班发布状态，1=发布成功；2=发布失败
	 */
	private String dayPublishStatus;
	/**
	 * 上一个排程
	 */
	private GlueScheduleResultVo previousSchedule;
	/**
	 * 下一个排程
	 */
	private GlueScheduleResultVo nextSchedule;
	/**
	 * 是否高能耗(对应数据字典，ISORNOT，0-是，1-否)
	 */
	private String isHighConsumption;
	/**
	 * 指定胶料+机台的间隔时间
	 */
	private BigDecimal intervalTime;
	/**
	 * 生产模式前排产
	 */
	private GlueScheduleResultVo productionBefore;
	/**
	 * 生产模式后排产
	 */
	private GlueScheduleResultVo productionAfter;
	/**
	 * 绑定生产的排程记录
	 */
	private GlueScheduleResultVo bindScheduleResult;
	/**
	 * 日用量，关联分厂需求计划获取
	 */
//	private Double dayUseQty;
	/**
	 * 排产日标记，1：第一天，2：第二天
	 */
	private String dayFlag;
	/**
	 * 第一天需求是否已完成生产
	 */
	private boolean isDay1Finish;
    /**
     * 预计剩余量，用于计算优先级
     */
    private Double expectedRemainingQty;
	/**
	 * 选配方优先级
	 */
	private Double recipeOrder;
	/**
	 * 标记为生产模式的记录
	 */
	private Boolean productionModelTag;

	/**
	 * 获取映射字段的的key
	 *
	 * @return 映射字段的的key
	 */
	public String getGlueRecipeMapKey() {
		return GenerageMapKeyUtils.createMapKey(getGlue(), getRecipeType());
	}
}
