package com.zlt.aps.tm.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.dto.TmMouthPlateDto;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.entity.TmMouthPlate;
import com.zlt.aps.tm.mapper.TmMouthPlateMapper;
import com.zlt.aps.tm.service.TmMachineInfoService;
import com.zlt.aps.tm.service.TmMouthPlateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 胎面口型板信息维护 服务实现类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@Service
public class TmMouthPlateServiceImpl extends ServiceImpl<TmMouthPlateMapper, TmMouthPlate> implements TmMouthPlateService {

    @Autowired
    private TmMouthPlateMapper tmMouthPlateMapper;
    @Autowired
    private TmMachineInfoService tmMachineInfoService;

    /**
     * 查询胎面口型板信息维护列表
     *
     * @param tmMouthPlate 胎面口型板信息维护
     * @return 胎面口型板信息维护集合
     */
    @Override
    public List<TmMouthPlateDto> selectMouthPlateList(TmMouthPlate tmMouthPlate) {
        return tmMouthPlateMapper.selectMouthPlateWithMachineInfo(tmMouthPlate);
    }

    /**
     * 查询胎面口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎面口型板信息维护集合
     */
    @Override
    public TmMouthPlate selectTmMouthPlateById(Long id) {
        LambdaQueryWrapper<TmMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMouthPlate::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(TmMouthPlate::getId, id);
        return tmMouthPlateMapper.selectOne(wrapper);
    }

    /**
     * 新增胎面口型板信息维护
     *
     * @param tmMouthPlate 胎面口型板信息维护
     */
    @Override
    public void saveTmMouthPlate(TmMouthPlate tmMouthPlate) {
        if (ObjectUtils.allNotNull(tmMouthPlate.getMouthPlateCode(), tmMouthPlate.getMachineId()) && tmMouthPlateMapper.checkUnique(tmMouthPlate) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.unique"));
        }
        tmMouthPlate.setBaseVale(tmMouthPlate.getId());
        saveOrUpdate(tmMouthPlate);
    }

    /**
     * 批量删除胎面口型板信息维护
     *
     * @param ids 需要删除的胎面口型板信息维护ID
     */
    @Override
    public void deleteTmMouthPlateByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        List<TmMouthPlate> list = new ArrayList<>();
        for (Long id : ids) {
            TmMouthPlate tmMouthPlate = new TmMouthPlate();
            tmMouthPlate.setId(id);
            tmMouthPlate.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            tmMouthPlate.setBaseVale(tmMouthPlate.getId());
            list.add(tmMouthPlate);
        }
        updateBatchById(list);
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<TmMouthPlateDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;

        List<TmMouthPlateDto> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmMachineInfo> machineInfoList = tmMachineInfoService.selectMachineInfoList(new TmMachineInfo());
        //将机台名称转换为机台id，并做校验
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineCode, TmMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineName, TmMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMouthPlateCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TmMouthPlateDto mouthPlate = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(mouthPlate.getMouthPlateCode()+mouthPlate.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                mouthPlate.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.mouthPlateCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            //基础校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, mouthPlate);
            String machineName = mouthPlate.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtil.isNotBlank(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                // 设置导入默认状态，机台id，更新人，更新时间
                mouthPlate.setMachineId(machineId);
                mouthPlate.setBaseVale(null);
                importList.add(mouthPlate);
            } else {
                failureNum++;
                mouthPlate.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tmMouthPlateMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmMouthPlateDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TmMouthPlateDto dto = tmMouthPlateMapper.selectByCodeAndMachineId(excelItem);
                    if (dto == null) {
                        //不存在插入
                        successNum++;
                        tmMouthPlateMapper.insertList(Collections.singletonList(excelItem));
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.mouthPlate.message.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.tmMouthPlateMapper.deleteAll();
    }
}
