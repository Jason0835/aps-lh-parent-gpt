package com.zlt.aps.nc.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 顺位校验结果
 *
 * @author zlt
 */
@Data
@Accessors(chain = true)
public class ShiftValidateResult {
    private boolean passed;
    private String errorMsg;
}
