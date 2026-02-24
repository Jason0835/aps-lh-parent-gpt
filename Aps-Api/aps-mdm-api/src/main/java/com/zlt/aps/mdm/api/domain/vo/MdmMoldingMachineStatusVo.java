package com.zlt.aps.mdm.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Chen
 * @since: 2021/11/22 15:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmMoldingMachineStatusVo extends MdmMoldingMachineStatus {

    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.moldingMachineId",sort = 70)
    private String moldingMachineCode;
}
