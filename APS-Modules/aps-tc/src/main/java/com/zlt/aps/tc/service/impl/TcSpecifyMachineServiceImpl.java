package com.zlt.aps.tc.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.dto.TcSpecifyMachineDto;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.entity.TcSpecifyMachine;
import com.zlt.aps.tc.mapper.TcSpecifyMachineMapper;
import com.zlt.aps.tc.service.TcMachineInfoService;
import com.zlt.aps.tc.service.TcSpecifyMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 胎面定点机台表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Service
public class TcSpecifyMachineServiceImpl extends ServiceImpl<TcSpecifyMachineMapper, TcSpecifyMachine> implements TcSpecifyMachineService {

    @Resource
    private TcSpecifyMachineMapper tcSpecifyMachineMapper;

    @Autowired
    private TcMachineInfoService tcMachineInfoService;

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
    public List<TcSpecifyMachineDto> listSpecifyMachine(TcSpecifyMachineDto dto) {
        return tcSpecifyMachineMapper.listSpecifyMachine(dto);
    }

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveSpecifyMachine(TcSpecifyMachine entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (!isSpecifyMachineUnique(entity)) {
            //根据物料号+机台信息验证
            throw new RuntimeException(I18nUtil.getMessage("ui.tc.specifyMachine.unique"));
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 根据物料号+机台验证唯一性
     *
     * @param entity
     * @return
     */
    private boolean isSpecifyMachineUnique(TcSpecifyMachine entity) {
        QueryWrapper<TcSpecifyMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SIDEWALL_CODE", entity.getSidewallCode());
        queryWrapper.eq("MACHINE_ID", entity.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (entity.getId() != null) {
            queryWrapper.ne("ID", entity.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TcSpecifyMachine> list = tcSpecifyMachineMapper.selectList(queryWrapper);
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
            TcSpecifyMachine entity = new TcSpecifyMachine();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcSpecifyMachineDto> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcSpecifyMachineDto> newList = new ArrayList<>();
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
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSidewallCode()+a.getMachineName()), Collectors.counting()));

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            TcSpecifyMachineDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSidewallCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.sidewallCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            String machineName = dto.getMachineName();
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (machineCodeMap.get(machineName) == null && StringUtils.isNotEmpty(machineName)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验失败
                failureNum++;
                dto.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                dto.setMachineId(machineCodeMap.get(machineName));
                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcSpecifyMachineMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcSpecifyMachineDto entity = list.get(i);
                    // 错误跳过
                    if (entity.getId() != null && entity.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    QueryWrapper<TcSpecifyMachine> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("SIDEWALL_CODE", entity.getSidewallCode());
                    queryWrapper.eq("MACHINE_ID", entity.getMachineId());
                    queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                    List<TcSpecifyMachine> exist = tcSpecifyMachineMapper.selectList(queryWrapper);
                    if (CollectionUtils.isEmpty(exist)) {
                        successNum++;
                        TcSpecifyMachine specifyMachine = new TcSpecifyMachine();
                        BeanUtils.copyProperties(entity, specifyMachine);
                        this.saveOrUpdate(specifyMachine);
                    } else {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.error.message.quota.unique");
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

    /**
     * 删除全部定点机台数据
     */
    public void deleteAllSpecifyMachine() {
        this.tcSpecifyMachineMapper.deleteAllSpecifyMachine();
    }
}
