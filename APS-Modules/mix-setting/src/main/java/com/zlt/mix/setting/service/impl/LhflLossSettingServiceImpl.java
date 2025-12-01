package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;
import com.zlt.mix.setting.mapper.LhflLossSettingMapper;
import com.zlt.mix.setting.service.LhflLossSettingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫磺辅料耗损率设定Service业务层处理
 *
 * @author Joran.zhang
 * @date 2022-05-23
 */
@Service
public class LhflLossSettingServiceImpl extends ServiceImpl<LhflLossSettingMapper, LhflLossSetting> implements LhflLossSettingService {
    @Resource
    private LhflLossSettingMapper lhflLossrateSettingMapper;

    /**
     * 查询硫磺辅料耗损率设定列表
     *
     * @param lhflLossrateSetting 硫磺辅料耗损率设定
     * @return 硫磺辅料耗损率设定
     */
    @Override
    public List<LhflLossSetting> selectLhflLossSettingList(LhflLossSetting lhflLossrateSetting) {
        return lhflLossrateSettingMapper.selectLhflLossSettingList(lhflLossrateSetting);
    }

    /**
     * 保存硫磺辅料耗损率设定信息（id为空则新增，id不为空则修改）
     *
     * @param lhflLossrateSetting
     */
    @Override
    public void saveLhflLossSetting(LhflLossSetting lhflLossrateSetting) {
        if (ZltConstant.NOT_UNIQUE.equals(checkLhflLossSettingUnique(lhflLossrateSetting))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.lhflLossSetting.database.unique" ));
        }
        lhflLossrateSetting.setBaseValue(lhflLossrateSetting.getId());
        this.saveOrUpdate(lhflLossrateSetting);
    }

    /**
     * 批量删除硫磺辅料耗损率设定
     *
     * @param ids 需要删除的硫磺辅料耗损率设定ID
     * @return 结果
     */
    @Override
    public int deleteLhflLossSettingByIds(Long[] ids)
    {
        return lhflLossrateSettingMapper.deleteLhflLossSettingByIds(ids);
    }


    /**
     * 校验硫磺辅料耗损率设定唯一性
     */
    @Override
    public String checkLhflLossSettingUnique(LhflLossSetting lhflLossrateSetting) {
        if (lhflLossrateSetting == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<LhflLossSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("mix_area", lhflLossrateSetting.getMixArea());
        if (StringUtils.isEmpty(lhflLossrateSetting.getMaterialName())) {
            queryWrapper.isNull("MATERIAL_NAME");
        } else {
            queryWrapper.eq("MATERIAL_NAME", lhflLossrateSetting.getMaterialName());
        }
        if (StringUtils.isNotEmpty(lhflLossrateSetting.getMachineCode())) {
            queryWrapper.eq("machine_code", lhflLossrateSetting.getMachineCode());
        } else {
            queryWrapper.isNull("machine_code");
        }
        if (lhflLossrateSetting.getId() != null) {
            queryWrapper.ne("ID", lhflLossrateSetting.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<LhflLossSetting> list = lhflLossrateSettingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入硫磺辅料耗损率设定数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhflLossSetting> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhflLossSetting> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {

            //需要在插入之前通过机台名称和密炼区获取机台编号
            List<String> machineCodes = this.lhflLossrateSettingMapper.selectMachineCodeList(list);
            //设置机台编号
            for (int i = 0; i < machineCodes.size(); i++) {
                String machineCode = machineCodes.get(i);
                LhflLossSetting lhflLossSetting = list.get(i);
                //机台编号不存在，且机台名称存在
                if (com.alibaba.nacos.common.utils.StringUtils.isEmpty(machineCode) && !StringUtils.isEmpty(lhflLossSetting.getMachineName())) {
                    lhflLossSetting.setId(-999L);
                    String message = I18nUtil.getMessage("setting.lhflLossSetting.machine.exists");
                    addImportErrorLog(importLogId, i + 2, String.format(message, lhflLossSetting.getMixArea(), lhflLossSetting.getMachineName()), importErrorLogs);
                }
                lhflLossSetting.setMachineCode(machineCode);
            }

            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.lhflLossrateSettingMapper.listLhflLossSettingNotUnique(list, importLogId, I18nUtil.getMessage("setting.lhflLossSetting.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getMixArea(), a.getMachineCode(), a.getMaterialName()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                LhflLossSetting lhflLossrateSetting = list.get(i);
                //exce中重复记录校验
                String key = GenerageMapKeyUtils.createMapKey(lhflLossrateSetting.getMixArea(), lhflLossrateSetting.getMachineCode(), lhflLossrateSetting.getMaterialName());

                Long hasValue = groupMap.get(key);
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    lhflLossrateSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.lhflLossSetting.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    lhflLossrateSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lhflLossrateSetting); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && lhflLossrateSetting.getId() == null) {
                    lhflLossrateSetting.setBaseValue(null);
                    importList.add(lhflLossrateSetting);
                } else {
                    lhflLossrateSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                lhflLossrateSettingMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                lhflLossrateSettingMapper.batchInsertLhflLossSettingInfo(importList);  //批量插入
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
}
