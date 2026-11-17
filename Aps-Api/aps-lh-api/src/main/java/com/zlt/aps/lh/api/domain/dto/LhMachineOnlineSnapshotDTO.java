package com.zlt.aps.lh.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 前段欠产准入使用的MES在机只读投影。
 * 逻辑删除由本投影的框架映射处理，不改变既有MES同步实体和写入接口。
 *
 * @author APS
 * @since 2026/11/15
 */
@Data
@TableName("T_LH_MACHINE_ONLINE_INFO")
public class LhMachineOnlineSnapshotDTO implements Serializable {

    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;

    /**
     * MES记录标识。
     */
    @TableId("ID")
    private Long id;

    /**
     * 工厂编号。
     */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 在机业务日期。
     */
    @TableField("ONLINE_DATE")
    private Date onlineDate;

    /**
     * MES机台编号，可能包含左右侧后缀。
     */
    @TableField("LH_CODE")
    private String lhCode;

    /**
     * 在机物料编码。
     */
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 在机左右模范围。
     */
    @TableField("LR_MOLDS")
    private String lrMolds;

    /**
     * 在机物料的产品状态，仅作为审计证据保留。
     */
    @TableField("PRODUCT_STATUS")
    private String productStatus;

    /**
     * 框架逻辑删除标识，禁止已删除快照参与日期比较。
     */
    @TableField(value = "IS_DELETE", select = false)
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
