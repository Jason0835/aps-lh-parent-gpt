package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.mapper.CxMachineInfoMapper;
import com.zlt.aps.cx.service.CxMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型机台信息维护功能逻辑层
 */
@Service
public class CxMachineInfoServiceImpl extends ServiceImpl<CxMachineInfoMapper, CxMachineInfo> implements CxMachineInfoService {

    @Autowired
    private CxMachineInfoMapper cxMachineInfoMapper;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;


    @Override
    public CxMachineInfo selectCxMachineInfoById(Long id) {
        return cxMachineInfoMapper.selectCxMachineInfoById(id);
    }

    @Override
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo) {
        return cxMachineInfoMapper.selectCxMachineInfoList(cxMachineInfo);
    }

    @Override
    public List<CxMachineInfo> listOrderByName(CxMachineInfo cxMachineInfo) {
        return cxMachineInfoMapper.listOrderByName(cxMachineInfo);
    }

    @Override
    public List<CxMachineInfo> selectCxMachineInfoList2(CxMachineInfo cxMachineInfo) {
        return cxMachineInfoMapper.selectCxMachineInfoList2(cxMachineInfo);
    }

    /**
     * 获取其他半部件机台列表
     *
     * @param cxMachineInfo
     * @return
     */
    @Override
    public List<CxMachineInfo> getOrtherMachineInfo(CxMachineInfo cxMachineInfo) {
        return cxMachineInfoMapper.getOrtherMachineInfo(cxMachineInfo);
    }

    @Override
    public int insertCxMachineInfo(CxMachineInfo cxMachineInfo) {
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxMachineInfoMap();
        cxMachineInfo.setBaseVale(null);
        return cxMachineInfoMapper.insertCxMachineInfo(cxMachineInfo);
    }

    @Override
    public int updateCxMachineInfo(CxMachineInfo cxMachineInfo) {
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxMachineInfoMap();

        cxMachineInfo.setBaseVale(cxMachineInfo.getId());
        return cxMachineInfoMapper.updateCxMachineInfo(cxMachineInfo);
    }

    @Override
    public int deleteCxMachineInfoByIds(Long[] ids) {
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxMachineInfoMap();
        return cxMachineInfoMapper.deleteCxMachineInfoByIds(ids);
    }

    @Override
    public int deleteCxMachineInfoById(Long id) {
        //Joran.zhang 2021-07-01 清空缓存，触发引擎重新加载缓存
        cxEngineQuotaCommonService.delCacheCxMachineInfoMap();
        return cxMachineInfoMapper.deleteCxMachineInfoById(id);
    }

    @Override
    public String checkMachineCodeUnique(CxMachineInfo cxMachineInfo) {
        List<CxMachineInfo> list = cxMachineInfoMapper.checkMachineCodeUnique(cxMachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxMachineInfo> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMachineInfo> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineCode(), Collectors.counting()));
        //机台名称分组
        Map<String, Long> nameMap = list.stream().collect(Collectors.groupingBy(a -> a.getMachineName(), Collectors.counting()));


        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxMachineInfo dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getMachineCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message = String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            if (dto.getDimensionMiniMum() != null && dto.getDimensionMaxiMum() != null && dto.getDimensionMiniMum().compareTo(dto.getDimensionMaxiMum()) > 0) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.machine.dimensionMaximumBigThanDimensionMinmum"), validated);
            }

            if (org.apache.commons.lang.StringUtils.isNotBlank(dto.getClassShift()) && dto.getClassShift().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.ClassShiftValidate");
                message = String.format(message, i + 2, I18nUtil.getMessage("ui.data.column.machine.classShift"));
                addImportErrorLog(importLogId, i + 2, message, validated);
            }

            //校验Excel机台名称唯一性
            Long hasNameValue = nameMap.get(dto.getMachineName());
            if (hasNameValue > 1) {
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord4Name");
                addImportErrorLog(importLogId, i + 2, message, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {

                // 唯一性校验
                Boolean hasFalse = false;
                CxMachineInfo query = new CxMachineInfo();
                if (updateSupport) { //勾选更新时只校验机台名称
                    query.setMachineCode(dto.getMachineCode());
                    query.setMachineName(dto.getMachineName());
                    List<CxMachineInfo> exist2 = cxMachineInfoMapper.checkMachineNameUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                } else { //不勾选更新时两个都校验
                    query.setMachineCode(dto.getMachineCode());
                    List<CxMachineInfo> exist1 = cxMachineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist1)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machine.message"), importErrorLogs);
                    }

                    query.setMachineCode(null);
                    query.setMachineName(dto.getMachineName());
                    List<CxMachineInfo> exist2 = cxMachineInfoMapper.checkMachineCodeUnique(query);
                    if (CollectionUtils.isNotEmpty(exist2)) {
                        hasFalse = true;
                        addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.cx.machineName.message"), importErrorLogs);
                    }
                }
                if (hasFalse) {
                    dto.setId(-999L);
                    failureNum++;
                    continue;
                }

                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    cxMachineInfoMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxMachineInfo newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        newItem.setBaseVale(null);

                        successNum++;
                        cxMachineInfoMapper.insertCxMachineInfo(newItem);
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
        cxEngineQuotaCommonService.delCacheCxMachineInfoMap();
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
