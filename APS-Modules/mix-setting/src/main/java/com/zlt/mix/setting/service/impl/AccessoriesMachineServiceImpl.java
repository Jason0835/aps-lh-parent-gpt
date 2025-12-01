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
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.mapper.AccessoriesMachineMapper;
import com.zlt.mix.setting.service.AccessoriesMachineService;
import com.zlt.mix.setting.service.MesPmtRecipeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫磺辅料与机台对应Service业务层处理
 *
 * @author Liam
 * @date 2022-04-18
 */
@Service
public class AccessoriesMachineServiceImpl extends ServiceImpl<AccessoriesMachineMapper, AccessoriesMachine> implements AccessoriesMachineService {
    @Resource
    private AccessoriesMachineMapper accessoriesMachineMapper;

    @Autowired
    private MesPmtRecipeService mesPmtRecipeService;

    /**
     * 查询硫磺辅料与机台对应列表
     *
     * @param accessoriesMachine 硫磺辅料与机台对应
     * @return 硫磺辅料与机台对应
     */
    @Override
    public List<AccessoriesMachine> selectAccessoriesMachineList(AccessoriesMachine accessoriesMachine) {
        //将机台编号转换为机台名称并使用逗号拼接
        return accessoriesMachineMapper.selectAccessoriesMachineList(accessoriesMachine);
    }

    /**
     * 保存硫磺辅料与机台对应信息（id为空则新增，id不为空则修改）
     *
     * @param accessoriesMachine
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAccessoriesMachine(AccessoriesMachine accessoriesMachine) {
        //删除旧机台（物理删除）
        baseMapper.trueDeleteByMixAreaAndGlue(accessoriesMachine.getMixArea(), accessoriesMachine.getMaterialName());

        //拆分机台名称列表
        accessoriesMachine.setBaseValue(null);
        List<MachineOrderDto> machineOrderList = accessoriesMachine.getMachineOrderList();
        LinkedList<AccessoriesMachine> list = new LinkedList<>();
        for (MachineOrderDto machineOrderDto : machineOrderList) {
            AccessoriesMachine machine = new AccessoriesMachine();
            BeanUtils.copyProperties(accessoriesMachine, machine);
            machine.setMachineCode(machineOrderDto.getMachineCode());
            machine.setMachineOrder(machineOrderDto.getMachineOrder().toString());
            list.add(machine);
        }
        if (CollectionUtils.isNotEmpty(list)) {
            baseMapper.batchInsertAccessoriesMachineInfo(list);
        }

    }

    /**
     * 批量删除硫磺辅料与机台对应
     *
     * @param ids 需要删除的硫磺辅料与机台对应ID
     * @return 结果
     */
    @Override
    public int deleteAccessoriesMachineByIds(Long[] ids) {
        //批量删除ID对应的（密炼区+胶料名称）的记录
        return accessoriesMachineMapper.deleteAccessoriesMachineByIds(ids);
    }


    /**
     * 校验硫磺辅料与机台对应唯一性
     */
    @Override
    public String checkAccessoriesMachineUnique(AccessoriesMachine accessoriesMachine) {
        if (accessoriesMachine == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<AccessoriesMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", accessoriesMachine.getMixArea());
        queryWrapper.eq("MATERIAL_NAME", accessoriesMachine.getMaterialName());
        if (accessoriesMachine.getId() != null) {
            queryWrapper.ne("ID", accessoriesMachine.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<AccessoriesMachine> list = accessoriesMachineMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入硫磺辅料与机台对应数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<AccessoriesMachine> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<AccessoriesMachine> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表

        try {

            //取机台编号
            List<MesPmtRecipe> lhflMachines = mesPmtRecipeService.selectMesPmtRecipeMachine(new MesPmtRecipe());
            Map<String, MesPmtRecipe> lhflMachineMap = CollectionUtil.toMap(lhflMachines, obj -> (
                    GenerageMapKeyUtils.createMapKey(obj.getMixArea(), obj.getRecipeMaterialName(), obj.getMachineName())
            ));

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMaterialName()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                AccessoriesMachine accessoriesMachine = list.get(i);
                //exce中重复记录校验
                String materialName = accessoriesMachine.getMaterialName();
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(accessoriesMachine.getMixArea(), materialName));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    accessoriesMachine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.accessoriesMachine.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, accessoriesMachine); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && accessoriesMachine.getId() == null) {
                    accessoriesMachine.setBaseValue(null);

                    //拆分机台名称，分别转为机台编号
                    String mixArea = accessoriesMachine.getMixArea();
                    String[] machineNameList = accessoriesMachine.getMachineName().split(",");

                    List<AccessoriesMachine> accessoriesMachines = new ArrayList<>();
                    int defaultOrder = 0;
                    for (String machineName : machineNameList) {
                        MesPmtRecipe mesPmtRecipe = lhflMachineMap.get(GenerageMapKeyUtils.createMapKey(mixArea, materialName, machineName));
                        if (mesPmtRecipe != null) {
                            AccessoriesMachine machine = new AccessoriesMachine();
                            BeanUtils.copyProperties(accessoriesMachine, machine);
                            machine.setMachineCode(mesPmtRecipe.getRecipeEquipCode());
                            machine.setMachineOrder(String.valueOf(defaultOrder += 10));
                            accessoriesMachines.add(machine);
                        } else {
                            //添加机台信息不存在的错误日志
                            String message = String.format(I18nUtil.getMessage("setting.accessoriesMachine.machineNoExist"), mixArea, materialName, machineName);
                            addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        }
                    }
                    if (accessoriesMachines.size() == machineNameList.length) {
                        importList.addAll(accessoriesMachines);
                        successNum++;
                    }

                } else {
                    accessoriesMachine.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //批量删除和批量新增
            if (CollectionUtils.isNotEmpty(importList)) {
                accessoriesMachineMapper.trueBatchDeleteAccessoriesMachineInfo(importList);
                accessoriesMachineMapper.batchInsertAccessoriesMachineInfo(importList);
            }
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        //由于机台名称可以有多个
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public List<AccessoriesMachine> selectExactAccessoriesMachineList(AccessoriesMachine accessoriesMachine) {
        List<AccessoriesMachine> list = accessoriesMachineMapper.selectExactAccessoriesMachineList(accessoriesMachine);
        //将机台编号转换为机台名称并使用逗号拼接ed
        return list;
    }

    /**
     * 根据密炼区和胶料名称查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    @Override
    public ArrayList<AccessoriesMachine> getAccessoriesMachineList(AccessoriesMachine accessoriesMachine) {
        return accessoriesMachineMapper.getAccessoriesMachineList(accessoriesMachine);
    }

    /**
     * 根据机台编号获取机台名称
     */
    public List<AccessoriesMachine> machineCodeToMachineName(List<AccessoriesMachine> accessoriesMachines, AccessoriesMachine accessoriesMachine) {

        // 取机台名称
        MesPmtRecipe params = new MesPmtRecipe();
        String accessoriesMachineMixArea = accessoriesMachine.getMixArea();
        if (StringUtils.isNotBlank(accessoriesMachineMixArea)) {
            params.setMixArea(accessoriesMachineMixArea);
        }
        String materialName = accessoriesMachine.getMaterialName();
        if (StringUtils.isNotBlank(materialName) && accessoriesMachines.size() == 1) {
            params.setRecipeMaterialName(materialName);
        }
        List<MesPmtRecipe> machineList = mesPmtRecipeService.selectMesPmtRecipeMachine(params);
        Map<String, MesPmtRecipe> machineMap = CollectionUtil.toMap(machineList, obj -> (
                GenerageMapKeyUtils.createMapKey(obj.getMixArea(), obj.getRecipeEquipCode())
        ));

        // 密炼区的字典通过ExcelUtil自动转换
        //（密炼区+机台编号）转换成机台名称
        for (AccessoriesMachine machine : accessoriesMachines) {
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
        return accessoriesMachines;
    }

    /**
     * 根据密炼区和胶料名称精确查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    public ArrayList<AccessoriesMachine> listRecipeMachine(AccessoriesMachine accessoriesMachine) {
    	return accessoriesMachineMapper.selectRecipeMachineList(accessoriesMachine);
    }
}
