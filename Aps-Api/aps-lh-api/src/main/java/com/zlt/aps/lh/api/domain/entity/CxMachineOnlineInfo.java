package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 成型在机信息（硫化侧只读引用实体）
 * <p>
 * 表结构与写入均由成型侧维护（对应表 {@code T_CX_MACHINE_ONLINE_INFO}，MES 同步成型机台在机快照）。
 * 硫化侧仅在胎胚收尾标识加载时读取，用于「成型在机双结构 → 结构切换完成」判定：
 * 同机台窗口内同时存在切换前后两个结构的在机行时，前结构胎胚强制标记收尾。
 * 因此本实体只声明判定所需字段，不参与写入。
 * </p>
 *
 * @author APS Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_CX_MACHINE_ONLINE_INFO")
@ApiModel(value = "成型在机信息（硫化侧只读引用）", description = "成型在机信息（硫化侧只读引用）")
public class CxMachineOnlineInfo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 上机日期
     */
    @ApiModelProperty(value = "日期", name = "onlineDate")
    @TableField(value = "ONLINE_DATE")
    private Date onlineDate;

    /**
     * 成型机台编码
     */
    @ApiModelProperty(value = "成型机台", name = "cxCode")
    @TableField(value = "CX_CODE")
    private String cxCode;

    /**
     * 物料编码（MES 同步口径该字段实际存的是胎胚编号，双结构判定按胎胚号取结构归属）
     */
    @ApiModelProperty(value = "物料编码（实际存胎胚编号）", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 交班在机标识（MES 6 点版本同步时置 '1'，其余为空/未回传）
     * <p>由成型侧 MES 同步写入（{@code CxMachineOnlineInfoServiceImpl#resolveHandoverOnline}），
     * 硫化侧只读：'1' 的行即交班时刻仍在产的机台+胎胚，用于“前结构已结束”判定各机台当前在产结构。</p>
     */
    @ApiModelProperty(value = "交班在机标识（1-仍在产；空-未在产）", name = "handoverOnline")
    @TableField(value = "HANDOVER_ONLINE")
    private String handoverOnline;
}
