package com.zlt.aps.common.engine.domain;

import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 引擎工序投产施工信息
 */
@ApiModel(value = "投产施工信息对象", description = "投产施工信息对象 ")
@Data
public class EngineProductConstructionInfo extends CxProductConstructionInfo {
}
