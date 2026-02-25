package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;

/**
 * 成型机可用信息-导入模板
 */
@Data
public class MdmMoldingMachineStatusTemplateVo extends BaseEntity {

    /**
     * 年份
     */
    @Excel(name = "ui.data.colume.year")
    @ImportExcelValidated(required = true, max = 9999)
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.colume.month")
    @ImportExcelValidated(required = true, min = 1, max = 12)
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 品名代码
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 5)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.productTypeCode")
    @TableField(exist = false)
    private String productTypeCode;

    /**
     * 成型机状态:0-禁用，1-可用
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.status", dictType = "sys_enable_disable")
    @TableField(value = "STATUS")
    private Integer status;

    /**
     * 机台编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.moldingMachineId")
    private String moldingMachineCode;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.remark")
    @TableField("REMARK")
    private String remark;

}
