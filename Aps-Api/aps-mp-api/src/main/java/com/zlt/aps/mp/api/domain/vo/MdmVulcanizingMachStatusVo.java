package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.mp.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author: Chen
 * @since: 2021/9/9 11:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmVulcanizingMachStatusVo extends MdmVulcanizingMachStatus {

    /**
     * 品名代号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 5)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.productTypeCode", sort = 50)
    private String productTypeCode;

    /**
     * 硫化机编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.docVulcanizationMachStatus.vulcanizingMachineId", sort = 70)
    private String vulcanizingMachineCode;

    /**
     * 根据id是否为空给创建时间，创建人，更新时间，更新人赋值
     */
    public void setBaseVale(Long id) {
        if (id == null) {
            //id为空，表示为新增操作
            this.setIsDelete(0);
            this.setCreateBy(SecurityUtils.getUsername());
            this.setCreateTime(new Date());
        } else {
            //更新操作
            this.setUpdateBy(SecurityUtils.getUsername());
            this.setUpdateTime(new Date());
        }
    }
}
