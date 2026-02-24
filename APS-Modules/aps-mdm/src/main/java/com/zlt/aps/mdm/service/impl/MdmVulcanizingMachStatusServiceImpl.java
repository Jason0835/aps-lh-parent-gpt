package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.mdm.mapper.MdmVulcanizingMachStatusEntityMapper;
import com.zlt.aps.mdm.mapper.VulcanizingMachineMapper;
import com.zlt.aps.mdm.service.IMdmVulcanizingMachStatusService;
import com.zlt.aps.mdm.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.aps.mdm.api.domain.entity.VulcanizingMachine;
import com.zlt.aps.mdm.api.domain.vo.CopyParamVo;
import com.zlt.aps.mdm.api.domain.vo.MdmVulcanizingMachStatusVo;
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
public class MdmVulcanizingMachStatusServiceImpl implements IMdmVulcanizingMachStatusService {

    @Resource
    private MdmVulcanizingMachStatusEntityMapper mdmVulcanizingMachStatusEntityMapper;
    @Resource
    private VulcanizingMachineMapper vulcanizingMachineMapper;

    @Resource
    private BaseDao baseDao;

    /**
     * 查询基础数据-硫化机可用信息
     *
     * @param id 基础数据-硫化机可用信息ID
     * @return 基础数据-硫化机可用信息
     */
    @Override
    public MdmVulcanizingMachStatus getDocVulcanizingMachStatusEntityById(Long id) {
        return mdmVulcanizingMachStatusEntityMapper.selectById(id);
    }

    /**
     * 查询基础数据-硫化机可用信息列表
     *
     * @param entity 基础数据-硫化机可用信息
     * @return 基础数据-硫化机可用信息集合
     */
    @Override
    public List<MdmVulcanizingMachStatusVo> selectDocVulcanizingMachStatusEntityList(MdmVulcanizingMachStatusVo entity) {
        return mdmVulcanizingMachStatusEntityMapper.selectDocVulcanizingMachStatusEntityList(entity);
    }

    /**
     * 新增基础数据-硫化机可用信息
     *
     * @param entity 基础数据-硫化机可用信息
     * @return 结果
     */
    @Override
    public int insertDocVulcanizingMachStatusEntity(MdmVulcanizingMachStatusVo entity) {
        return mdmVulcanizingMachStatusEntityMapper.insert(entity);
    }

    /**
     * 修改基础数据-硫化机可用信息
     *
     * @return 结果
     */
    @Override
    public int updateDocVulcanizingMachStatusEntity(Long[] ids, String status) {
        return mdmVulcanizingMachStatusEntityMapper.updateDocVulcanizingMachStatusListById(Arrays.asList(ids), status);
    }

    /**
     * 批量删除基础数据-硫化机可用信息
     *
     * @param ids 需要删除的基础数据-硫化机可用信息ID
     * @return 结果
     */
    @Override
    public int deleteDocVulcanizingMachStatusEntityByIds(Long[] ids) {
        return mdmVulcanizingMachStatusEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除基础数据-硫化机可用信息信息
     *
     * @param id 基础数据-硫化机可用信息ID
     * @return 结果
     */
    @Override
    public int deleteDocVulcanizingMachStatusEntityById(Long id) {
        return mdmVulcanizingMachStatusEntityMapper.deleteById(id);
    }

    /**
     * 校验基础数据-硫化机可用信息唯一性
     *
     * @param entity
     */
    @Override
    public String checkDocVulcanizingMachStatusEntityUnique(MdmVulcanizingMachStatusVo entity) {
        return mdmVulcanizingMachStatusEntityMapper.checkUnique(entity) == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /**
     * 复制可用台账信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyDocVulcanizingMachStatus(CopyParamVo copyParamVo) {
        copyParamVo.setCreatBy(SecurityUtils.getUsername());
        mdmVulcanizingMachStatusEntityMapper.deleteByYearAndMonth(copyParamVo);
        return mergeByParams(copyParamVo);
    }

    /**
     * 根据复制参数拷贝数据
     */
    private int mergeByParams(CopyParamVo params) {
        // 拷贝来源数据
        LambdaQueryWrapper<MdmVulcanizingMachStatus> fromWrapper = Wrappers.lambdaQuery();
        fromWrapper.eq(MdmVulcanizingMachStatus::getYear, params.getFromYear());
        fromWrapper.eq(MdmVulcanizingMachStatus::getMonth, params.getFromMonth());
        fromWrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), MdmVulcanizingMachStatus::getFactoryCode, params.getFactoryCode());
        List<MdmVulcanizingMachStatus> formList = mdmVulcanizingMachStatusEntityMapper.selectList(fromWrapper);

        return mergeByFormList(params, formList);
    }

    private int mergeByFormList(CopyParamVo params, List<MdmVulcanizingMachStatus> formList) {
        // 拷贝目标数据
        LambdaQueryWrapper<MdmVulcanizingMachStatus> copyWrapper = Wrappers.lambdaQuery();
        copyWrapper.eq(MdmVulcanizingMachStatus::getYear, params.getCopyToYear());
        copyWrapper.eq(MdmVulcanizingMachStatus::getMonth, params.getCopyToMonth());
        copyWrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), MdmVulcanizingMachStatus::getFactoryCode, params.getFactoryCode());

        List<MdmVulcanizingMachStatus> copyList = mdmVulcanizingMachStatusEntityMapper.selectList(copyWrapper);
        Map<Long, Long> copyMap = copyList.stream().collect(Collectors.toMap(MdmVulcanizingMachStatus::getVulcanizingMachineId, MdmVulcanizingMachStatus::getId, (v1, v2) -> v1));

        List<MdmVulcanizingMachStatus> updateList = new ArrayList<>();
        List<MdmVulcanizingMachStatus> insertList = new ArrayList<>();
        for (MdmVulcanizingMachStatus itemStatus : formList) {
            itemStatus.setYear(params.getCopyToYear());
            itemStatus.setMonth(params.getCopyToMonth());
            itemStatus.setId(null);
            itemStatus.setBaseVale(null);
            if (copyMap.containsKey(itemStatus.getVulcanizingMachineId())) {
                Long copyId = copyMap.get(itemStatus.getVulcanizingMachineId());
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

    /**
     * 合并可用台账信息
     *
     * @param copyParamVo 复制参数
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int mergeDocVulcanizingMachStatus(CopyParamVo copyParamVo) {
        copyParamVo.setCreatBy(SecurityUtils.getUsername());
        return mergeByParams(copyParamVo);
    }

    /**
     * 生成可用台账信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateDocVulcanizingMachStatus(CopyParamVo params) {
        params.setCreatBy(SecurityUtils.getUsername());

        List<MdmVulcanizingMachStatus> formList = mdmVulcanizingMachStatusEntityMapper.selectGenerateListByMachine(params);
        params.setCopyToYear(params.getGenerateToYear());
        params.setCopyToMonth(params.getGenerateToMonth());
        return mergeByFormList(params, formList);
    }

    /**
     * 导入基础数据-硫化机可用信息数据
     *
     * @param list          导入集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Override
    public AjaxResult importData(List<MdmVulcanizingMachStatusVo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<MdmVulcanizingMachStatusVo> importList = new ArrayList<>();
        List<MdmVulcanizingMachStatus> insertList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // 硫化机对象
        Map<String, Long> vulcanizingMachineMap = new HashMap<>();
        List<String> machineCodeList = list.stream().map(MdmVulcanizingMachStatusVo::getVulcanizingMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(machineCodeList)) {
            LambdaQueryWrapper<VulcanizingMachine> wrapper = Wrappers.lambdaQuery();
            wrapper.in(VulcanizingMachine::getVulcanizingMachineCode, machineCodeList);
            List<VulcanizingMachine> machineList = vulcanizingMachineMapper.selectList(wrapper);
            vulcanizingMachineMap = machineList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductTypeCode(), item.getVulcanizingMachineCode()), VulcanizingMachine::getId, (v1, v2) -> v1));
        }

        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String repeatingRecordStr = I18nUtil.getMessage("ui.data.alert.DocVulcanizingMachStatus.repeatingRecord");
        String noVulcanizingMachineStr = I18nUtil.getMessage("ui.data.alert.DocVulcanizingMachine.noVulcanizingMachine");
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                MdmVulcanizingMachStatusVo vulcanizingMachStatusEntity = list.get(i);
                int errorNum = i + 2;
                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, vulcanizingMachStatusEntity);

                Long vulcanizingMachineId = vulcanizingMachineMap.get(GenerageMapKeyUtils.createMapKey(vulcanizingMachStatusEntity.getFactoryCode(), vulcanizingMachStatusEntity.getProductTypeCode(), vulcanizingMachStatusEntity.getVulcanizingMachineCode()));
                if (StringUtils.isNotBlank(vulcanizingMachStatusEntity.getVulcanizingMachineCode()) && vulcanizingMachineId == null) {
                    // 未获取到对应的硫化机信息
                    vulcanizingMachStatusEntity.setId(-999L);
                    // TODO 国际化导入提示
                    String message = String.format(rowCountStr, i + 2) + noVulcanizingMachineStr;
                    addImportErrorLog(importLogId, errorNum, message, validated);
                }

                if (CollectionUtils.isEmpty(validated)) {
                    vulcanizingMachStatusEntity.setBaseVale(null);
                    vulcanizingMachStatusEntity.setVulcanizingMachineId(vulcanizingMachineId);
                    importList.add(vulcanizingMachStatusEntity);
                } else {
                    failureNum++;
                    vulcanizingMachStatusEntity.setId(-999L);
                    importErrorLogs.addAll(validated);
                }
            }

            // 唯一键分组
            Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(item -> (item.getYear() + item.getMonth() + item.getFactoryCode() + item.getProductTypeCode() + item.getVulcanizingMachineCode()), Collectors.counting()));
            for (int i = 0; i < list.size(); i++) {
                MdmVulcanizingMachStatusVo vulcanizingMachStatusEntity = list.get(i);
                int errorNum = i + 2;
                // 错误记录跳过
                if (vulcanizingMachStatusEntity.getId() != null && vulcanizingMachStatusEntity.getId().equals(-999L)) {
                    continue;
                }
                //重复记录校验
                Long hasValue = groupMap.get(vulcanizingMachStatusEntity.getYear() + vulcanizingMachStatusEntity.getMonth() + vulcanizingMachStatusEntity.getFactoryCode() + vulcanizingMachStatusEntity.getProductTypeCode() + vulcanizingMachStatusEntity.getVulcanizingMachineCode());
                if (hasValue > 1) {
                    failureNum++;
                    vulcanizingMachStatusEntity.setId(-999L);
                    // TODO 国际化导入提示
                    String message = String.format(rowCountStr, i + 2) + repeatingRecordStr;
                    addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                    continue;
                }
                insertList.add(vulcanizingMachStatusEntity);
            }

            try {
                //存在则更新状态和备注,不存在则插入
                if (updateSupport && org.apache.commons.collections.CollectionUtils.isNotEmpty(insertList)) {
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
    private void mergeByList(List<MdmVulcanizingMachStatus> newList) {
        if (CollectionUtils.isEmpty(newList)) {
            return;
        }

        List<String> factoryCodeList = newList.stream().map(MdmVulcanizingMachStatus::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<Integer> yearList = newList.stream().map(MdmVulcanizingMachStatus::getYear).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Integer> monthList = newList.stream().map(MdmVulcanizingMachStatus::getMonth).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> vulcanizingMachineIdList = newList.stream().map(MdmVulcanizingMachStatus::getVulcanizingMachineId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<MdmVulcanizingMachStatus> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CollectionUtils.isNotEmpty(factoryCodeList), MdmVulcanizingMachStatus::getFactoryCode, factoryCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(yearList), MdmVulcanizingMachStatus::getYear, yearList);
        wrapper.in(CollectionUtils.isNotEmpty(monthList), MdmVulcanizingMachStatus::getMonth, monthList);
        wrapper.in(CollectionUtils.isNotEmpty(vulcanizingMachineIdList), MdmVulcanizingMachStatus::getVulcanizingMachineId, vulcanizingMachineIdList);
        List<MdmVulcanizingMachStatus> oldList = mdmVulcanizingMachStatusEntityMapper.selectList(wrapper);
        Function<MdmVulcanizingMachStatus, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getVulcanizingMachineId());
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MdmVulcanizingMachStatus::getId, (v1, v2) -> v1));

        List<MdmVulcanizingMachStatus> updateList = new ArrayList<>();
        List<MdmVulcanizingMachStatus> insertList = new ArrayList<>();
        for (MdmVulcanizingMachStatus itemStatus : newList) {
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
}
