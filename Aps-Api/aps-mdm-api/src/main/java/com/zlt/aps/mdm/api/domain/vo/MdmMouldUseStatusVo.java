package com.zlt.aps.mdm.api.domain.vo;

import com.zlt.aps.mdm.api.domain.entity.MdmMouldUseStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class MdmMouldUseStatusVo extends MdmMouldUseStatus implements Serializable {

    /**
     * 启用模具总数量
     */
    private Integer mouldQty;

    /**
     * 禁用模具数量
     */
    private Integer noMouldQty;

    /**
     * 模具总数量
     */
    private Integer totalMouldQty;
}
