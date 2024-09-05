package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.cx.api.domain.entity.CxProductStockLimit;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 成型投产班次库存限定设置
 */
@Data
@ApiModel(value = "成型投产班次库存限定设置", description = "成型投产班次库存限定设置 ")
public class CxEngineProductStockLimit extends CxProductStockLimit {
}
