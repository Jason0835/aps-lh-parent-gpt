package com.zlt.aps.gsq.service.impl;


import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.zlt.aps.gsq.api.domain.dto.GsqSpecifyMachineDto;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.entity.GsqSpecifyMachine;
import com.zlt.aps.gsq.mapper.GsqSpecifyMachineMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.GsqSpecifyMachineService;
import com.zlt.common.utils.StringUtil;

/**
 * <p>
 * 钢丝圈定点机台表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class GsqSpecifyMachineServiceImpl extends ServiceImpl<GsqSpecifyMachineMapper, GsqSpecifyMachine> implements GsqSpecifyMachineService {

    @Resource
    private GsqSpecifyMachineMapper gsqSpecifyMachineMapper;
    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    public List<GsqSpecifyMachineDto> listSpecifyMachine(GsqSpecifyMachineDto dto) {
        return gsqSpecifyMachineMapper.listSpecifyMachine(dto);
    }

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveSpecifyMachine(GsqSpecifyMachine entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isSpecifyMachineUnique(entity)) {
            //根据物料号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.gsq.specifyMachine.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 根据物料号+机台验证唯一性
     *
     * @param entity
     * @return
     */
    private boolean isSpecifyMachineUnique(GsqSpecifyMachine entity) {
        QueryWrapper<GsqSpecifyMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("STEEL_RING_CODE", entity.getSteelRingCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GsqSpecifyMachine> list = gsqSpecifyMachineMapper.selectList(queryWrapper);
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
        LambdaUpdateWrapper<GsqSpecifyMachine> wrapper = new LambdaUpdateWrapper<>();
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
    public AjaxResult importData(List<GsqSpecifyMachineDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqSpecifyMachineDto> importList = new ArrayList<>();
        //将机台名称转为机台code
        List<GsqMachineInfo> machineInfoList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            // 未查询到机台信息
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
//        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getId));
        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(GsqMachineInfo::getMachineName, GsqMachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSteelRingCode()+a.getMachineName()), Collectors.counting()));

        //将机台code转换为机台id，并做校验
        for (int i = 0; i < list.size(); i++) {
            GsqSpecifyMachineDto specifyMachine = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(specifyMachine.getSteelRingCode()+specifyMachine.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                specifyMachine.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.gsq.specifyMachine.column.steelRingCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            // 校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, specifyMachine);
            String machineName = specifyMachine.getMachineName();
            Long machineId = machineCodeMap.get(machineName);
            if (machineId == null && !StringUtil.isEmpty(machineName)) {
                // 未查询到对应机台信息
                ImportUtil.addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验失败
                failureNum++;
                specifyMachine.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                // 查询到机台信息，且校验通过
                specifyMachine.setMachineId(machineId);
                specifyMachine.setBaseVale(null);
                importList.add(specifyMachine);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                gsqSpecifyMachineMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    GsqSpecifyMachineDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    GsqSpecifyMachine tmSpecifyMachine = new GsqSpecifyMachine();
                    BeanUtils.copyProperties(excelItem, tmSpecifyMachine);
                    if (isSpecifyMachineUnique(tmSpecifyMachine)) {
                        //不存在插入
                        successNum++;
                        saveSpecifyMachine(tmSpecifyMachine);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        ImportUtil.addImportErrorLog(importLogId, i + 2,
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
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
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
        this.gsqSpecifyMachineMapper.deleteAllSpecifyMachine();
    }
}
