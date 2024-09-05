package com.zlt.aps.gsq.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.gsq.engine.mapper.GsqEngineLossMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineLossService;
import com.zlt.aps.gsq.engine.vo.GsqLossVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 损耗率service
 */
@Slf4j
@Service
public class GsqEngineLossServiceImpl implements GsqEngineLossService {

    @Resource
    private GsqEngineLossMapper gsqEngineLossMapper;

    /**
     * 把损耗率list转成map
     *
     * @return key：机台id + ‘#’ + 钢丝圈代码，例如20#HT3568-377P
     */
    public Map<String, Double> getLossRateMap() {
        Map<String, Double> lossMap = new HashMap<>();
        List<GsqLossVo> list = gsqEngineLossMapper.listLossRate();
        for (GsqLossVo lossVo : list) {
            lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
        }
        return lossMap;
    }

    /**
     * 获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 钢丝圈代码 > 机台 >工序参数配置）
     *
     * @param steelRingCode  钢丝圈代码
     * @param machineIds 机台id
     * @param paramLossRate 工序参数设置的损耗率
     * @return
     */
    public double getLossRate(String steelRingCode, String machineIds, Map<String, Double> lossMap, double paramLossRate) {
        steelRingCode = (StringUtils.isBlank(steelRingCode) ? "" : steelRingCode);
        if (StringUtils.isBlank(machineIds)) {
            //排程没有机台信息
            double lossRate = lossMap.getOrDefault("#" + steelRingCode, paramLossRate);
            return BigDecimalUtil.div(lossRate, 100D);
        } else {
            //排程有机台信息
            String[] machineIdArry = machineIds.split(",");
            double totalLoss = 0;
            for (int i = 0; i < machineIdArry.length; i++) {
                String key1 = machineIdArry[i] + "#" + steelRingCode;
                String key2 = "#" + steelRingCode;
                String key3 = machineIdArry[i] + "#";
                Double templossRate = lossMap.getOrDefault(key1, lossMap.getOrDefault(key2, lossMap.getOrDefault(key3, paramLossRate)));
                totalLoss += templossRate;
            }
            totalLoss = BigDecimalUtil.div(totalLoss, 100D);  //把耗损率由百分比，转成对应小数
            return BigDecimalUtil.div(totalLoss, machineIdArry.length, 4);  //计算平均损耗率
        }
    }
}
