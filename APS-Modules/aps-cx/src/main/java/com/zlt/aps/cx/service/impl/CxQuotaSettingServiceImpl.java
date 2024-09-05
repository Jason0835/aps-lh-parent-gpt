package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import com.zlt.aps.cx.entity.CxQuotaSetting;
import com.zlt.aps.cx.mapper.CxQuotaSettingMapper;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.CxQuotaSettingService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型定额设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-16
 */
@Service
public class CxQuotaSettingServiceImpl extends ServiceImpl<CxQuotaSettingMapper, CxQuotaSetting> implements CxQuotaSettingService {
    @Autowired
    private CxQuotaSettingMapper cxQuotaSettingMapper;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private CxMachineInfoService cxMachineInfoService;

    /**
     * 查询成型定额设定列表
     *
     * @param quotaSetting 成型定额设定
     * @return 成型定额设定集合
     */
    @Override
    public List<CxQuotaSettingDto> selectCxQuotaSettingList(CxQuotaSetting quotaSetting) {
        return cxQuotaSettingMapper.selectCxQuotaSettingList(quotaSetting);
    }

    /**
     * 查询成型定额设定
     *
     * @param id 成型定额设定ID
     * @return 成型定额设定
     */
    @Override
    public CxQuotaSetting selectCxQuotaSettingById(Long id) {
        LambdaQueryWrapper<CxQuotaSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxQuotaSetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(CxQuotaSetting::getId, id);
        return cxQuotaSettingMapper.selectOne(wrapper);
    }

    /**
     * 修改成型定额设定
     *
     * @param quotaSetting 成型定额设定
     */
    @Override
    public void saveCxQuotaSetting(CxQuotaSetting quotaSetting) {
        Integer widthMinimum = quotaSetting.getSectionWidthMinimum();
        Integer widthMaximum = quotaSetting.getSectionWidthMaximum();
        if (ObjectUtils.allNotNull(widthMinimum, widthMaximum) && widthMinimum > widthMaximum) {
            log.error("输入的成型定额断面宽下限大于成型定额断面宽上限");
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.minMoreThanMax"));
        }
        // 唯一性校验，相同记录下限和上限部分不能有交集
        List<CxQuotaSettingDto> list = cxQuotaSettingMapper.checkUnique(quotaSetting);
        if (list.size() > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.cxQuota.unique"));
        }
        quotaSetting.setBaseVale(quotaSetting.getId());
        saveOrUpdate(quotaSetting);
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxQuotaSetting();
    }

    /**
     * 批量删除成型定额设定
     *
     * @param ids 需要删除的成型定额设定ID
     */
    @Override
    public void deleteCxQuotaSettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        cxQuotaSettingMapper.deleteQuotaSettingByIds(ids);
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxQuotaSetting();
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxQuotaSettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxQuotaSetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMachineType()+a.getSpecDimension()+a.getCarcassBothLayer()+a.getReinforce()+a.getTireType()+a.getSectionWidthMinimum()+a.getSectionWidthMaximum()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxQuotaSettingDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getMachineType()+dto.getSpecDimension()+dto.getCarcassBothLayer()+dto.getReinforce()+dto.getTireType()+dto.getSectionWidthMinimum()+dto.getSectionWidthMaximum());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineType");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cx.limit.specDimension");
                String columnName3 = I18nUtil.getMessage("ui.data.column.cx.setting.carcassBothLayer");
                String columnName4 = I18nUtil.getMessage("ui.data.column.cx.setting.reinforce");
                String columnName5 = I18nUtil.getMessage("ui.data.column.cx.setting.tireType");
                String columnName6 = I18nUtil.getMessage("ui.data.column.cx.setting.sectionWidthMinimum");
                String columnName7 = I18nUtil.getMessage("ui.data.column.cx.setting.sectionWidthMaximum");
                message=String.format(message,columnName+"+"+columnName2+"+"+columnName3+"+"+columnName4+"+"+columnName5+"+"+columnName6+"+"+columnName7);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                CxQuotaSetting newEntity = new CxQuotaSetting();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setBaseVale(null);
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    for (int i = 0; i < newList.size(); i++) {
                        CxQuotaSetting aa = newList.get(i);
                        List<CxQuotaSettingDto> exist = cxQuotaSettingMapper.checkUnique(aa);
                        if (CollectionUtils.isEmpty(exist)) {
                            saveOrUpdate(aa);
                        } else {
                            aa.setId(exist.get(0).getId());
                            aa.setCreateTime(exist.get(0).getCreateTime());
                            aa.setCreateBy(exist.get(0).getCreateBy());
                            aa.setUpdateBy(SecurityUtils.getUsername());
                            aa.setUpdateTime(new Date());
                            saveOrUpdate(aa);
                        }
                    }
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxQuotaSettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxQuotaSetting newItem = new CxQuotaSetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        Integer widthMinimum = newItem.getSectionWidthMinimum();
                        Integer widthMaximum = newItem.getSectionWidthMaximum();
                        if (ObjectUtils.allNotNull(widthMinimum, widthMaximum) && widthMinimum > widthMaximum) {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.minMoreThanMax"), importErrorLogs);
                            continue;
                        }

                        List<CxQuotaSettingDto> exist = cxQuotaSettingMapper.checkUnique(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            saveOrUpdate(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }

        //Joran.zhang 2021-11-26 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxQuotaSetting();
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
