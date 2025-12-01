package com.zlt.mix.schedule.engine.service.decompose.impl;

import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.mapper.MotherGlueDecomposeMapper;
import com.zlt.mix.schedule.engine.service.decompose.MotherGlueDecomposeService;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 根据终炼胶分解出对应的母炼胶
 */
@Service("MotherGlueDecomposeServiceImpl")
public class MotherGlueDecomposeServiceImpl implements MotherGlueDecomposeService {

    @Resource
    private MotherGlueDecomposeMapper motherGlueDecomposeMapper;

    /**
     * 解析终炼母炼分解表
     * @param mixArea 密炼区
     * @param glueAreaMachineList 终炼胶、密炼区、机台列表
     * @param glueMachineMap 胶料对应的机台map
     * @return 最终返回格式：key--终炼胶code，value--此终炼胶下的母炼胶code集合
     */
    public Map<String, List<GlueAreaMachineVo>> parseGlueDecompose(String mixArea, List<GlueAreaMachineVo> glueAreaMachineList, Map<String, String> glueMachineMap) {
        //从终炼母炼分解表中解析终炼胶对应的母炼胶列表(start)
        Map<String, List<GlueAreaMachineVo>> glueMap = new HashMap<>();
        List<GlueDecompose> glueDecomposeList = motherGlueDecomposeMapper.listGlueDecompose(glueAreaMachineList); //获取终炼母炼分解表列表数据
        for(GlueDecompose glueDecompose : glueDecomposeList) {
            int segment = glueDecompose.getSegment();
            if(segment == 1) {
                //1段胶没有母炼胶，直接不需要解析
                continue;
            }
            List<GlueAreaMachineVo> motherGlueList = this.gainMotherList(mixArea, glueDecompose, glueMachineMap);  //获取终炼胶下的全部母炼胶列表
            glueMap.put(glueDecompose.getGlue(), motherGlueList);
        }
        //从终炼母炼分解表中解析终炼胶对应的母炼胶列表(end)

        //从密炼机指定胶料表中 分解 终炼胶对应的母炼胶(start)
        Map<String, List<GlueAreaMachineVo>> machineGlueMap = new HashMap<>();
        List<MachineGlueDecompose> machineGlueDecomposeList = motherGlueDecomposeMapper.listMachineGlueDecompose(glueAreaMachineList);
        for(MachineGlueDecompose machineGlueDecompose : machineGlueDecomposeList) {
            List<GlueAreaMachineVo> machineMotherGlueList = this.gainMachineMotherList(machineGlueDecompose, glueMachineMap);  //获取终炼胶在特殊机台下的母炼胶分解列表
            glueMap.put(machineGlueDecompose.getGlue(), machineMotherGlueList);
        }
        //从密炼机指定胶料表中 分解 终炼胶对应的母炼胶(end)
        return glueMap;
    }

    /**
     * 解析母炼胶 对应的全部下级母炼胶
     * @return
     */
    public List<GlueAreaMachineVo> parseGlueDecomposeByMother(GlueDecomposePlan plan, Map<String, String> glueMachineMap) {
        List<GlueAreaMachineVo> motherGlueList = new ArrayList<>();
        String motherGlue = plan.getGlue();
        String mixArea = plan.getMixArea();
        List<GlueDecompose> list = motherGlueDecomposeMapper.listMotherGlueDecompose(motherGlue);
        if(list != null && !list.isEmpty()) {
            GlueDecompose glueDecompose = list.get(0);  //只取第一条记录
            List<GlueAreaMachineVo> tempMotherGlueList = this.gainMotherList(mixArea, glueDecompose, glueMachineMap);  //获取终炼胶下的全部母炼胶列表
            for(GlueAreaMachineVo glueAreaMachineVo : tempMotherGlueList) {
                if(motherGlue.equals(glueAreaMachineVo.getGlue())) {
                    break;
                } else {
                    motherGlueList.add(glueAreaMachineVo);
                }
            }
            String upGlue = getUpGlue(glueDecompose, tempMotherGlueList, motherGlue);
            plan.setUpGlue(upGlue);  //设置父级胶
            return motherGlueList;
        } else {
            return motherGlueList;
        }
    }

    private String getUpGlue(GlueDecompose glueDecompose, List<GlueAreaMachineVo> tempMotherGlueList, String motherGlue) {
        if(tempMotherGlueList == null || tempMotherGlueList.isEmpty()) {
            return null;
        }
        String upGlue = null;
        for(int i = tempMotherGlueList.size()-1; i >= 0; i--) {
            GlueAreaMachineVo glueAreaMachineVo = tempMotherGlueList.get(i);
            if(motherGlue.equals(glueAreaMachineVo.getGlue()) && i == tempMotherGlueList.size()-1) {
                return glueDecompose.getGlue();
            } else if(motherGlue.equals(glueAreaMachineVo.getGlue())) {
                break;
            }
            upGlue = glueAreaMachineVo.getGlue();
        }
        return upGlue;
    }

    /**
     * 获取密炼机指定终炼胶对应的母炼胶列表
     * @param machineGlueDecompose 密炼机指定胶料对象
     * @param glueMachineMap 胶料对应的机台map
     * @return
     */
    private List<GlueAreaMachineVo> gainMachineMotherList(MachineGlueDecompose machineGlueDecompose, Map<String, String> glueMachineMap) {
        List<GlueAreaMachineVo> list = new ArrayList<>();
        String mixArea = machineGlueDecompose.getMixArea();
        String glue = "";
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue1())) {
            glue = machineGlueDecompose.getMotherGlue1();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue2())) {
            glue = machineGlueDecompose.getMotherGlue2();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue3())) {
            glue = machineGlueDecompose.getMotherGlue3();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue4())) {
            glue = machineGlueDecompose.getMotherGlue4();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue5())) {
            glue = machineGlueDecompose.getMotherGlue5();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue6())) {
            glue = machineGlueDecompose.getMotherGlue6();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue7())) {
            glue = machineGlueDecompose.getMotherGlue7();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue8())) {
            glue = machineGlueDecompose.getMotherGlue8();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(StringUtils.isNotBlank(machineGlueDecompose.getMotherGlue9())) {
            glue = machineGlueDecompose.getMotherGlue9();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        return list;
    }

    /**
     * 获取终炼胶下的全部母炼胶列表
     * @param mixArea 密炼区
     * @param glueDecompose
     * @param glueMachineMap 胶料对应的机台map
     * @return
     */
    private List<GlueAreaMachineVo> gainMotherList(String mixArea, GlueDecompose glueDecompose, Map<String, String> glueMachineMap) {
        List<GlueAreaMachineVo> list = new ArrayList<>();
        int segment = glueDecompose.getSegment();
        if(segment == 1) {
            //1段胶没有母炼胶，直接不需要解析
            return list;
        }

        String glue = "";
        if(segment >= 2 && StringUtils.isNotBlank(glueDecompose.getMotherGlue1())) {
            glue = glueDecompose.getMotherGlue1();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 3 && StringUtils.isNotBlank(glueDecompose.getMotherGlue2())) {
            glue = glueDecompose.getMotherGlue2();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 4 && StringUtils.isNotBlank(glueDecompose.getMotherGlue3())) {
            glue = glueDecompose.getMotherGlue3();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 5 && StringUtils.isNotBlank(glueDecompose.getMotherGlue4())) {
            glue = glueDecompose.getMotherGlue4();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 6 && StringUtils.isNotBlank(glueDecompose.getMotherGlue5())) {
            glue = glueDecompose.getMotherGlue5();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 7 && StringUtils.isNotBlank(glueDecompose.getMotherGlue6())) {
            glue = glueDecompose.getMotherGlue6();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 8 && StringUtils.isNotBlank(glueDecompose.getMotherGlue7())) {
            glue = glueDecompose.getMotherGlue7();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 9 && StringUtils.isNotBlank(glueDecompose.getMotherGlue8())) {
            glue = glueDecompose.getMotherGlue8();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        if(segment >= 10 && StringUtils.isNotBlank(glueDecompose.getMotherGlue9())) {
            glue = glueDecompose.getMotherGlue9();
            list.add(new GlueAreaMachineVo(mixArea, glue, glueMachineMap.get(mixArea + glue)));
        }
        return list;
    }

}
