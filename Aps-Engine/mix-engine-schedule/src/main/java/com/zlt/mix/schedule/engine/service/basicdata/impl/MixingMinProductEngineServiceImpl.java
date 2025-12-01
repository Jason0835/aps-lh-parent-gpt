package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.schedule.engine.mapper.MixingMinProductEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MixingMinProductEngineService;
import com.zlt.mix.setting.api.domain.entity.MixingMinProduct;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎炼胶单规格最小排产数相关ServiceImpl
 *
 * @author Liam
 * @since 2025/4/11
 */
@Service
public class MixingMinProductEngineServiceImpl implements MixingMinProductEngineService {
    @Resource
    private MixingMinProductEngineMapper mixingMinProductEngineMapper;

    /**
     * 加载炼胶单规格最小排产数
     *
     * @param mixArea 密炼取
     * @return 炼胶单规格最小排产数
     */
    @Override
    public Map<String, BigDecimal> mapMixingMinProduct(String mixArea) {
        MixingMinProduct query = new MixingMinProduct();
        query.setMixArea(mixArea);
        return mixingMinProductEngineMapper.selectMixingMinProduct(query).stream()
                .filter(v -> StringUtils.isNotBlank(v.getGlue()) && v.getMinProductStock() != null)
                .collect(Collectors.toMap(MixingMinProduct::getGlue, v-> BigDecimal.valueOf(v.getMinProductStock()), (v1, v2) -> v1));
    }
}
