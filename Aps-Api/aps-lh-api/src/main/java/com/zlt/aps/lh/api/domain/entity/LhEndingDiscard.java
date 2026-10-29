package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 硫化收尾舍弃计划记录实体类（t_lh_ending_discard）。
 *
 * <p>记录命中收尾小余量或仅历史欠产不再上机规则、本次不排产并进入最终未排结果的物料。
 * 一条记录对应某工厂某排程批次下某个物料被舍弃的计划量，用于追溯收尾舍弃计划量的去向；
 * 该表只做记录，不参与排程计算。</p>
 *
 * @author APS
 * @date 2026/10/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_lh_ending_discard")
@ApiModel(value = "硫化收尾舍弃计划记录", description = "硫化收尾不排产物料的舍弃计划量记录")
public class LhEndingDiscard extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    private String factoryCode;

    /**
     * 排程日期（与排程结果落库的 SCHEDULE_DATE 一致，即排程目标日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 自动排程批次号
     */
    @ApiModelProperty(value = "自动排程批次号")
    private String batchNo;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    /**
     * 产品状态：X-试验示方，T-量试示方，S-正规示方
     */
    @ApiModelProperty(value = "产品状态：X-试验示方，T-量试示方，S-正规示方")
    private String productStatus;

    /**
     * 舍弃计划量，取对应未排规则命中时现有逻辑计算的实际硫化余量。
     */
    @ApiModelProperty(value = "舍弃计划量")
    private Integer discardPlanQty;
}
