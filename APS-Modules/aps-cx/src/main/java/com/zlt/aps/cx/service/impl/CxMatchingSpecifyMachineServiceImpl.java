package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cx.mapper.ConstructionInfoMapper;
import com.zlt.aps.cx.mapper.CxMatchingSpecifyMachineListMapper;
import com.zlt.aps.cx.mapper.CxMatchingSpecifyMachineMapper;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 定点机台Service业务层处理
 *
 * @author zlt
 * @date 2021-06-11
 */
@Service
public class CxMatchingSpecifyMachineServiceImpl implements CxMatchingSpecifyMachineService {
    @Autowired
    private CxMatchingSpecifyMachineMapper tSpecifyMachineMapper;

    @Autowired
    private CxMatchingSpecifyMachineListMapper cxSpecifyMachineListMapper;

    @Autowired
    private CxMachineInfoService cxMachineInfoService;

    @Autowired
    private ConstructionInfoMapper constructionInfoMapper;

    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    @Override
    public CxMatchingSpecifyMachine selectTSpecifyMachineById(Long id) {
        return tSpecifyMachineMapper.selectTSpecifyMachineById(id);
    }

    /**
     * 查询定点机台列表
     *
     * @param CxSpecifyMachine 定点机台
     * @return 定点机台
     */
    @Override
    public List<CxMatchingSpecifyMachine> selectTSpecifyMachineList(CxMatchingSpecifyMachine CxSpecifyMachine) {
        return tSpecifyMachineMapper.selectTSpecifyMachineList(CxSpecifyMachine);
    }

    /**
     * 新增定点机台
     *
     * @param CxSpecifyMachine 定点机台
     * @return 结果
     */
    @Transactional
    @Override
    public int insertTSpecifyMachine(CxMatchingSpecifyMachine cxSpecifyMachine) {
        cxSpecifyMachine.setBaseVale(null);
        int rows = tSpecifyMachineMapper.insertTSpecifyMachine(cxSpecifyMachine);
        return rows;
    }

    /**
     * 修改定点机台
     *
     * @param CxSpecifyMachine 定点机台
     * @return 结果
     */
    @Transactional
    @Override
    public int updateTSpecifyMachine(CxMatchingSpecifyMachine cxSpecifyMachine) {
        cxSpecifyMachine.setBaseVale(cxSpecifyMachine.getId());
        return tSpecifyMachineMapper.updateTSpecifyMachine(cxSpecifyMachine);
    }

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的定点机台ID
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTSpecifyMachineByIds(Long[] ids) {
        tSpecifyMachineMapper.deleteTSpecifyMachineListBySpecifyMachineIds(ids);
        return tSpecifyMachineMapper.deleteTSpecifyMachineByIds(ids);
    }

    /**
     * 删除定点机台信息
     *
     * @param id 定点机台ID
     * @return 结果
     */
    @Override
    public int deleteTSpecifyMachineById(Long id) {
        tSpecifyMachineMapper.deleteTSpecifyMachineListBySpecifyMachineId(id);
        return tSpecifyMachineMapper.deleteTSpecifyMachineById(id);
    }

    /**
     * 新增定点机台配置列信息
     *
     * @param CxSpecifyMachine 定点机台对象
     */
    public void insertTSpecifyMachineList(CxMatchingSpecifyMachine CxSpecifyMachine) {
        List<CxMatchingSpecifyMachineList> tSpecifyMachineListList = CxSpecifyMachine.getTSpecifyMachineListList();
        Long id = CxSpecifyMachine.getId();
        if (StringUtils.isNotNull(tSpecifyMachineListList)) {
            List<CxMatchingSpecifyMachineList> list = new ArrayList<CxMatchingSpecifyMachineList>();
            for (CxMatchingSpecifyMachineList tSpecifyMachineList : tSpecifyMachineListList) {
                tSpecifyMachineList.setSpecifyMachineId(id);
                tSpecifyMachineList.setBaseVale(null);
                list.add(tSpecifyMachineList);
            }
            if (list.size() > 0) {
                tSpecifyMachineMapper.batchTSpecifyMachineList(list);
            }
        }
    }

    /**
     * 配置列信息
     */
    @Override
    public List<CxMatchingSpecifyMachineList> detailList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        return cxSpecifyMachineListMapper.selectCxSpecifyMachineListList(cxMatchingSpecifyMachineList);
    }

    @Override
    public CxMatchingSpecifyMachineList selectCxSpecifyMachineListById(Long id) {
        return cxSpecifyMachineListMapper.selectCxSpecifyMachineListById(id);
    }

    public int detailAdd(CxMatchingSpecifyMachineList en) {
        en.setBaseVale(null);
        return cxSpecifyMachineListMapper.insertCxSpecifyMachineList(en);
    }

    public int detailEdit(CxMatchingSpecifyMachineList en) {
        en.setBaseVale(en.getId());
        return cxSpecifyMachineListMapper.updateCxSpecifyMachineList(en);
    }


    @Override
    public int deleteDetailByIds(Long[] ids) {
        return cxSpecifyMachineListMapper.deleteCxSpecifyMachineListByIds(ids);
    }

    public List<CxMatchingSpecifyMachineList> viewList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        return cxSpecifyMachineListMapper.viewList(cxMatchingSpecifyMachineList);
    }

    /**
     * 校验定点机台唯一性
     */
    @Override
    public List<CxMatchingSpecifyMachine> checkCxSpecifyMachineUnic(CxMatchingSpecifyMachine cxSpecifyMachine) {
        return tSpecifyMachineMapper.checkCxSpecifyMachineUnic(cxSpecifyMachine);
    }

    /**
     * 校验定点机台详情唯一性
     */
    @Override
    public List<CxMatchingSpecifyMachineList> checkCxSpecifyMachineDetailUnic(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        return cxSpecifyMachineListMapper.checkCxSpecifyMachineDetailUnic(cxMatchingSpecifyMachineList);
    }

    /**
     * 定点机台导入数据
     */
    @Override
    public AjaxResult importData(List<CxMatchingSpecifyMachine> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMatchingSpecifyMachine> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getEmbryoCode()+a.getSap()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxMatchingSpecifyMachine dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getEmbryoCode()+dto.getSap());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cx.machine.embryoCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cx.machine.sap");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            //校验胎胚代码
            ConstructionInfoDto constructionInfoDto = new ConstructionInfoDto();
            constructionInfoDto.setEmbryoCode(dto.getEmbryoCode());
            List<ConstructionInfoDto> constructionInfoList = constructionInfoMapper.listConstructionInfo(constructionInfoDto);
            if (CollectionUtils.isEmpty(constructionInfoList)) {
                String message = I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.embryoCodeNotExist4Import");
                String column = I18nUtil.getMessage("ui.data.column.cx.machine.embryoCode");
                message=String.format(message, i + 2,column);
                addImportErrorLog(importLogId, i + 2,message, validated);
            }else{
                ConstructionInfoDto constructionInfo=constructionInfoList.get(0);
                dto.setSpecDesc(constructionInfo.getSpecDesc());
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                CxMatchingSpecifyMachine newEntity = new CxMatchingSpecifyMachine();
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
                    tSpecifyMachineMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxMatchingSpecifyMachine newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        newItem.setBaseVale(null);

                        List<CxMatchingSpecifyMachine> exist = tSpecifyMachineMapper.checkCxSpecifyMachineUnic(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            tSpecifyMachineMapper.insertTSpecifyMachine(newItem);
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

    /**
     * 定点机台详情导入数据
     */
    @Override
    public AjaxResult detailImportData(List<CxMatchingSpecifyMachineList> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMatchingSpecifyMachineList> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getProcedureCode()+a.getMachineName()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxMatchingSpecifyMachineList dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getProcedureCode()+dto.getMachineName());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cx.machine.procedureCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.machine.machineCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);

            //机台id校验
            if(StringUtils.isBlank(dto.getMachineId())){
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
                addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                CxMatchingSpecifyMachineList newEntity = new CxMatchingSpecifyMachineList();
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
                    cxSpecifyMachineListMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxMatchingSpecifyMachineList newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        newItem.setBaseVale(null);
                        List<CxMatchingSpecifyMachineList> exist = cxSpecifyMachineListMapper.checkCxSpecifyMachineDetailUnic(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            cxSpecifyMachineListMapper.insertCxSpecifyMachineList(newItem);
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
