package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
  * 规格使用模数
  * @ClassName CxEngineSapSpecMoldUse
  * @Description 维护sap和胎胚在自动排程投产时默认使用模数
  * @Author Joran.Zhang
  * @Date 2022/1/18 10:28
  * @Version 1.0
**/
@Data
@ApiModel(value = "规格使用模数对象", description = "规格使用模数对象")
public class CxEngineSapSpecMoldUse extends SapSpecMoldUse {
}
