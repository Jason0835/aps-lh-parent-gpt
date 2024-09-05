package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.gsq.engine.mapper.GsqEngineMachineMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineMachineService;
import com.zlt.aps.gsq.engine.vo.GsqSpecifyMachineVo;
import com.zlt.aps.gsq.engine.vo.GsqTwiningDiscMachineVo;
import com.zlt.aps.gsq.engine.vo.GsqTwiningDiscVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.blankDefault;

@Slf4j
@Service
public class GsqEngineMachineServiceImpl implements GsqEngineMachineService {

    @Resource
    private GsqEngineMachineMapper gsqEngineMachineMapper;

    /**
     * 获得钢丝圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    public Map<String, String> getSpecifyMachineMap(String jobType) {
        Map<String, String> specifyMachineMap = new HashMap<>();
        List<GsqSpecifyMachineVo> list = gsqEngineMachineMapper.listGsqSpecifyMachine(jobType);  //查询钢丝圈定点机台信息
        for(GsqSpecifyMachineVo specifyMachineVo : list) {
            specifyMachineMap.put(specifyMachineVo.getSteelRingCode(), specifyMachineVo.getMachineIds());
        }
//        Map<String, String> specifyMachineMap = list.stream().collect(Collectors.toMap(GsqSpecifyMachineVo::getSteelRingCode, GsqSpecifyMachineVo::getMachineIds));
        return specifyMachineMap;
    }

    /**
     * 获得缠绕盘和机台map (key = 规格尺寸~排列方式 )
     * @return
     */
    public Map<String, String> getTwiningDiscMachineMap() {
        Map<String, String> twiningDiscMachineMap = new HashMap<>();
        List<GsqTwiningDiscMachineVo> list = gsqEngineMachineMapper.listGsqTwiningDiscMachine();  //查询钢丝圈口型板信息
        for(GsqTwiningDiscMachineVo twiningDiscMachineVo : list) {
            String spec = blankDefault(twiningDiscMachineVo.getSpec(), "");  //规格尺寸
            String orderWay = blankDefault(twiningDiscMachineVo.getOrderWay(), "");  //排列方式
            orderWay = orderWay.replace("/", "");  //去掉钢丝圈排列的/符号
            orderWay = orderWay.replace("-", "");  //去掉钢丝圈排列的-符号
            twiningDiscMachineMap.put(spec + "~" + orderWay, twiningDiscMachineVo.getMachineIds());
        }
        return twiningDiscMachineMap;
    }

    /**
     * 获得钢丝圈代码和缠绕盘（value = 规格尺寸~排列方式）map
     * @Param scheduleDate 排程日期
     * @return
     */
    public Map<String, String> getTwiningDiscMap(String scheduleDate) {
        Map<String, String> twiningDiscMap = new HashMap<>();
        List<GsqTwiningDiscVo> list = gsqEngineMachineMapper.listGsqTwiningDisc(scheduleDate);
        for(GsqTwiningDiscVo gsqTwiningDiscVo : list) {
            String specOrder = blankDefault(gsqTwiningDiscVo.getSpecOrder(), "");  //缠绕盘 尺寸~排列
            specOrder = specOrder.replace("/", "");  //去掉钢丝圈排列的/符号
            specOrder = specOrder.replace("-", "");  //去掉钢丝圈排列的-符号
            twiningDiscMap.put(gsqTwiningDiscVo.getSteelRingCode(), specOrder);
        }
        return twiningDiscMap;
    }
}
