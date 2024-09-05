package com.zlt.aps.common.core.annotation;

import lombok.Data;

@Data
public class ImportTest {

    @ImportValidated(name="ui.glueOrder.column.glueCode",required = true, maxLength = 3, isCode = true)
    public String aa;

    @ImportValidated(name="ui.glueOrder.column.orderNum", min = 600)
    private Integer sort;
}
