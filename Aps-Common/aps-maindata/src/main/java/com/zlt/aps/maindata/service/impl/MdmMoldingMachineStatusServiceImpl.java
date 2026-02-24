package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineStatusEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineStatusService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusVo;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import io.seata.common.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

@Service
public class MdmMoldingMachineStatusServiceImpl implements IMdmMoldingMachineStatusService {

    @Resource
    private BaseDao baseDao;
    @Resource
    private MdmMoldingMachineStatusEntityMapper mdmMoldingMachineStatusEntityMapper;
    @Resource
    private MdmMoldingMachineEntityMapper moldingMachineEntityMapper;

    /**
     * 查询基础数据-成型机可用信息
     *
     * @param id 基础数据-成型机可用信息ID
     * @return 基础数据-成型机可用信息
     */
    @Override
    public MdmMoldingMachineStatus selectDocMoldingMachineStatusById(Long id) {
        return mdmMoldingMachineStatusEntityMapper.selectById(id);
    }

    /**
     * 查询基础数据-成型机可用信息列表
     *
     * @param docMoldingMachineStatus 基础数据-成型机可用信息
     * @return 基础数据-成型机可用信息
     */
    @Override
    public List<MdmMoldingMachineStatusVo> selectDocMoldingMachineStatusList(MdmMoldingMachineStatus docMoldingMachineStatus) {
        return mdmMoldingMachineStatusEntityMapper.selectDocMoldingMachineStatusList(docMoldingMachineStatus);
    }

    /**
     * 新增基础数据-成型机可用信息
     *
     * @param docMoldingMachineStatus 基础数据-成型机可用信息
     * @return 结果
     */
    @Override
    public int insertDocMoldingMachineStatus(MdmMoldingMachineStatus docMoldingMachineStatus) {
        return mdmMoldingMachineStatusEntityMapper.insert(docMoldingMachineStatus);
    }

    /**
     * 修改基础数据-成型机可用信息
     *
     * @param ids    要修改状态的id
     * @param status 修改的状态
     * @return 结果
     */
    @Override
    public int updateDocMoldingMachineStatus(Long[] ids, String status) {
        return mdmMoldingMachineStatusEntityMapper.updateMoldingMachineStatusListById(Arrays.asList(ids), status);
    }

    /**
     * 批量删除基础数据-成型机可用信息
     *
     * @param ids 需要删除的基础数据-成型机可用信息ID
     * @return 结果
     */
    @Override
    public int deleteDocMoldingMachineStatusByIds(Long[] ids) {
        return mdmMoldingMachineStatusEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 校验基础数据-成型机可用信息唯一性
     */
    @Override
    public String checkDocMoldingMachineStatusUnique(MdmMoldingMachineStatus docMoldingMachineStatus) {
        if (docMoldingMachineStatus == null) {
            return UserConstants.NOT_UNIQUE;
        }
        if (mdmMoldingMachineStatusEntityMapper.checkUnique(docMoldingMachineStatus) != 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入基础数据-成型机可用信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MdmMoldingMachineStatusVo> list, boolean updateSupport, Long importLogId) {
        //1.初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMoldingMachineStatusVo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<MdmMoldingMachineStatus> insertList = new ArrayList<>();

        //2.国际化初始化
        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String noMoldingMachinesStr = I18nUtil.getMessage("ui.data.alert.DocMoldingMachineStatus.noMoldingMachines");
        String repeatingRecordStr = I18nUtil.getMessage("ui.data.alert.DocMoldingMachineStatus.repeatingRecord");

        // 成型机信息
        LambdaQueryWrapper<MdmMoldingMachine> machineWrapper = Wrappers.lambdaQuery();
        Map<String, Long> moldingMachineMap = new HashMap<>();
        List<String> machineCodeList = list.stream().map(MdmMoldingMachineStatusVo::getMoldingMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
//        if (CollectionUtils.isNotEmpty(machineCodeList)) {
//            machineWrapper.in(MdmMoldingMachine::getCxMachineCode, machineCodeList);
//            List<MdmMoldingMachine> moldingMachineList = moldingMachineEntityMapper.selectList(machineWrapper);
//            moldingMachineMap = moldingMachineList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductTypeCode(), item.getCxMachineCode()), MdmMoldingMachine::getId, (v1, v2) -> v1));
//        }

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                MdmMoldingMachineStatusVo machineStatusEntity = list.get(i);
                int errorNum = i + 2;
                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, machineStatusEntity);
                Long moldingMachineId = moldingMachineMap.get(GenerageMapKeyUtils.createMapKey(machineStatusEntity.getFactoryCode(), machineStatusEntity.getProductTypeCode(), machineStatusEntity.getMoldingMachineCode()));
                if (StringUtils.isNotBlank(machineStatusEntity.getMoldingMachineCode()) && moldingMachineId == null) {
                    // 未获取到对应的成型机信息
                    machineStatusEntity.setId(-999L);
                    String message = String.format(rowCountStr, i + 2) + noMoldingMachinesStr;
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }
                if (CollectionUtils.isEmpty(validated)) {
                    machineStatusEntity.setBaseVale(null);
                    machineStatusEntity.setMoldingMachineId(moldingMachineId);
                    importList.add(machineStatusEntity);
                } else {
                    failureNum++;
                    machineStatusEntity.setId(-999L);
                    importErrorLogs.addAll(validated);
                }
            }

            // 唯一键分组
            Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(item -> (item.getYear() + item.getMonth() + item.getFactoryCode() + item.getProductTypeCode() + item.getMoldingMachineCode()), Collectors.counting()));
            for (int i = 0; i < list.size(); i++) {
                MdmMoldingMachineStatusVo machineStatusEntity = list.get(i);
                int errorNum = i + 2;
                // 错误记录跳过
                if (machineStatusEntity.getId() != null && machineStatusEntity.getId().equals(-999L)) {
                    continue;
                }
                //重复记录校验
                Long hasValue = groupMap.get(machineStatusEntity.getYear() + machineStatusEntity.getMonth() + machineStatusEntity.getFactoryCode() + machineStatusEntity.getProductTypeCode() + machineStatusEntity.getMoldingMachineCode());
                if (hasValue > 1) {
                    failureNum++;
                    machineStatusEntity.setId(-999L);
                    // TODO 国际化导入提示
                    String message = String.format(rowCountStr, i + 2) + repeatingRecordStr;
                    addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                    continue;
                }
                insertList.add(machineStatusEntity);
            }

            try {
                //存在则更新状态和备注,不存在则插入
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(insertList)) {
                    successNum = insertList.size();
                    mergeByList(insertList);
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

    /**
     * 有则更新，无则插入
     */
    private void mergeByList(List<MdmMoldingMachineStatus> newList) {
        if (CollectionUtils.isEmpty(newList)) {
            return;
        }

        List<String> factoryCodeList = newList.stream().map(MdmMoldingMachineStatus::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<Integer> yearList = newList.stream().map(MdmMoldingMachineStatus::getYear).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Integer> monthList = newList.stream().map(MdmMoldingMachineStatus::getMonth).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> moldingMachineIdList = newList.stream().map(MdmMoldingMachineStatus::getMoldingMachineId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<MdmMoldingMachineStatus> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMoldingMachineStatus::getFactoryCode, factoryCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(yearList), MdmMoldingMachineStatus::getYear, yearList);
        wrapper.in(CollectionUtils.isNotEmpty(monthList), MdmMoldingMachineStatus::getMonth, monthList);
        wrapper.in(CollectionUtils.isNotEmpty(moldingMachineIdList), MdmMoldingMachineStatus::getMoldingMachineId, moldingMachineIdList);
        List<MdmMoldingMachineStatus> oldList = mdmMoldingMachineStatusEntityMapper.selectList(wrapper);
        Function<MdmMoldingMachineStatus, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getMoldingMachineId());
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MdmMoldingMachineStatus::getId, (v1, v2) -> v1));

        List<MdmMoldingMachineStatus> updateList = new ArrayList<>();
        List<MdmMoldingMachineStatus> insertList = new ArrayList<>();
        for (MdmMoldingMachineStatus itemStatus : newList) {
            itemStatus.setBaseVale(null);
            String key = keyFunc.apply(itemStatus);
            if (oldMap.containsKey(key)) {
                itemStatus.setId(oldMap.get(key));
                itemStatus.setCreateBy(null);
                itemStatus.setCreateTime(null);
                updateList.add(itemStatus);
            } else {
                insertList.add(itemStatus);
            }
        }

        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);
    }

    /**
     * 拷贝指定年月的数据到指定年月
     *
     * @param params 指定的数据源年月和指定要拷贝到的年月
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyMoldingMachineStatus(CopyParamVo params) {
        params.setCreatBy(SecurityUtils.getUsername());
        mdmMoldingMachineStatusEntityMapper.deleteByYearAndMonth(params);
        return mergeByParams(params);
    }

    /**
     * 根据复制参数拷贝数据
     */
    private int mergeByParams(CopyParamVo params) {
        // 拷贝来源数据
        LambdaQueryWrapper<MdmMoldingMachineStatus> fromWrapper = Wrappers.lambdaQuery();
        fromWrapper.eq(MdmMoldingMachineStatus::getYear, params.getFromYear());
        fromWrapper.eq(MdmMoldingMachineStatus::getMonth, params.getFromMonth());
        fromWrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), MdmMoldingMachineStatus::getFactoryCode, params.getFactoryCode());
        List<MdmMoldingMachineStatus> formList = mdmMoldingMachineStatusEntityMapper.selectList(fromWrapper);

        return mergeByFormList(params, formList);
    }

    private int mergeByFormList(CopyParamVo params, List<MdmMoldingMachineStatus> formList) {
        // 拷贝目标数据
        LambdaQueryWrapper<MdmMoldingMachineStatus> copyWrapper = Wrappers.lambdaQuery();
        copyWrapper.eq(MdmMoldingMachineStatus::getYear, params.getCopyToYear());
        copyWrapper.eq(MdmMoldingMachineStatus::getMonth, params.getCopyToMonth());
        copyWrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), MdmMoldingMachineStatus::getFactoryCode, params.getFactoryCode());
        List<MdmMoldingMachineStatus> copyList = mdmMoldingMachineStatusEntityMapper.selectList(copyWrapper);
        Map<Long, Long> copyMap = copyList.stream().collect(Collectors.toMap(MdmMoldingMachineStatus::getMoldingMachineId, MdmMoldingMachineStatus::getId, (v1, v2) -> v1));

        List<MdmMoldingMachineStatus> updateList = new ArrayList<>();
        List<MdmMoldingMachineStatus> insertList = new ArrayList<>();
        for (MdmMoldingMachineStatus itemStatus : formList) {
            itemStatus.setYear(params.getCopyToYear());
            itemStatus.setMonth(params.getCopyToMonth());
            itemStatus.setId(null);
            itemStatus.setBaseVale(null);
            if (copyMap.containsKey(itemStatus.getMoldingMachineId())) {
                Long copyId = copyMap.get(itemStatus.getMoldingMachineId());
                itemStatus.setId(copyId);
                itemStatus.setCreateBy(null);
                itemStatus.setCreateTime(null);
                updateList.add(itemStatus);
            } else {
                insertList.add(itemStatus);
            }
        }

        int count = baseDao.insertBatch(insertList);
        count += baseDao.updateBatch(updateList);

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int mergeMoldingMachineStatus(CopyParamVo params) {
        params.setCreatBy(SecurityUtils.getUsername());
        return mergeByParams(params);
    }

    /**
     * 从档案表生产数据到指定年月
     *
     * @param params 指定年月
     * @return 结果
     */
    @Override
    public int generateMoldingMachineStatus(CopyParamVo params) {
        params.setCreatBy(SecurityUtils.getUsername());
        List<MdmMoldingMachineStatus> formList = mdmMoldingMachineStatusEntityMapper.selectGenerateListByMachine(params);
        params.setCopyToYear(params.getGenerateToYear());
        params.setCopyToMonth(params.getGenerateToMonth());
        return mergeByFormList(params, formList);
    }
}
