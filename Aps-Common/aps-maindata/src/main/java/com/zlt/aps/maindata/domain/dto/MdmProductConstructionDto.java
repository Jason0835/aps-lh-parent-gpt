package com.zlt.aps.maindata.domain.dto;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
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
