package com.zlt.aps.tc.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.entity.TcMouthPlate;
import com.zlt.aps.tc.mapper.TcMouthPlateMapper;
import com.zlt.aps.tc.service.TcMachineInfoService;
import com.zlt.aps.tc.service.TcMouthPlateService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 胎侧口型板信息维护 服务实现类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
@Service
public class TcMouthPlateServiceImpl extends ServiceImpl<TcMouthPlateMapper, TcMouthPlate> implements TcMouthPlateService {

    @Autowired
    private TcMouthPlateMapper tcMouthPlateMapper;

    @Autowired
    private TcMachineInfoService tcMachineInfoService;

    /**
     * 查询胎侧口型板信息维护列表
     *
     * @param mouthPlate 胎侧口型板信息维护
     * @return 胎侧口型板信息维护集合
     */
    @Override
    public List<TcMouthPlateDto> selectMouthPlateList(TcMouthPlate mouthPlate) {
        return tcMouthPlateMapper.selectMouthPlateWithMachineInfo(mouthPlate);
    }

    /**
     * 查询胎侧口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎侧口型板信息维护集合
     */
    @Override
    public TcMouthPlate selectTmMouthPlateById(Long id) {
        LambdaQueryWrapper<TcMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMouthPlate::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(TcMouthPlate::getId, id);
        return tcMouthPlateMapper.selectOne(wrapper);
    }

    /**
     * 新增胎侧口型板信息维护
     *
     * @param mouthPlate 胎侧口型板信息维护
     */
    @Override
    public void saveTmMouthPlate(TcMouthPlate mouthPlate) {
        if (ObjectUtils.allNotNull(mouthPlate.getMouthPlateCode(), mouthPlate.getMachineId()) && tcMouthPlateMapper.checkUnique(mouthPlate) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.unique"));
        }
        mouthPlate.setBaseVale(mouthPlate.getId());
        saveOrUpdate(mouthPlate);
    }

    /**
     * 批量删除胎侧口型板信息维护
     *
     * @param ids 需要删除的胎侧口型板信息维护ID
     */
    @Override
    public void deleteTmMouthPlateByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        LambdaUpdateWrapper<TcMouthPlate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcMouthPlateDto> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcMouthPlateDto> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<TcMachineInfo> machineInfoList = tcMachineInfoService.selectMachineInfoList(new TcMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            ImportUtil.addImportErrorLog(importLogId, null, errorMsg, importErrorLogs);
            return AjaxResult.error(errorMsg, importErrorLogs);
        }
        Map<String, Long> machineCodeMap = new HashMap<>();
//        machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
        machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getId()));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMouthPlateCode()+a.getMachineName()), Collectors.counting()));

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            TcMouthPlateDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getMouthPlateCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.mouthPlateCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = dto.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (machineId == null && StringUtil.isNotBlank(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                dto.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                dto.setMachineId(machineId);
                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcMouthPlateMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcMouthPlateDto entity = list.get(i);
                    // 错误跳过
                    if (entity.getId() != null && entity.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    TcMouthPlate mouthPlate = new TcMouthPlate();
                    BeanUtils.copyProperties(entity, mouthPlate);
                    if (tcMouthPlateMapper.checkUnique(mouthPlate) <= 0) {
                        successNum++;
                        saveOrUpdate(mouthPlate);
                    } else {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.mouthPlate.message.unique");
                        ImportUtil.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.tcMouthPlateMapper.deleteAll();
    }

}
