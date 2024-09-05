package com.zlt.aps.tc.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.tc.engine.mapper.TcEngineLossMapper;
import com.zlt.aps.tc.engine.service.TcEngineLossService;
import com.zlt.aps.tc.engine.vo.TcLossVo;
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
public class TcEngineLossServiceImpl implements TcEngineLossService {

    @Resource
    private TcEngineLossMapper tcEngineLossMapper;

    /**
     * 把损耗率list转成map
     *
     * @return key：机台id + ‘#’ + 胎侧代码，例如20#HT3568-377P
     */
    public Map<String, Double> getLossRateMap() {
        Map<String, Double> lossMap = new HashMap<>();
        List<TcLossVo> list = tcEngineLossMapper.listLossRate();
        for (TcLossVo lossVo : list) {
            lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
        }
        return lossMap;
    }

    /**
     * 获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 机台 >工序参数配置）
     *
     * @param sidewallCode  胎侧代码
     * @param machineIds 机台id
     * @param paramLossRate 工序参数设置的损耗率
     * @return
     */
    public double getLossRate(String sidewallCode, String machineIds, Map<String, Double> lossMap, double paramLossRate) {
        sidewallCode = (StringUtils.isBlank(sidewallCode) ? "" : sidewallCode);
        if (StringUtils.isBlank(machineIds)) {
            //排程没有机台信息
            double lossRate = lossMap.getOrDefault("#" + sidewallCode, paramLossRate);
            return BigDecimalUtil.div(lossRate, 100D);
        } else {
            //排程有机台信息
            String[] machineIdArry = machineIds.split(",");
            double totalLoss = 0;
            for (int i = 0; i < machineIdArry.length; i++) {
                String key1 = machineIdArry[i] + "#" + sidewallCode;
                String key2 = "#" + sidewallCode;
                String key3 = machineIdArry[i] + "#";
                Double templossRate = lossMap.getOrDefault(key1, lossMap.getOrDefault(key2, lossMap.getOrDefault(key3, paramLossRate)));
                totalLoss += templossRate;
            }
            totalLoss = BigDecimalUtil.div(totalLoss, 100D);  //把耗损率由百分比，转成对应小数
            return BigDecimalUtil.div(totalLoss, machineIdArry.length, 4);  //计算平均损耗率
        }
    }
}
