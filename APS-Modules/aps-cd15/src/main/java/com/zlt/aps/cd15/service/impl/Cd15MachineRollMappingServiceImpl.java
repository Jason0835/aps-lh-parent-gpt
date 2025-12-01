package com.zlt.aps.cd15.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.mapper.Cd15MachineRollMappingMapper;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.cd15.service.Cd15MachineRollMappingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 钢带大卷与机台的映射表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Service
public class Cd15MachineRollMappingServiceImpl extends ServiceImpl<Cd15MachineRollMappingMapper, Cd15MachineRollMapping> implements Cd15MachineRollMappingService {

    @Resource
    private Cd15MachineRollMappingMapper machineRollMappingMapper;

    @Autowired
    private Cd15MachineInfoService cd15MachineInfoService;

    /**
     * 根据条件钢带大卷与机台的映射表
     *
     * @return
     */
    public List<Cd15MachineRollMappingDto> listMachineRollMapping(Cd15MachineRollMappingDto dto) {
        return machineRollMappingMapper.listMachineRollMapping(dto);
    }

    /**
     * 保存钢带大卷与机台的映射表（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveMachineRollMapping(Cd15MachineRollMapping entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isUnique(entity)) {
            //根据大卷号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.cd15.machine.roll.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 根据大卷编号+机台验证唯一性
     *
     * @param entity
     * @return
     */
    private boolean isUnique(Cd15MachineRollMapping entity) {
        QueryWrapper<Cd15MachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", entity.getBigRollCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd15MachineRollMapping> list = machineRollMappingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return false;
        }
        return true;
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteMachineRollMapping(Long[] ids) {
        LambdaUpdateWrapper<Cd15MachineRollMapping> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkMachineRollMapping(Cd15MachineRollMappingDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<Cd15MachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd15MachineRollMapping> list = machineRollMappingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
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
    public AjaxResult importData(List<Cd15MachineRollMappingDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15MachineRollMappingDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<Cd15MachineInfo> machineInfoList = cd15MachineInfoService.selectMachineInfoList(new Cd15MachineInfo());
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineCode, Cd15MachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineName, Cd15MachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getBigRollCode()+a.getMachineName(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15MachineRollMappingDto machineRollMappingDto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(machineRollMappingDto.getBigRollCode()+machineRollMappingDto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                machineRollMappingDto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.common.column.gy.bigRollCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineRollMappingDto);
            String machineName = machineRollMappingDto.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtils.isNotEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                machineRollMappingDto.setMachineId(machineId);
                machineRollMappingDto.setBaseVale(null);
                importList.add(machineRollMappingDto);
            } else {
                failureNum++;
                machineRollMappingDto.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    machineRollMappingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15MachineRollMappingDto excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        Cd15MachineRollMapping entity = new Cd15MachineRollMapping();
                        BeanUtils.copyProperties(excelItem, entity);
                        if (isUnique(entity)) {
                            //不存在插入
                            successNum++;
                            Cd15MachineRollMapping cd15MachineRollMapping = new Cd15MachineRollMapping();
                            BeanUtils.copyProperties(excelItem, cd15MachineRollMapping);
                            machineRollMappingMapper.insert(cd15MachineRollMapping);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.cd15.MachineRollMapping.message.unique"), importErrorLogs);
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
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public void deleteAll() {
        this.machineRollMappingMapper.deleteAll();
    }
}
