package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;
import com.zlt.mix.setting.mapper.SettingFormulaInfoMapper;
import com.zlt.mix.setting.service.SettingFormulaInfoService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配方信息 Service业务层处理
 *
 * @author Liam
 * @date 2022-03-22
 */
@Service
public class SettingFormulaInfoServiceImpl extends ServiceImpl<SettingFormulaInfoMapper, SettingFormulaInfo> implements SettingFormulaInfoService {

    /**
     * 查询配方信息列表
     *
     * @param entity 配方信息
     * @return 配方信息列表
     */
    @Override
    public List<SettingFormulaInfo> selectSettingFormulaInfoList(SettingFormulaInfo entity) {
        return baseMapper.selectSettingFormulaInfoList(entity);
    }

    /**
     * 保存配方信息（id为空则新增，id不为空则修改）
     *
     * @param entity 配方信息
     */
    @Override
    public void saveSettingFormulaInfo(SettingFormulaInfo entity) {
        if (ObjectUtils.allNotNull(entity.getGlue()) && ZltConstant.NOT_UNIQUE.equals(checkGlueUnique(entity))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.formulaInfo.database.unique"));
        }
        entity.setBaseValue(entity.getId());
        saveOrUpdate(entity);
    }


    /**
     * 批量删除配方信息
     *
     * @param ids 配方信息的ID数组
     * @return 成功删除的条数
     */
    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteBatchByIds(Arrays.asList(ids));
    }

    /**
     * 导入配方信息
     *
     * @param list          配方信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的id
     * @return 操作消息
     */
    @Override
    public AjaxResult importData(List<SettingFormulaInfo> list, boolean updateSupport, Long importLogId) {

        int successNum = 0;
        int failureNum = 0;

        //检验成功的对象和错误日志对象
        List<SettingFormulaInfo> importList = new ArrayList<>();
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
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(SettingFormulaInfo::getGlue, Collectors.counting()));

            //提取正确数据和错误记录
            for (int i = 0; i < list.size(); i++) {
                SettingFormulaInfo entity = list.get(i);

                //检测重复记录
                Long hasValue = groupMap.get(entity.getGlue());
                if (hasValue > 1) {
                    entity.setId(-999L);
                    ImportUtil.addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("setting.formulaInfo.excel.unique"), importErrorLogList);
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

    @Override
    public String checkGlueUnique(SettingFormulaInfo entity) {
        if (entity == null || StringUtils.isBlank(entity.getGlue())) {
            return ZltConstant.NOT_UNIQUE;
        }
        LambdaQueryWrapper<SettingFormulaInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettingFormulaInfo::getGlue, entity.getGlue());
        if (entity.getId() != null) {
            wrapper.ne(SettingFormulaInfo::getId, entity.getId());
        }
        Long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

}
