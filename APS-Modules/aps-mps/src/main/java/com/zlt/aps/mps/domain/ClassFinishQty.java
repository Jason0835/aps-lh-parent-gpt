package com.zlt.aps.mps.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;

import lombok.Data;

/**
 * 各工序班次完成量同步
 * 
 * @TableName T_MES_CLASS_FINISH_QTY
 */
@Data
public class ClassFinishQty extends ApsBaseEntity {
	/**
	 * 主键ID，对应序列SEQ_FINISH_QTY
	 */
	private Long id;

	/**
	 * 工序编号，0、硫化，1、成型，2、胎面，3、胎侧，4、内衬，5、胎圈，6、钢丝圈，7、15度裁断，8、90度裁断，9、钢带压延，10、纤维压延
	 */
	private String procedureCode;

	/**
	 * 查询码，CLASS1：1班，CLASS2：2班，CLASS3：3班
	 */
	private String queryCode;

	/**
	 * 完成日期
	 */
	private Date finishDate;

	/**
	 * SAP号
	 */
	private String sapCode;

	/**
	 * 物料号
	 */
	private String materialCode;

	/**
	 * 完成量
	 */
	private BigDecimal finishQty = BigDecimal.ZERO;

	/**
	 * 1班完成量
	 */
	private BigDecimal class1FinishQty;

	/**
	 * 2班完成量
	 */
	private BigDecimal class2FinishQty;

	/**
	 * 3班完成量
	 */
	private BigDecimal class3FinishQty;

	private static final long serialVersionUID = -690270786746175568L;
}