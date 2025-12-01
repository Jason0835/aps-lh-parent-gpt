package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.mapper.MixMachineMapper;
import com.zlt.mix.setting.service.MixMachineService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 密炼机台信息Service业务层处理
 * 
 * @author Gim
 * @date 2022-03-22
 */
@Service
public class MixMachineServiceImpl extends ServiceImpl<MixMachineMapper, MixMachine> implements MixMachineService {
    @Resource
    private MixMachineMapper mixMachineMapper;

    /**
     * 查询密炼机台信息列表
     * 
     * @param mixMachine 密炼机台信息
     * @return 密炼机台信息
     */
    @Override
    public List<MixMachine> selectMixMachineList(MixMachine mixMachine) {
        return mixMachineMapper.selectMixMachineList(mixMachine);
    }

    /**
     * 保存密炼机台信息信息（id为空则新增，id不为空则修改）
     *
     * @param mixMachine
     */
    @Override
    public void saveMixMachine(MixMachine mixMachine) {
        if (ZltConstant.NOT_UNIQUE.equals(checkMixMachineUnique(mixMachine))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.machine.database.unique1"));
        }
        if (ZltConstant.NOT_UNIQUE.equals(checkMixMachineUnique2(mixMachine))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.machine.database.unique2"));
        }
        mixMachine.setBaseValue(mixMachine.getId());
        this.saveOrUpdate(mixMachine);
    }

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的密炼机台信息ID
     * @return 结果
     */
    @Override
    public int deleteMixMachineByIds(Long[] ids)
    {
        return mixMachineMapper.deleteMixMachineByIds(ids);
    }


    /**
     * 校验密炼机台信息唯一性
     */
    @Override
    public String checkMixMachineUnique(MixMachine mixMachine) {
        if (mixMachine == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<MixMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", mixMachine.getMixArea());
        queryWrapper.eq("MACHINE_CODE", mixMachine.getMachineCode());
        if (mixMachine.getId() != null) {
            queryWrapper.ne("ID", mixMachine.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MixMachine> list = mixMachineMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }
    /**
     * 校验密炼机台信息唯一性
     */
    @Override
    public String checkMixMachineUnique2(MixMachine mixMachine) {
        if (mixMachine == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<MixMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", mixMachine.getMixArea());
        queryWrapper.eq("MACHINE_NAME", mixMachine.getMachineName());
        if (mixMachine.getId() != null) {
            queryWrapper.ne("ID", mixMachine.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<MixMachine> list = mixMachineMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入密炼机台信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixMachine> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixMachine> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键
        Map<Integer, Long> codeUniqueErrorMap2 = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.mixMachineMapper.listMixMachineNotUnique(list, importLogId, I18nUtil.getMessage("setting.machine.database.unique1"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.mixMachineMapper.listMixMachineNotUnique2(list, importLogId, I18nUtil.getMessage("setting.machine.database.unique2"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap2 = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            } else if (CollectionUtils.isNotEmpty(list)) {
                //检测是否存在仅有密炼区和机台名称冲突，而机台编号不冲突的记录
                codeUniqueErrorLogs = this.mixMachineMapper.listMixMachineNotUnique3(list, importLogId, I18nUtil.getMessage("setting.machine.database.unique2"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMachineCode()), Collectors.counting()));
            Map<String, Long> groupMap2 = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMachineName()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                MixMachine mixMachine = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(mixMachine.getMixArea(), mixMachine.getMachineCode()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    mixMachine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.machine.excel.unique1");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }
                Long hasValue2 = groupMap2.get(GenerageMapKeyUtils.createMapKey(mixMachine.getMixArea(), mixMachine.getMachineName()));
                if (hasValue2 > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    mixMachine.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.machine.excel.unique2");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    mixMachine.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }
                //违反数据库唯一键的记录
                if(codeUniqueErrorMap2.containsKey(i + 2)) {
                    //数据已经系统中存在
                    mixMachine.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, mixMachine); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && mixMachine.getId() == null) {
                    mixMachine.setBaseValue(null);
                    importList.add(mixMachine);
                } else {
                    mixMachine.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                mixMachineMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                mixMachineMapper.batchInsertMixMachineInfo(importList);  //批量插入
            }
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    @Override
    public ArrayList<MixMachine> getAllMachineInfo() {
        return mixMachineMapper.getAllMachineInfo();
    }
}
