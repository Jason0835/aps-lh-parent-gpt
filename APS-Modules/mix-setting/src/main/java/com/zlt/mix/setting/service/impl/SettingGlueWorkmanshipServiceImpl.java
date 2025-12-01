package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.SettingGlueWorkmanship;
import com.zlt.mix.setting.mapper.SettingGlueWorkmanshipMapper;
import com.zlt.mix.setting.service.SettingGlueWorkmanshipService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分厂胶料工艺信息Service业务层处理
 *
 * @author Liam
 * @date 2022-03-18
 */
@Service
public class SettingGlueWorkmanshipServiceImpl extends ServiceImpl<SettingGlueWorkmanshipMapper, SettingGlueWorkmanship> implements SettingGlueWorkmanshipService {

    /**
     * 获取分厂胶料工艺信息列表
     *
     * @param entity 分厂胶料工艺信息
     * @return 分厂胶料工艺信息列表
     */
    @Override
    public List<SettingGlueWorkmanship> selectSettingGlueWorkmanshipList(SettingGlueWorkmanship entity) {
        return baseMapper.selectSettingGlueWorkmanshipList(entity);
    }

    /**
     * 保存分厂胶料工艺信息（id为空则新增，id不为空则修改）
     *
     * @param entity 分厂胶料工艺信息
     */
    @Override
    public void saveGlueWorkmanship(SettingGlueWorkmanship entity) {
        if (ObjectUtils.allNotNull(entity.getFactoryCode(), entity.getGlue()) && checkUnique(entity) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("setting.workmanship.database.unique"));
        }
        entity.setBaseValue(entity.getId());
        saveOrUpdate(entity);
    }

    /**
     * 批量逻辑删除分厂胶料工艺信息
     *
     * @param ids 需要进行逻辑删除的id数组
     * @return 逻辑删除的数目
     */
    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 导入分厂胶料工艺信息
     *
     * @param list          分厂胶料工艺信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的ID
     * @return 操作消息
     */
    @Override
    public AjaxResult importData(List<SettingGlueWorkmanship> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;

        //检验成功的对象和错误日志对象
        List<SettingGlueWorkmanship> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogList = new ArrayList<>();

        //数据库的字段重复和结果映射
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {

            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = baseMapper.listFormulaInfoNotUnique(list, importLogId, I18nUtil.getMessage("setting.formulaInfo.database.unique"), SecurityUtils.getUsername());
                importErrorLogList.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            }

            //按照业务主键进行分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getFactoryCode() + a.getGlue()), Collectors.counting()));

            //提取正确数据和错误记录
            for (int i = 0; i < list.size(); i++) {
                SettingGlueWorkmanship entity = list.get(i);

                //检测重复记录
                Long hasValue = groupMap.get(entity.getFactoryCode() + entity.getGlue());
                if (hasValue > 1) {
                    entity.setId(-999L);
                    ImportUtil.addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("setting.workmanship.excel.unique"), importErrorLogList);
                    continue;
                }

                //违反数据库唯一键的记录
                if (codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    entity.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                //检测当前行的数据的正确性
                List<ImportErrorLog> errorLogs = ImportUtil.validated(importLogId, i + 2, entity);
                if (CollectionUtils.isEmpty(errorLogs) && entity.getId() == null) {
                    entity.setBaseValue(null);
                    importList.add(entity);
                } else {
                    entity.setId(-999L);
                    importErrorLogList.addAll(errorLogs);
                }
            }

            //勾选更新，调用merge
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                baseMapper.mergeSql(importList);
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                //批量插入
                baseMapper.batchInsertFormulaInfo(importList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            failureNum = list.size();
            importErrorLogList.clear();
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogList);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogList);
        }

        successNum = importList.size();
        failureNum = list.size() - successNum;
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 检查记录重复次数
     *
     * @param entity 分厂胶料工艺信息
     * @return 重复记录的数量
     */
    public Long checkUnique(SettingGlueWorkmanship entity) {
        LambdaQueryWrapper<SettingGlueWorkmanship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettingGlueWorkmanship::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(SettingGlueWorkmanship::getGlue, entity.getGlue());
        if (entity.getId() != null) {
            wrapper.ne(SettingGlueWorkmanship::getId, entity.getId());
        }
        return baseMapper.selectCount(wrapper);
    }
}
