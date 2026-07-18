package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 施工信息钢带层位映射器。
 */
@Component
public class Cd15ConstructionMaterialMapper {

    /**
     * 将施工信息1至3层及左右加强层展开为统一钢带单耗列表。
     *
     * @param construction 施工信息
     * @return 完整钢带层位列表
     */
    public List<Cd15ConstructionMaterial> map(MdmConstructionInfo construction) {
        if (construction == null) {
            return Collections.emptyList();
        }
        List<Cd15ConstructionMaterial> result = new ArrayList<>();
        String bigRollCode = construction.getArticleCrownSpec();
        String cuttingAngle = construction.getBeltCuttingAngle();
        this.addLayer(result, construction, bigRollCode, cuttingAngle, 1, false,
                construction.getBeltCode1(), construction.getBeltCraft1(), construction.getBelt1Length());
        this.addLayer(result, construction, bigRollCode, cuttingAngle, 2, false,
                construction.getBeltCode2(), construction.getBeltCraft2(), construction.getBelt2Length());
        this.addLayer(result, construction, bigRollCode, cuttingAngle, 3, false,
                construction.getBeltCode3(), construction.getBeltCraft3(), construction.getBelt3Length());
        this.addLayer(result, construction, bigRollCode, cuttingAngle, 101, true,
                construction.getBeltCodeLeftCode(), construction.getBeltCodeLeftCraft(),
                construction.getBeltCodeLeftLength());
        this.addLayer(result, construction, bigRollCode, cuttingAngle, 102, true,
                construction.getBeltCodeRightCode(), construction.getBeltCodeRightCraft(),
                construction.getBeltCodeRightLength());
        return result;
    }

    /**
     * 将一个有效钢带层位加入统一材料列表；字段合法性由批次前置检查负责。
     */
    private void addLayer(List<Cd15ConstructionMaterial> result,
                          MdmConstructionInfo construction,
                          String bigRollCode,
                          String cuttingAngle,
                          int layerNo,
                          boolean reinforcement,
                          String steelStripCode,
                          java.math.BigDecimal craftWidth,
                          java.math.BigDecimal unitConsumeMillimeter) {
        if (!StringUtils.hasText(steelStripCode)) {
            return;
        }
        result.add(Cd15ConstructionMaterial.builder()
                .constructionCode(construction.getConstructionCode())
                .constructionVersion(construction.getConstructionVersion())
                .steelStripCode(steelStripCode.trim())
                .bigRollCode(this.trim(bigRollCode))
                .cordWidth(construction.getCordWidth())
                .cuttingAngle(this.trim(cuttingAngle))
                .layerNo(layerNo)
                .reinforcement(reinforcement)
                .unitConsumeMillimeter(unitConsumeMillimeter)
                .craftWidth(craftWidth)
                .build());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}