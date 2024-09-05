package com.zlt.aps.cx.api.domain.dto;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 全钢成型定额展示对象 t_cx_show_de
 */
@Data
@ApiModel(value = "全钢成型定额展示对象", description = "全钢成型定额展示对象")
public class CxShowDeDto extends BaseEntity {
     private String processCode;
     private String processDesc;
     private Double specDimension;
     private Double threeNum;
    private Double fourNum;
}
