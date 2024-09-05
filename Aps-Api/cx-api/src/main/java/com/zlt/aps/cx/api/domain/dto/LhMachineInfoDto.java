package com.zlt.aps.cx.api.domain.dto;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 硫化机台信息对象
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-12-17 11:08:29
 */
@Data
@ApiModel(value = "硫化机台对象", description = "硫化机台信息")
public class LhMachineInfoDto {

	/** 主键ID */
	@ApiModelProperty(value = "id")
	private Long id;

	/** 机台编号 */
	@ApiModelProperty(value = "机台编号")
	private String machineCode;

	/** 机台名称 */
	@ApiModelProperty(value = "机台名称")
	private String machineName;

	/** 胎胚代码 */
	@ApiModelProperty(value = "胎胚代码")
	private String embryoCode;
	/** 投产胎胚数，主要用于前端背景色显示，大于1需要标注颜色 */
	@ApiModelProperty(value = "投产胎胚数")
	private int embryoNum = 0;

	/** 胎胚库存 */
	@ApiModelProperty(value = "胎胚库存")
	private Long embyroStock = 0L;

	/** 月计划剩余量（外胎） */
	@ApiModelProperty(value = "月计划剩余量（外胎）")
	private Integer monthPlanOs;

	/** 寸口 */
	@ApiModelProperty(value = "寸口")
	private Double specDimension;

	/** 成型生产状态:0-未生产；1-生产中；2-已收尾 */
    @ApiModelProperty(value = "成型生产状态")
    private String cxProductionStatus;

	/** 硫化生产状态，枚举项TaskTypeEnum:1-待投产；2-待换模；3-投产中；4-已收尾；5-已收尾欠产 */
    @ApiModelProperty(value = "硫化生产状态")
    private String lhProductionStatus;

	/** 排产日期 */
	@ApiModelProperty(value = "排产日期")
	private Date scheduleDate;

	/** 模数 */
	@ApiModelProperty(value = "模数")
	private int moldNum = 0;
	
	/** 可硫化班数 */
    @ApiModelProperty(value = "可硫化班数")
    private Double classAvailableLhShift;

	/** 单班硫化量 */
	@ApiModelProperty(value = "单班硫化量")
	private Integer singleShiftLhQty;
}
