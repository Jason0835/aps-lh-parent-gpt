package com.zlt.aps.mdm.api.domain.vo;

import com.zlt.aps.mdm.api.domain.entity.MdmProductConstruction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Chen
 * @since 2025/10/29
 */
@Data
@ApiModel(value = "物料信息施工配置保存Vo", description = "物料信息施工配置保存Vo")
public class ConfigConstructionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "列表", name = "list")
    private List<MdmProductConstruction> list;
}
