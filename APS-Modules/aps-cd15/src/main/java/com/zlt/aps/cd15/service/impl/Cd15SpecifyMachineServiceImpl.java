package com.zlt.aps.cd15.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.dto.Cd15SpecifyMachineDto;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.mapper.Cd15SpecifyMachineMapper;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.cd15.service.Cd15SpecifyMachineService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 15度裁断定点机台表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class Cd15SpecifyMachineServiceImpl extends ServiceImpl<Cd15SpecifyMachineMapper, Cd15SpecifyMachine> implements Cd15SpecifyMachineService {

    @Resource
    private Cd15SpecifyMachineMapper cd15SpecifyMachineMapper;
    @Autowired
    private Cd15MachineInfoService cd15MachineInfoService;

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    public List<Cd15SpecifyMachineDto> listSpecifyMachine(Cd15SpecifyMachineDto dto) {
        return cd15SpecifyMachineMapper.listSpecifyMachine(dto);
    }

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveSpecifyMachine(Cd15SpecifyMachine entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isSpecifyMachineUnique(entity)) {
            //根据物料号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.cd15.specifyMachine.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 根据物料号+机台验证唯一性
     *
     * @param entity
     * @return
     */
    private boolean isSpecifyMachineUnique(Cd15SpecifyMachine entity) {
        QueryWrapper<Cd15SpecifyMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("STEEL_STRIP_CODE", entity.getSteelStripCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<Cd15SpecifyMachine> list = cd15SpecifyMachineMapper.selectList(queryWrapper);
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
    public void deleteSpecifyMachine(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            Cd15SpecifyMachine entity = new Cd15SpecifyMachine();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
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
    public AjaxResult importData(List<Cd15SpecifyMachineDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15SpecifyMachineDto> importList = new ArrayList<>();
        //将机台名称转换为机台id，并做校验
        List<Cd15MachineInfo> machineInfoList = cd15MachineInfoService.selectMachineInfoList(new Cd15MachineInfo());
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineCode, Cd15MachineInfo::getId));
        Map<String, Long> machineNameMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineName, Cd15MachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSteelStripCode()+a.getMachineName()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15SpecifyMachineDto specifyMachineDto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(specifyMachineDto.getSteelStripCode()+specifyMachineDto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                specifyMachineDto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.common.column.gy.steelStripCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, specifyMachineDto);
            String machineName = specifyMachineDto.getMachineName();
            Long machineId = machineNameMap.get(machineName);
            if (machineId == null) {
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isEmpty(validated)) {
                specifyMachineDto.setMachineId(machineId);
                specifyMachineDto.setBaseVale(null);
                importList.add(specifyMachineDto);
            } else {
                failureNum++;
                // 校验失败标识
                specifyMachineDto.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                cd15SpecifyMachineMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    Cd15SpecifyMachineDto excelItem = list.get(i);
                    // 校验失败跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    Cd15SpecifyMachine specifyMachine = new Cd15SpecifyMachine();
                    BeanUtils.copyProperties(excelItem, specifyMachine);
                    boolean unique = isSpecifyMachineUnique(specifyMachine);
                    if (unique) {
                        //不存在插入
                        successNum++;
                        cd15SpecifyMachineMapper.insert(specifyMachine);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
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

    /**
     * 删除全部定点机台数据
     */
    public void deleteAllSpecifyMachine() {
        this.cd15SpecifyMachineMapper.deleteAllSpecifyMachine();
    }
}
