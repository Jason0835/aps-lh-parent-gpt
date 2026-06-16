package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        add(result, construction.getConstructionCode(), construction.getCordSpec(),
                construction.getCordWidth(), 1, construction.getTireFabricCode1(),
                construction.getTireFabricCraft1(), construction.getTireFabricLength1());
        add(result, construction.getConstructionCode(), construction.getCordSpec(),
                construction.getCordWidth(), 2, construction.getTireFabricCode2(),
                construction.getTireFabricCraft2(), construction.getTireFabricLength2());
        add(result, construction.getConstructionCode(), construction.getCordSpec(),
                construction.getCordWidth(), 3, construction.getTireFabricCode3(),
                construction.getTireFabricCraft3(), construction.getTireFabricLength3());
        return result;
    }

    private void add(List<Cd90ConstructionMaterial> result,
                     String constructionCode,
                     String bigRollCode,
                     BigDecimal cordWidth,
                     int layerNo,
                     String clothCode,
                     String craftWidthRaw,
                     BigDecimal unitConsumeMillimeter) {
        if (!StringUtils.hasText(clothCode) || unitConsumeMillimeter == null) {
            return;
        }
        if (unitConsumeMillimeter.signum() <= 0) {
            throw new IllegalArgumentException("施工帘布单耗必须大于0, constructionCode="
                    + constructionCode + ", layerNo=" + layerNo);
        }
        BigDecimal craftWidth = parseCraftWidth(
                constructionCode, clothCode, bigRollCode, layerNo, craftWidthRaw);
        result.add(Cd90ConstructionMaterial.builder()
                .constructionCode(constructionCode)
                .clothCode(clothCode)
                .bigRollCode(bigRollCode)
                .cordSpec(clothCode)
                .layerNo(layerNo)
                .unitConsumeMillimeter(unitConsumeMillimeter)
                .cordWidth(cordWidth)
                .craftWidth(craftWidth)
                .craftWidthRaw(craftWidthRaw)
                .build());
    }

    /**
     * 尝试把施工工艺字段解析为直裁宽度；失败时保留原值并由上层转为资料缺失。
     */
    private BigDecimal parseCraftWidth(String constructionCode,
                                       String clothCode,
                                       String bigRollCode,
                                       int layerNo,
                                       String craftWidthRaw) {
        if (!StringUtils.hasText(craftWidthRaw)) {
            log.warn("[直裁自动排程] 施工直裁宽度为空, constructionCode={}, clothCode={}, "
                            + "bigRollCode={}, layerNo={}",
                    constructionCode, clothCode, bigRollCode, layerNo);
            return null;
        }
        try {
            return new BigDecimal(craftWidthRaw.trim());
        } catch (NumberFormatException exception) {
            log.warn("[直裁自动排程] 施工直裁宽度解析失败, constructionCode={}, clothCode={}, "
                            + "bigRollCode={}, layerNo={}, craftWidthRaw={}",
                    constructionCode, clothCode, bigRollCode, layerNo, craftWidthRaw);
            return null;
        }
    }
}
