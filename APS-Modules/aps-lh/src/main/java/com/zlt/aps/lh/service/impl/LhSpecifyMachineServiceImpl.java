package com.zlt.aps.lh.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.mapper.LhSpecifyMachineMapper;
import com.zlt.aps.lh.service.LhMachineInfoService;
import com.zlt.aps.lh.service.LhSpecifyMachineService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 硫化定点机台信息Service业务层处理
 *
 * @author zlt
 * @date 2021-07-21
 */
@Service
public class LhSpecifyMachineServiceImpl implements LhSpecifyMachineService {
    @Autowired
    private LhSpecifyMachineMapper lhSpecifyMachineMapper;

    @Autowired
    private LhMachineInfoService lhMachineInfoService;

    /**
     * 查询硫化定点机台信息
     *
     * @param id 硫化定点机台信息ID
     * @return 硫化定点机台信息
     */
    @Override
    public LhSpecifyMachine selectLhSpecifyMachineById(Long id) {
        return lhSpecifyMachineMapper.selectLhSpecifyMachineById(id);
    }

    /**
     * 查询硫化定点机台信息列表
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 硫化定点机台信息
     */
    @Override
    public List<LhSpecifyMachine> selectLhSpecifyMachineList(LhSpecifyMachine lhSpecifyMachine) {
        return lhSpecifyMachineMapper.selectLhSpecifyMachineList(lhSpecifyMachine);
    }

    /**
     * 新增硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    @Override
    public int insertLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine) {
        lhSpecifyMachine.setBaseVale(null);
        return lhSpecifyMachineMapper.insertLhSpecifyMachine(lhSpecifyMachine);
    }

    /**
     * 修改硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    @Override
    public int updateLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine) {
        lhSpecifyMachine.setBaseVale(lhSpecifyMachine.getId());
        return lhSpecifyMachineMapper.updateLhSpecifyMachine(lhSpecifyMachine);
    }

    /**
     * 批量删除硫化定点机台信息
     *
     * @param ids 需要删除的硫化定点机台信息ID
     * @return 结果
     */
    @Override
    public int deleteLhSpecifyMachineByIds(Long[] ids) {
        return lhSpecifyMachineMapper.deleteLhSpecifyMachineByIds(ids);
    }

    /**
     * 删除硫化定点机台信息信息
     *
     * @param id 硫化定点机台信息ID
     * @return 结果
     */
    @Override
    public int deleteLhSpecifyMachineById(Long id) {
        return lhSpecifyMachineMapper.deleteLhSpecifyMachineById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkLhSpecifyMachineUnique(LhSpecifyMachine lhSpecifyMachine) {
        if (lhSpecifyMachine == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<LhSpecifyMachine> list = lhSpecifyMachineMapper.checkLhSpecifyMachineUnique(lhSpecifyMachine);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 定点机台导入数据
     */
    @Override
    public AjaxResult importData(List<LhSpecifyMachine> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhSpecifyMachine> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<LhMachineInfo> machineInfoList = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo());
        Map<String, String> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {

            //根据机台名称去重
            TreeSet<LhMachineInfo> treeSet = new TreeSet<LhMachineInfo>(new Comparator<LhMachineInfo>() {
                @Override
                public int compare(LhMachineInfo o1, LhMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList =new ArrayList<>(treeSet);

            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getMachineCode()));
        }

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSapCode()+a.getMachineName()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            LhSpecifyMachine dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSapCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.specifyMachine.sapCode");
                String columnName2 = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                String machineName = dto.getMachineName();
                String machineCode = machineCodeMap.get(machineName);
                if (StringUtil.isBlank(machineCode) && StringUtil.isNotBlank(machineName)) {
                    dto.setId(-999L);
                    failureNum++;
                    ImportUtil.addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.error.message.column.machineNotExist"), importErrorLogs);
                    continue;
                }

                LhSpecifyMachine newEntity = new LhSpecifyMachine();
                dto.setBaseVale(null);
                dto.setMachineCode(machineCodeMap.get(dto.getMachineName()));
                BeanUtils.copyProperties(dto, newEntity);
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    lhSpecifyMachineMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        LhSpecifyMachine newItem = list.get(i);
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        List<LhSpecifyMachine> exist = lhSpecifyMachineMapper.checkLhSpecifyMachineUnique(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            lhSpecifyMachineMapper.insertLhSpecifyMachine(newItem);
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

}
