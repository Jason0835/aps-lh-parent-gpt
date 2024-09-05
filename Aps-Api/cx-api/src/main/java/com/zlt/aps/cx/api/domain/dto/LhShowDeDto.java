package com.zlt.aps.cx.api.domain.dto;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 全钢硫化定额展示对象 t_cx_show_de
 */
@Data
@ApiModel(value = "全钢硫化定额展示对象", description = "全钢硫化定额展示对象")
public class LhShowDeDto extends BaseEntity {
     private String processCode;
     private String sapSpec;
     private String sapCode;
     private Double num;
}
