package com.zlt.aps.mdm.api.domain.dto;

import com.zlt.aps.mdm.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mdm.api.domain.vo.ProductSpecInfoVo;
import lombok.Data;

import java.util.List;

/**
 * SAP与施工关系辅助类
 *
 * @author ZLT
 * @date 20250418
 */
@Data
public class MdmProductConstructionDto extends MdmProductConstruction {
    /**
     * 施工信息数据
     */
    private List<ProductSpecInfoVo> productSpecCodeInfoList;

}
