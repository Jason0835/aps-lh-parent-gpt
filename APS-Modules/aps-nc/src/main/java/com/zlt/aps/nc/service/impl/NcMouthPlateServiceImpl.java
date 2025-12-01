package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.nc.api.domain.dto.NcMouthPlateDto;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.entity.NcMouthPlate;
import com.zlt.aps.nc.mapper.NcMouthPlateMapper;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcMouthPlateService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 内衬口型板信息维护 服务实现类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@Service
public class NcMouthPlateServiceImpl extends ServiceImpl<NcMouthPlateMapper, NcMouthPlate> implements NcMouthPlateService {

    @Autowired
    private NcMouthPlateMapper ncMouthPlateMapper;

    @Autowired
    private NcMachineInfoService ncMachineInfoService;

    /**
     * 查询内衬口型板信息维护列表
     *
     * @param mouthPlate 内衬口型板信息维护
     * @return 内衬口型板信息维护集合
     */
    @Override
    public List<NcMouthPlateDto> selectMouthPlateList(NcMouthPlate mouthPlate) {
        return ncMouthPlateMapper.selectMouthPlateWithMachineInfo(mouthPlate);
    }

    /**
     * 查询内衬口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 内衬口型板信息维护集合
     */
    @Override
    public NcMouthPlate selectMouthPlateById(Long id) {
        LambdaQueryWrapper<NcMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NcMouthPlate::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(NcMouthPlate::getId, id);
        return ncMouthPlateMapper.selectOne(wrapper);
    }

    /**
     * 新增或更新内衬口型板信息维护
     *
     * @param mouthPlate 内衬口型板信息维护
     */
    @Override
    public void saveMouthPlate(NcMouthPlate mouthPlate) {
        if (ObjectUtils.allNotNull(mouthPlate.getMouthPlateCode(), mouthPlate.getMachineId()) && ncMouthPlateMapper.checkUnique(mouthPlate) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.unique"));
        }
        mouthPlate.setBaseVale(mouthPlate.getId());
        saveOrUpdate(mouthPlate);
    }

    /**
     * 批量删除内衬口型板信息维护
     *
     * @param ids 需要删除的内衬口型板信息维护ID
     */
    @Override
    public void deleteMouthPlateByIds(Long[] ids) {
        if (ids == null) {
            return;
        }

        LambdaUpdateWrapper<NcMouthPlate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
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
    public AjaxResult importData(List<NcMouthPlateDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;

        List<NcMouthPlateDto> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<NcMachineInfo> machineInfoList = ncMachineInfoService.selectMachineInfoList(new NcMachineInfo());
        //将机台名称转换为机台id，并做校验
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineCode, NcMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineName, NcMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMouthPlateCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            NcMouthPlateDto mouthPlate = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(mouthPlate.getMouthPlateCode()+mouthPlate.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                mouthPlate.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.mouthPlateCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            // 校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, mouthPlate);
            String machineName = mouthPlate.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtils.isNotBlank(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                // 设置机台id，更新人，更新时间
                mouthPlate.setBaseVale(null);
                mouthPlate.setMachineId(machineId);
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
                ncMouthPlateMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    NcMouthPlateDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    NcMouthPlate ncMouthPlate = new NcMouthPlate();
                    BeanUtils.copyProperties(excelItem, ncMouthPlate);
                    int unique = ncMouthPlateMapper.checkUnique(ncMouthPlate);
                    if (unique == 0) {
                        //不存在插入
                        successNum++;
                        ncMouthPlateMapper.insert(ncMouthPlate);
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
}
