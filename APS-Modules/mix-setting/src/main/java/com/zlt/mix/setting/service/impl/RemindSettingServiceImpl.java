package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.RemindSetting;
import com.zlt.mix.setting.mapper.RemindSettingMapper;
import com.zlt.mix.setting.service.RemindSettingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 提醒设备Service业务层处理
 *
 * @author Gim
 * @date 2022-03-23
 */
@Service
public class RemindSettingServiceImpl extends ServiceImpl<RemindSettingMapper, RemindSetting> implements RemindSettingService {
    @Resource
    private RemindSettingMapper remindSettingMapper;

    /**
     * 查询提醒设备列表
     *
     * @param remindSetting 提醒设备
     * @return 提醒设备
     */
    @Override
    public List<RemindSetting> selectRemindSettingList(RemindSetting remindSetting) {
        return remindSettingMapper.selectRemindSettingList(remindSetting);
    }

    /**
     * 保存提醒设备信息（id为空则新增，id不为空则修改）
     *
     * @param remindSetting
     */
    @Override
    public void saveRemindSetting(RemindSetting remindSetting) {
        if (ZltConstant.NOT_UNIQUE.equals(checkRemindSettingUnique(remindSetting))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.remindSetting.database.unique"));
        }

        remindSetting.setBaseValue(remindSetting.getId());
        if (remindSetting.getWordColor() == null || remindSetting.getWordColor().equals("")) {
            remindSetting.setWordColor("#000000");//无值设置默认黑色
        }
        if (remindSetting.getBackgroundColor() == null || remindSetting.getBackgroundColor().equals("")) {
            remindSetting.setBackgroundColor("#ffffff");//无值设置默认白色
        }
        this.saveOrUpdate(remindSetting);
    }

    /**
     * 批量删除提醒设备
     *
     * @param ids 需要删除的提醒设备ID
     * @return 结果
     */
    @Override
    public int deleteRemindSettingByIds(Long[] ids) {
        return remindSettingMapper.deleteRemindSettingByIds(ids);
    }


    /**
     * 校验提醒设备唯一性
     */
    @Override
    public String checkRemindSettingUnique(RemindSetting remindSetting) {
        if (remindSetting == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<RemindSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("FIELD_CODE", remindSetting.getFieldCode());
        queryWrapper.eq("FIELD_VALUE", remindSetting.getFieldValue());
        if (remindSetting.getId() != null) {
            queryWrapper.ne("ID", remindSetting.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<RemindSetting> list = remindSettingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入提醒设备数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<RemindSetting> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<RemindSetting> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.remindSettingMapper.listRemindSettingNotUnique(list, importLogId, I18nUtil.getMessage("setting.remindSetting.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> GenerageMapKeyUtils.createMapKey(a.getFieldCode(), a.getFieldValue()), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                RemindSetting remindSetting = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(remindSetting.getFieldCode(), remindSetting.getFieldValue()));
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    remindSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.remindSetting.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if (codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    remindSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, remindSetting); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && remindSetting.getId() == null) {
                    //文字颜色和背景颜色的默认值
                    if (StringUtils.isEmpty(remindSetting.getWordColor())) {
                        remindSetting.setWordColor("#000000");
                    }
                    if (StringUtils.isEmpty(remindSetting.getBackgroundColor())) {
                        remindSetting.setBackgroundColor("#ffffff");
                    }

                    remindSetting.setBaseValue(null);
                    importList.add(remindSetting);
                } else {
                    remindSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                remindSettingMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                remindSettingMapper.batchInsertRemindSettingInfo(importList);  //批量插入
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
