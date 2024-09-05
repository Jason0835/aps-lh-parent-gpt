package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.mapper.CxMachineGroupMapper;
import com.zlt.aps.cx.service.CxMachineGroupService;
import com.zlt.aps.cx.service.CxMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型机组Service业务层处理
 *
 * @author zlt
 * @date 2021-12-16
 */
@Service
public class CxMachineGroupServiceImpl implements CxMachineGroupService {
    @Autowired
    private CxMachineGroupMapper cxMachineGroupMapper;

    @Autowired
    private CxMachineInfoService cxMachineInfoService;

    /**
     * 查询成型机组
     *
     * @param id 成型机组ID
     * @return 成型机组
     */
    @Override
    public CxMachineGroup selectCxMachineGroupById(Long id) {
        return cxMachineGroupMapper.selectCxMachineGroupById(id);
    }

    /**
     * 查询成型机组列表
     *
     * @param cxMachineGroup 成型机组
     * @return 成型机组
     */
    @Override
    public List<CxMachineGroup> selectCxMachineGroupList(CxMachineGroup cxMachineGroup) {
        return cxMachineGroupMapper.selectCxMachineGroupList(cxMachineGroup);
    }

    @Override
    public List<CxMachineGroupForExcel> selectCxMachineGroup4Excel(CxMachineGroup cxMachineGroup) {
        return cxMachineGroupMapper.selectCxMachineGroup4Excel(cxMachineGroup);
    }



    /**
     * 新增成型机组
     *
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    @Transactional
    @Override
    public int insertCxMachineGroup(CxMachineGroup cxMachineGroup) {
        cxMachineGroup.setBaseVale(null);
        int rows = cxMachineGroupMapper.insertCxMachineGroup(cxMachineGroup);
        insertCxMachineGroupList(cxMachineGroup);
        return rows;
    }

    /**
     * 修改成型机组
     *
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    @Transactional
    @Override
    public int updateCxMachineGroup(CxMachineGroup cxMachineGroup) {
        cxMachineGroup.setBaseVale(cxMachineGroup.getId());
        cxMachineGroupMapper.deleteCxMachineGroupListByGroupId(cxMachineGroup.getId());
        insertCxMachineGroupList(cxMachineGroup);
        return cxMachineGroupMapper.updateCxMachineGroup(cxMachineGroup);
    }

    /**
     * 批量删除成型机组
     *
     * @param ids 需要删除的成型机组ID
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteCxMachineGroupByIds(Long[] ids) {
        cxMachineGroupMapper.deleteCxMachineGroupListByGroupIds(ids);
        return cxMachineGroupMapper.deleteCxMachineGroupByIds(ids);
    }

    /**
     * 删除成型机组信息
     *
     * @param id 成型机组ID
     * @return 结果
     */
    @Override
    public int deleteCxMachineGroupById(Long id) {
        cxMachineGroupMapper.deleteCxMachineGroupListByGroupId(id);
        return cxMachineGroupMapper.deleteCxMachineGroupById(id);
    }

    /**
     * 新增组别机台列信息
     *
     * @param cxMachineGroup 成型机组对象
     */
    public void insertCxMachineGroupList(CxMachineGroup cxMachineGroup) {
       String[] machineCodes= cxMachineGroup.getMachineCodes();
       if (machineCodes!=null && machineCodes.length>0){
           List<CxMachineGroupList> list = new ArrayList<CxMachineGroupList>();
           Long id = cxMachineGroup.getId();
           for (String machineCode : machineCodes) {
               CxMachineGroupList cxMachineGroupList=new CxMachineGroupList();
               cxMachineGroupList.setGroupId(id);
               cxMachineGroupList.setCxMachineCode(machineCode);
               cxMachineGroupList.setBaseVale(null);
               list.add(cxMachineGroupList);
           }
           if (CollectionUtils.isNotEmpty(list)) {
               cxMachineGroupMapper.batchCxMachineGroupList(list);
           }
       }
    }

    /**
     * 校验成型机组唯一性
     */
    @Override
    public String checkCxMachineGroupUnique(CxMachineGroup cxMachineGroup) {
        List<CxMachineGroup> list = cxMachineGroupMapper.checkCxMachineGroupUnique(cxMachineGroup);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型机组数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxMachineGroupForExcel> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMachineGroupForExcel> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //构建机台Map
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

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getGroupName()+a.getCxMachineCode()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxMachineGroupForExcel cxMachineGroupForExcel = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(cxMachineGroupForExcel.getGroupName()+cxMachineGroupForExcel.getCxMachineCode());
            if (hasValue > 1) {
                failureNum++;
                cxMachineGroupForExcel.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.machineGroup.groupName");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cxScheduleResult.cxMachineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxMachineGroupForExcel);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxMachineGroupForExcel.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                cxMachineGroupForExcel.setBaseVale(null);
                importList.add(cxMachineGroupForExcel);
            }
        }

        //构造组名--班数、备注Map，其中班数和备注以Excel内的第一条有效值为最终值
        //组名--机台组Map，过滤了错误的记录
        Map<String, Map<String,Object>> groupNameAndShiftMap = new HashMap<>();
        Map<String, List<String>> groupNameAndCodesMap = new HashMap<>();
        for (CxMachineGroupForExcel a:list){
            if (a.getId() != null && a.getId().equals(-999L)) {
                continue;
            }
            if(groupNameAndShiftMap.get(a.getGroupName())==null){
                Map<String,Object> shiftAndRemark=new HashMap<>();
                shiftAndRemark.put("Shift",a.getProductShift());
                shiftAndRemark.put("Remark",a.getRemark());
                groupNameAndShiftMap.put(a.getGroupName(), shiftAndRemark);
            }
            if(groupNameAndCodesMap.get(a.getGroupName())==null){
                List<String> machineCodes=new ArrayList<>();
                if(StringUtils.isNotBlank(a.getCxMachineCode())){
                    machineCodes.add(a.getCxMachineCode());
                }
                groupNameAndCodesMap.put(a.getGroupName(),machineCodes);
            }else{
                List<String> machineCodes=groupNameAndCodesMap.get(a.getGroupName());
                if(StringUtils.isNotBlank(a.getCxMachineCode())){
                    machineCodes.add(a.getCxMachineCode());
                }
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
               // cxMachineGroupMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxMachineGroupForExcel item = list.get(i);
                    // 错误记录跳过
                    if (item.getId() != null && item.getId().equals(-999L)) {
                        continue;
                    }

                    CxMachineGroup cxMachineGroup=new CxMachineGroup();
                    cxMachineGroup.setGroupName(item.getGroupName());
                    List<CxMachineGroup> exist = cxMachineGroupMapper.checkCxMachineGroupUnique(cxMachineGroup);
                    if (CollectionUtils.isNotEmpty(exist)) {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                    }else{
                        successNum++;
                        cxMachineGroup.setProductShift(item.getProductShift());

                        this.insertCxMachineGroup(cxMachineGroup);
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
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
