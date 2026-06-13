package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 施工信息帘布层位映射器。
 */
@Component
public class Cd90ConstructionMaterialMapper {

    /**
     * 将施工信息1至3层展开为统一帘布单耗列表。
     *
     * @param construction 施工信息
     * @return 完整帘布层位列表
     */
    public List<Cd90ConstructionMaterial> map(MdmConstructionInfo construction) {
        if (construction == null) {
            return Collections.emptyList();
        }
        List<Cd90ConstructionMaterial> result = new ArrayList<>();
        add(result, construction.getConstructionCode(), construction.getCordSpec(), 1,
                construction.getTireFabricCode1(), construction.getTireFabricLength1());
        add(result, construction.getConstructionCode(), construction.getCordSpec(), 2,
                construction.getTireFabricCode2(), construction.getTireFabricLength2());
        add(result, construction.getConstructionCode(), construction.getCordSpec(), 3,
                construction.getTireFabricCode3(), construction.getTireFabricLength3());
        return result;
    }

    private void add(List<Cd90ConstructionMaterial> result,
                     String constructionCode,
                     String cordSpec,
                     int layerNo,
                     String clothCode,
                     BigDecimal unitConsumeMillimeter) {
        if (!StringUtils.hasText(clothCode) || unitConsumeMillimeter == null) {
            return;
        }
        if (unitConsumeMillimeter.signum() <= 0) {
            throw new IllegalArgumentException("施工帘布单耗必须大于0, constructionCode="
                    + constructionCode + ", layerNo=" + layerNo);
        }
        result.add(Cd90ConstructionMaterial.builder()
                .constructionCode(constructionCode)
                .clothCode(clothCode)
                .bigRollCode(cordSpec)
                .cordSpec(cordSpec)
                .layerNo(layerNo)
                .unitConsumeMillimeter(unitConsumeMillimeter)
                .build());
    }
}
