package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.dto.MachineOrderDto;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.mapper.FormulaMachineMapper;
import com.zlt.mix.setting.service.FormulaMachineService;
import com.zlt.mix.setting.service.MesPmtRecipeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 配方与机台对应Service业务层处理
 *
 * @author Gim
 * @date 2022-03-28
 */
@Service
public class FormulaMachineServiceImpl extends ServiceImpl<FormulaMachineMapper, FormulaMachine> implements FormulaMachineService {
    @Resource
    private FormulaMachineMapper formulaMachineMapper;

    @Autowired
    private MesPmtRecipeService mesPmtRecipeService;

    /**
     * 查询配方与机台对应列表
     *
     * @param formulaMachine 配方与机台对应
     * @return 配方与机台对应
     */
    @Override
    public List<FormulaMachine> selectFormulaMachineList(FormulaMachine formulaMachine) {
        List<FormulaMachine> formulaMachines = formulaMachineMapper.selectFormulaMachineList(formulaMachine);
        return formulaMachines;
    }

    /**
     * 根据机台编号获取机台名称
     */
    public List<FormulaMachine> machineCodeToMachineName(List<FormulaMachine> formulaMachines, FormulaMachine formulaMachine) {

        // 取机台名称
        MesPmtRecipe params = new MesPmtRecipe();
        String formulaMachineMixArea = formulaMachine.getMixArea();
        if (StringUtils.isNotBlank(formulaMachineMixArea)) {
            params.setMixArea(formulaMachineMixArea);
        }
        String glue = formulaMachine.getGlue();
        if (StringUtils.isNotBlank(glue) && formulaMachines.size() == 1) {
            params.setRecipeMaterialName(glue);
        }
        List<MesPmtRecipe> machineList = mesPmtRecipeService.selectMesPmtRecipeMachine(params);
        Map<String, MesPmtRecipe> machineMap = CollectionUtil.toMap(machineList, obj -> (
                GenerageMapKeyUtils.createMapKey(obj.getMixArea(), obj.getRecipeEquipCode())
        ));

        // 密炼区的字典通过ExcelUtil自动转换
        //（密炼区+机台编号）转换成机台名称
        for (FormulaMachine machine : formulaMachines) {
            if (StringUtils.isNotBlank(machine.getMachineCode()) && StringUtils.isNotBlank(machine.getMixArea())) {
                String mixArea = machine.getMixArea();
                // 拼接机台
                String[] machineCodeList = machine.getMachineCode().split(",");
                StringBuilder machineName = new StringBuilder();
                for (String machineCode : machineCodeList) {
                    MesPmtRecipe mesPmtRecipe = machineMap.get(GenerageMapKeyUtils.createMapKey(mixArea, machineCode));
                    if (mesPmtRecipe != null) {
                        if (StringUtils.isNotBlank(machineName)) {
                            machineName.append(",").append(mesPmtRecipe.getMachineName());
                        } else {
                            machineName.append(mesPmtRecipe.getMachineName());
                        }
                    }
                }
                machine.setMachineName(machineName.toString());
            }
        }
        return formulaMachines;
    }

    /**
     * 保存配方与机台对应信息（id为空则新增，id不为空则修改）
     *
     * @param formulaMachine
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFormulaMachine(FormulaMachine formulaMachine) {
        // 先删除（物理删除）
        formulaMachineMapper.trueDeleteByMixAreaAndGlue(formulaMachine.getMixArea(), formulaMachine.getGlue());
        formulaMachine.setId(null);
        formulaMachine.setBaseValue(null);
        List<FormulaMachine> list = new ArrayList<>();
        List<MachineOrderDto> machineOrderList = formulaMachine.getMachineOrderList();
        for (MachineOrderDto machineOrderDto : machineOrderList) {
            FormulaMachine machine = new FormulaMachine();
            BeanUtils.copyProperties(formulaMachine, machine);
            machine.setMachineCode(machineOrderDto.getMachineCode());
            machine.setMachineOrder(machineOrderDto.getMachineOrder().toString());
            list.add(machine);
        }
        formulaMachineMapper.batchInsertFormulaMachineInfo(list);
    }

    /**
     * 批量删除配方与机台对应
     *
     * @param ids 需要删除的配方与机台对应ID
     * @return 结果
     */
    @Override
    public int deleteFormulaMachineByIds(Long[] ids) {
        List<FormulaMachine> list = formulaMachineMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(list)) {
            formulaMachineMapper.batchDeleteFormulaMachineInfo(list);
        }
        return 1;
    }


    /**
     * 校验配方与机台对应唯一性
     */
    @Override
    public String checkFormulaMachineUnique(FormulaMachine formulaMachine) {
        if (formulaMachine == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<FormulaMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", formulaMachine.getMixArea());
        queryWrapper.eq("GLUE", formulaMachine.getGlue());
        // 编辑不校验
//        if (formulaMachine.getId() != null) {
//            queryWrapper.ne("ID", formulaMachine.getId());  //编辑的时候校验，要过滤掉自身的id
//        }

        List<FormulaMachine> list = formulaMachineMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入配方与机台对应数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<FormulaMachine> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum;
        List<FormulaMachine> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表

        try {

            // 取机台编号
            List<MesPmtRecipe> machineList = mesPmtRecipeService.selectMesPmtRecipeMachine(new MesPmtRecipe());
            Map<String, MesPmtRecipe> machineMap = CollectionUtil.toMap(machineList, obj -> (
                    GenerageMapKeyUtils.createMapKey(obj.getMixArea(), obj.getRecipeMaterialName(), obj.getMachineName())
            ));

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getGlue()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                FormulaMachine formulaMachine = list.get(i);
                //excel中重复记录校验
                String glue = formulaMachine.getGlue();
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(formulaMachine.getMixArea(), glue));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    formulaMachine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.formulaMachine.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, formulaMachine); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && formulaMachine.getId() == null) {
                    formulaMachine.setBaseValue(null);

                    //拆分机台信息，将机台名称转换为机台编号
                    String mixArea = formulaMachine.getMixArea();
                    String[] machineNameList = formulaMachine.getMachineName().split(",");

                    List<FormulaMachine> formulaMachines = new ArrayList<>();
                    int defaultOrder = 0;
                    for (String machineName : machineNameList) {
                        MesPmtRecipe mesPmtRecipe = machineMap.get(GenerageMapKeyUtils.createMapKey(mixArea, glue, machineName));
                        if (mesPmtRecipe != null) {
                            FormulaMachine machine = new FormulaMachine();
                            BeanUtils.copyProperties(formulaMachine, machine);
                            machine.setMachineCode(mesPmtRecipe.getRecipeEquipCode());
                            machine.setMachineOrder(String.valueOf(defaultOrder += 10));
                            formulaMachines.add(machine);
                        } else {
                            //添加错误日志
                            formulaMachine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                            String message = String.format(I18nUtil.getMessage("setting.formulaMachine.machineNotExist"), mixArea, glue, machineName);
                            addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        }
                    }
                    if (formulaMachines.size() == machineNameList.length) {
                        importList.addAll(formulaMachines);
                        successNum++;
                    }
                } else {
                    formulaMachine.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }

            }

            // 批量删除和新增
            if (CollectionUtils.isNotEmpty(importList)) {
                formulaMachineMapper.trueBatchDeleteFormulaMachineInfo(importList);
                formulaMachineMapper.batchInsertFormulaMachineInfo(importList);
            }
        } catch (Exception e) {
            successNum = 0;
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        //由于机台名称有多个
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public List<FormulaMachine> selectExactFormulaMachineList(FormulaMachine machine) {

        List<FormulaMachine> formulaMachines = formulaMachineMapper.selectExactFormulaMachineList(machine);

        return formulaMachines;
    }

    /**
     * 根据密炼区和胶料名称查询配方与机台对应信息
     * @param machine 参数
     * @return 查询到的集合
     */
    @Override
    public ArrayList<FormulaMachine> getFormulaMachineList(FormulaMachine machine) {
        return formulaMachineMapper.getFormulaMachineList(machine);
    }
    

    /**
     * 根据机台名称和胶料名称进行精确查询
     *
     * @param machine
     * @return
     */
    @Override
    public ArrayList<FormulaMachine> selectRecipeMachineList(FormulaMachine machine) {
    	return formulaMachineMapper.selectRecipeMachineList(machine);
    }
}
