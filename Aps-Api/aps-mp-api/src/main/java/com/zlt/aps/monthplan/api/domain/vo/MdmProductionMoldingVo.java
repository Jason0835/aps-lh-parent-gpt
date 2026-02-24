package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionMolding;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MdmProductionMoldingVo extends MdmProductionMolding {
    // 品名
    @Excel(name = "ui.data.column.docFactoryNotProduction.productName", type = Excel.Type.EXPORT, dictType = "biz_product_type", sort = 70)
    private String productName;
}
