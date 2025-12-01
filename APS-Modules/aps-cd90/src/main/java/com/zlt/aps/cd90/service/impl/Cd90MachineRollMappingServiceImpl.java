package com.zlt.aps.cd90.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.mapper.Cd90MachineRollMappingMapper;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import com.zlt.aps.cd90.service.Cd90MachineRollMappingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-16
 */
@Service
public class Cd90MachineRollMappingServiceImpl extends ServiceImpl<Cd90MachineRollMappingMapper, Cd90MachineRollMapping> implements Cd90MachineRollMappingService {

    @Resource
    private Cd90MachineRollMappingMapper machineRollMappingMapper;

    @Autowired
    private Cd90MachineInfoService cd90MachineInfoService;

    /**
     * 根据条件大卷颜色提示列表
     *
     * @return
     */
    public List<Cd90MachineRollMappingDto> listMachineRollMapping(Cd90MachineRollMappingDto dto) {
        return machineRollMappingMapper.listMachineRollMapping(dto);
    }

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveMachineRollMapping(Cd90MachineRollMapping entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isUnique(entity)) {
            //根据大卷号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.cd90.machine.roll.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 根据大卷编号+机台验证唯一性
     *
     * @param entity
     * @return
     */
    private boolean isUnique(Cd90MachineRollMapping entity) {
        QueryWrapper<Cd90MachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", entity.getBigRollCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd90MachineRollMapping> list = machineRollMappingMapper.selectList(queryWrapper);
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
        LambdaUpdateWrapper<Cd90MachineRollMapping> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkMachineRollMapping(Cd90MachineRollMappingDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<Cd90MachineRollMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd90MachineRollMapping> list = machineRollMappingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90MachineRollMappingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90MachineRollMapping> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<Cd90MachineInfo> machineInfoList = cd90MachineInfoService.selectMachineInfoList(new Cd90MachineInfo());
        Map<String, Long> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
//            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineCode(), a.getId()));
            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getId()));
        }
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getBigRollCode() + v.getMachineName()), Collectors.counting()));
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90MachineRollMappingDto dto = list.get(i);

			// excel内业务主键唯一校验
			if (groupMap.get(dto.getBigRollCode() + dto.getMachineName()) > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.bigRollColor.column.bigRollCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.loss.line");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
								columnName1 + "+" + columnName2),
						importErrorLogs);
				failureNum++;
				continue;
			}
            
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            String machineName = dto.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && StringUtils.isNotEmpty(machineName)) {
                addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dto.setMachineId(machineCodeMap.get(dto.getMachineName()));
                Cd90MachineRollMapping newEntity = new Cd90MachineRollMapping();
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
                    machineRollMappingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {

                        Cd90MachineRollMappingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }

                        List<Cd90MachineRollMappingDto> uniqueList=machineRollMappingMapper.checkUnique(dto);
                        if (CollectionUtils.isEmpty(uniqueList)) {
                            Cd90MachineRollMapping newItem = new Cd90MachineRollMapping();
                            BeanUtils.copyProperties(dto, newItem);
                            newItem.setBaseVale(null);
                            successNum++;
                            this.saveOrUpdate(newItem);
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
        //返回提示信息及错误集合
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
