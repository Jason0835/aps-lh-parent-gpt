package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;
import com.zlt.aps.cx.mapper.CxSpecifyMachineMapper;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.CxSpecifyMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 定点机台Service业务层处理
 *
 * @author zlt
 * @date 2021-07-21
 */
@Service
public class CxSpecifyMachineServiceImpl implements CxSpecifyMachineService {
    @Autowired
    private CxSpecifyMachineMapper cxSpecifyMachineMapper;

    @Autowired
    private CxMachineInfoService cxMachineInfoService;

    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    @Override
    public CxSpecifyMachine selectCxSpecifyMachine1ById(Long id) {
        return cxSpecifyMachineMapper.selectCxSpecifyMachine1ById(id);
    }

    /**
     * 查询定点机台列表
     *
     * @param cxSpecifyMachine 定点机台
     * @return 定点机台
     */
    @Override
    public List<CxSpecifyMachine> selectCxSpecifyMachine1List(CxSpecifyMachine cxSpecifyMachine) {
        return cxSpecifyMachineMapper.selectCxSpecifyMachine1List(cxSpecifyMachine);
    }

    /**
     * 新增定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    @Override
    public int insertCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine) {
        cxSpecifyMachine.setBaseVale(null);
        return cxSpecifyMachineMapper.insertCxSpecifyMachine1(cxSpecifyMachine);
    }

    /**
     * 修改定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    @Override
    public int updateCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine) {
        cxSpecifyMachine.setBaseVale(cxSpecifyMachine.getId());
        return cxSpecifyMachineMapper.updateCxSpecifyMachine1(cxSpecifyMachine);
    }

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的定点机台ID
     * @return 结果
     */
    @Override
    public int deleteCxSpecifyMachine1ByIds(Long[] ids) {
        return cxSpecifyMachineMapper.deleteCxSpecifyMachine1ByIds(ids);
    }

    /**
     * 删除定点机台信息
     *
     * @param id 定点机台ID
     * @return 结果
     */
    @Override
    public int deleteCxSpecifyMachine1ById(Long id) {
        return cxSpecifyMachineMapper.deleteCxSpecifyMachine1ById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkCxSpecifyMachine1Unique(CxSpecifyMachine cxSpecifyMachine) {
        if (cxSpecifyMachine == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxSpecifyMachine> list = cxSpecifyMachineMapper.checkCxSpecifyMachine1Unique(cxSpecifyMachine);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxSpecifyMachine> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxSpecifyMachine> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        List<CxMachineInfo> machineInfoList = cxMachineInfoService.selectCxMachineInfoList(new CxMachineInfo());
        Map<String, String> machineCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineInfoList)) {

            //根据机台名称去重
            TreeSet<CxMachineInfo> treeSet = new TreeSet<CxMachineInfo>(new Comparator<CxMachineInfo>() {
                @Override
                public int compare(CxMachineInfo o1, CxMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList =new ArrayList<>(treeSet);

            machineInfoList.forEach(a -> machineCodeMap.put(a.getMachineName(), a.getMachineCode()));
        }
        Map<String, String> cxmachineTypeMap = machineInfoList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineName, CxMachineInfo::getMachineType));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSapCode()+a.getEmbryoCode()+a.getMachineName()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxSpecifyMachine dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSapCode()+dto.getEmbryoCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.specifyMachine.sapCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.specifyMachine.embryoCode");
                String columnName3 = I18nUtil.getMessage("ui.data.column.machine.machineName");
                message=String.format(message,columnName+"+"+columnName2+"+"+columnName3);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            String machineName = dto.getMachineName();
            if (machineCodeMap.get(machineName) == null) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            //20230909 导入时成型机类型需要和胎胚对应 Nick+
            if (StringUtils.isNotEmpty(cxmachineTypeMap.get(dto.getMachineName()))) {
                String machineType = cxmachineTypeMap.get(dto.getMachineName()).equals("1") ? "Y" : "E";
                String embryoCode = dto.getEmbryoCode();

                if (embryoCode.length() < 1 || !embryoCode.startsWith(machineType)){
                    addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.column.machineTypeNotMatch1"), validated);
                }
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dto.setMachineCode(machineCodeMap.get(dto.getMachineName()));
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
                    cxSpecifyMachineMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxSpecifyMachine newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        List<CxSpecifyMachine> exist = cxSpecifyMachineMapper.checkCxSpecifyMachine1Unique(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            cxSpecifyMachineMapper.insertCxSpecifyMachine1(newItem);
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
