package com.zlt.mix.schedule.engine.service.decompose.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.mapper.MotherGlueDecomposeMapper;
import com.zlt.mix.schedule.engine.service.decompose.MotherGlueDecomposeService;
import com.zlt.mix.schedule.engine.vo.DecomposeRecipeVo;
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
@Service("MotherRecipeDecomposeServiceImpl")
public class MotherRecipeDecomposeServiceImpl implements MotherGlueDecomposeService {

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
        Map<String, List<GlueAreaMachineVo>> glueMap = new HashMap<>();
        //从配方表中解析终炼胶对应的母炼胶列表(start)
        Map<String, String> decomposeRecipeMap = this.mapDecomposeRecipe();  //从配方表中查询出全部终炼、母炼分解Map信息
        for(GlueAreaMachineVo glueAreaMachineVo : glueAreaMachineList) {
            List<GlueAreaMachineVo> motherGlueList = new ArrayList<>();
            this.parseRecipeDecomposeByMother(motherGlueList, decomposeRecipeMap, glueMachineMap, glueAreaMachineVo, mixArea);
            motherGlueList = motherGlueList.stream().sorted(Comparator.comparing(GlueAreaMachineVo::getGlue)).collect(Collectors.toList());  //根据胶料升序排
            glueMap.put(glueAreaMachineVo.getGlue(), motherGlueList);
        }
        //从配方表中解析终炼胶对应的母炼胶列表(end)

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
     * @param plan
     * @param glueMachineMap 胶料对应的机台map
     * @return
     */
    public List<GlueAreaMachineVo> parseGlueDecomposeByMother(GlueDecomposePlan plan , Map<String, String> glueMachineMap) {
        List<GlueAreaMachineVo> motherGlueList = new ArrayList<>();
        String glue = plan.getGlue();
        String mixArea = plan.getMixArea();
        String machineCode = plan.getMachineCode();
        Map<String, String> decomposeRecipeMap = this.mapDecomposeRecipe();  //从配方表中查询出全部终炼、母炼分解Map信息

        this.parseRecipeDecomposeByMother(motherGlueList, decomposeRecipeMap, glueMachineMap, new GlueAreaMachineVo(mixArea, glue, machineCode), mixArea);
        return motherGlueList;
    }

    /**
     * 解析胶料胶 对应的全部下级母炼胶(递归)
     * @param motherGlueList 此胶料对应的母炼胶列表
     * @param decomposeRecipeMap 配方表中全部终炼、母炼分解Map信息
     * @param glueMachineMap 胶料对应的机台map
     * @param fatherGlueAreaMachine 父级胶料+机台
     * @param mixArea 密炼区
     * @return
     */
    private void parseRecipeDecomposeByMother(List<GlueAreaMachineVo> motherGlueList, Map<String, String> decomposeRecipeMap, Map<String, String> glueMachineMap,
                                                              GlueAreaMachineVo fatherGlueAreaMachine, String mixArea) {
        String fatherMachineCode = fatherGlueAreaMachine.getMachineCode();
        String fatherGlue = fatherGlueAreaMachine.getGlue();
        String sonGlue = decomposeRecipeMap.get(fatherMachineCode + fatherGlue);
        if(sonGlue == null || sonGlue.equals(fatherGlue)) {
            return;
        }

        GlueAreaMachineVo sonGlueAreaMachine = new GlueAreaMachineVo(mixArea, sonGlue, glueMachineMap.get(mixArea + sonGlue));
        motherGlueList.add(sonGlueAreaMachine);
        this.parseRecipeDecomposeByMother(motherGlueList, decomposeRecipeMap, glueMachineMap, sonGlueAreaMachine, mixArea);
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
     * 从配方表中查询出全部终炼、母炼分解Map信息（同个胶料、同个机台下有多个配方的情况下，拿配方类型最小的，版本号最大的那条记录）
     * @return key= 机台code + 胶料名称， value = 母胶料名称
     */
    private Map<String, String> mapDecomposeRecipe() {
        Map<String, String> map = new HashMap<>();
        List<DecomposeRecipeVo> decomposeRecipeList = motherGlueDecomposeMapper.listDecomposeRecipe();
        for(DecomposeRecipeVo recipeVo : decomposeRecipeList) {
            map.put(recipeVo.getMachineCode() + recipeVo.getGlue(), recipeVo.getSonGlue());
        }
        return map;
    }
}
