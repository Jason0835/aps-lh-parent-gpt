package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.mdm.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mdm.mapper.MdmProductVulcanizingEntityMapper;
import com.zlt.aps.mdm.mapper.VulcanizingMachineMapper;
import com.zlt.aps.mdm.service.IMdmProductVulcanizingService;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmProductVulcanizing;
import com.zlt.aps.mdm.api.domain.entity.VulcanizingMachine;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * 基础数据-硫化机正在生产品种Service业务层处理
 *
 * @author hsc
 * @date 2021-09-01
 */
@Service
public class MdmProductVulcanizingServiceImpl implements IMdmProductVulcanizingService {

    @Autowired
    private MdmProductVulcanizingEntityMapper mdmProductVulcanizingEntityMapper;
    @Resource
    private VulcanizingMachineMapper vulcanizingMachineMapper;
    @Resource
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Resource
    private BaseDao baseDao;

    /**
     * 查询基础数据-硫化机正在生产品种
     *
     * @param id 基础数据-硫化机正在生产品种主键
     * @return 基础数据-硫化机正在生产品种
     */
    @Override
    public MdmProductVulcanizing selectDocProductVulcanizationById(Long id) {
        return mdmProductVulcanizingEntityMapper.selectById(id);
    }

    /**
     * 查询基础数据-硫化机正在生产品种列表
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 基础数据-硫化机正在生产品种
     */
    @Override
    public List<MdmProductVulcanizing> selectDocProductVulcanizationList(MdmProductVulcanizing docProductVulcanization) {
        LambdaQueryWrapper<MdmProductVulcanizing> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getFactoryCode()), MdmProductVulcanizing::getFactoryCode, docProductVulcanization.getFactoryCode());
        wrapper.eq(docProductVulcanization.getYear() != null, MdmProductVulcanizing::getYear, docProductVulcanization.getYear());
        wrapper.eq(docProductVulcanization.getMonth() != null, MdmProductVulcanizing::getMonth, docProductVulcanization.getMonth());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getProductCode()), MdmProductVulcanizing::getProductCode, docProductVulcanization.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getProductTypeCode()), MdmProductVulcanizing::getProductTypeCode, docProductVulcanization.getProductTypeCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getVulcanizingMachineCode()), MdmProductVulcanizing::getVulcanizingMachineCode, docProductVulcanization.getVulcanizingMachineCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getMouldCode()), MdmProductVulcanizing::getMouldCode, docProductVulcanization.getMouldCode());
        return mdmProductVulcanizingEntityMapper.selectList(wrapper);
    }

    /**
     * 新增基础数据-硫化机正在生产品种
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 结果
     */
    @Override
    public int insertDocProductVulcanization(MdmProductVulcanizing docProductVulcanization) {
        return mdmProductVulcanizingEntityMapper.insert(docProductVulcanization);
    }

    /**
     * 修改基础数据-硫化机正在生产品种
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 结果
     */
    @Override
    public int updateDocProductVulcanization(MdmProductVulcanizing docProductVulcanization) {
        return mdmProductVulcanizingEntityMapper.updateById(docProductVulcanization);
    }

    /**
     * 批量删除基础数据-硫化机正在生产品种
     *
     * @param ids 需要删除的基础数据-硫化机正在生产品种主键
     * @return 结果
     */
    @Override
    public int deleteDocProductVulcanizationByIds(Long[] ids) {
        return mdmProductVulcanizingEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 校验基础数据-硫化机正在生产品种唯一性
     */
    @Override
    public String checkDocProductVulcanizationUnique(MdmProductVulcanizing docProductVulcanization) {
        if (docProductVulcanization == null) {
            return UserConstants.NOT_UNIQUE;
        }

        LambdaQueryWrapper<MdmProductVulcanizing> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(docProductVulcanization.getId() != null, MdmProductVulcanizing::getId, docProductVulcanization.getId());
        wrapper.eq(docProductVulcanization.getYear() != null, MdmProductVulcanizing::getYear, docProductVulcanization.getYear());
        wrapper.eq(docProductVulcanization.getMonth() != null, MdmProductVulcanizing::getMonth, docProductVulcanization.getMonth());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getProductCode()), MdmProductVulcanizing::getProductCode, docProductVulcanization.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getVulcanizingMachineCode()), MdmProductVulcanizing::getVulcanizingMachineCode, docProductVulcanization.getVulcanizingMachineCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getMouldCode()), MdmProductVulcanizing::getMouldCode, docProductVulcanization.getMouldCode());
        wrapper.eq(StringUtils.isNotBlank(docProductVulcanization.getFactoryCode()), MdmProductVulcanizing::getFactoryCode, docProductVulcanization.getFactoryCode());
        Long count = mdmProductVulcanizingEntityMapper.selectCount(wrapper);
        return count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 导入基础数据-硫化机正在生产品种数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MdmProductVulcanizing> list, boolean updateSupport, Long importLogId) {
        //1.初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmProductVulcanizing> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //2.国际化初始化
        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String materialNumberIsNoEqualsProductnameStr = I18nUtil.getMessage("ui.data.alert.materialNumberIsNoEqualsProductname");
        String materialNumberIsNoEqualsMachinenumberStr = I18nUtil.getMessage("ui.data.alert.materialNumberIsNoEqualsMachinenumber");
        String repeatingRecordStr = I18nUtil.getMessage("ui.data.alert.repeatingRecord");
        String notUniqueStr = I18nUtil.getMessage("ui.data.alert.ProductVulcanization.notUnique");

        // 硫化机台
        List<String> machineCodeList = list.stream().map(MdmProductVulcanizing::getVulcanizingMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, Long> machineMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(machineCodeList)) {
            LambdaQueryWrapper<VulcanizingMachine> machineWrapper = Wrappers.lambdaQuery();
            machineWrapper.in(VulcanizingMachine::getVulcanizingMachineCode, machineCodeList);
            List<VulcanizingMachine> machineList = vulcanizingMachineMapper.selectList(machineWrapper);
            machineMap = machineList.stream().collect(Collectors.toMap(VulcanizingMachine::getVulcanizingMachineCode, VulcanizingMachine::getId, (v1, v2) -> v1));
        }
        // 物料信息
        List<String> productCodeList = list.stream().map(MdmProductVulcanizing::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, MdmMaterialInfo> productCodeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(productCodeList)) {
            LambdaQueryWrapper<MdmMaterialInfo> productCodeWrapper = Wrappers.lambdaQuery();
            productCodeWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
            List<MdmMaterialInfo> productInfoList = mdmMaterialInfoEntityMapper.selectList(productCodeWrapper);
            productCodeMap = productInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (v1, v2) -> v1));
        }

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmProductVulcanizing docProductVulcanization = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docProductVulcanization);
            if (CollectionUtils.isNotEmpty(validated)) {
                docProductVulcanization.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                //判断品名和物料编号的关系
                MdmMaterialInfo productInfo = productCodeMap.get(docProductVulcanization.getProductCode());
                if (productInfo != null) {
                    if (!docProductVulcanization.getProductTypeCode().equals(productInfo.getProductTypeCode())) {
                        docProductVulcanization.setId(-999L);
                        failureNum++;
                        String message = String.format(rowCountStr, i + 2) + materialNumberIsNoEqualsProductnameStr;
                        addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        continue;
                    }
                }
                //判断硫化机编号好物料编号的关系
                Long machineId = machineMap.get(docProductVulcanization.getVulcanizingMachineCode());
                if (machineId == null) {
                    docProductVulcanization.setId(-999L);
                    failureNum++;
                    String message = String.format(rowCountStr, i + 2) + materialNumberIsNoEqualsMachinenumberStr;
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    continue;
                }
                docProductVulcanization.setVulcanizingMachineId(machineId);
                importList.add(docProductVulcanization);
            }
        }

        // 唯一键分组
        Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(item -> (item.getYear() + item.getMonth() + item.getProductCode() + item.getVulcanizingMachineCode() + item.getMouldCode() + item.getFactoryCode() + item.getProductTypeCode()), Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmProductVulcanizing docProductVulcanization = list.get(i);
            // 错误记录跳过
            if (docProductVulcanization.getId() != null && docProductVulcanization.getId().equals(-999L)) {
                continue;
            }
            Long hasValue = groupMap.get(docProductVulcanization.getYear() + docProductVulcanization.getMonth() + docProductVulcanization.getProductCode() + docProductVulcanization.getVulcanizingMachineCode() + docProductVulcanization.getMouldCode() + docProductVulcanization.getFactoryCode() + docProductVulcanization.getProductTypeCode());
            if (hasValue > 1) {
                docProductVulcanization.setId(-999L);
                failureNum++;
                String message = String.format(rowCountStr, i + 2) + repeatingRecordStr;
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                importList.remove(docProductVulcanization);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                this.mergeDocProductVulcanization(importList);
            } else {
                //唯一则新增
                List<MdmProductVulcanizing> insertList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    MdmProductVulcanizing docProductVulcanization = list.get(i);
                    // 错误记录跳过
                    if (docProductVulcanization.getId() != null && docProductVulcanization.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkDocProductVulcanizationUnique(docProductVulcanization);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        insertList.add(docProductVulcanization);
//                        this.insertDocProductVulcanization(docProductVulcanization);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2, notUniqueStr
                                , importErrorLogs);
                    }
                }
                if (CollectionUtils.isNotEmpty(insertList)) {
                    this.baseDao.insertBatch(insertList);
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

    /**
     * 有则更新，无则插入
     */
    public void mergeDocProductVulcanization(List<MdmProductVulcanizing> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        List<Integer> yearList = dataList.stream().map(MdmProductVulcanizing::getYear).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Integer> monthList = dataList.stream().map(MdmProductVulcanizing::getMonth).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> factoryCodeList = dataList.stream().map(MdmProductVulcanizing::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = dataList.stream().map(MdmProductVulcanizing::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> vulcanizingMachineCodeList = dataList.stream().map(MdmProductVulcanizing::getVulcanizingMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> mouldCodeList = dataList.stream().map(MdmProductVulcanizing::getMouldCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<MdmProductVulcanizing> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CollectionUtils.isNotEmpty(yearList), MdmProductVulcanizing::getYear, yearList);
        wrapper.in(CollectionUtils.isNotEmpty(monthList), MdmProductVulcanizing::getMonth, monthList);
        wrapper.in(CollectionUtils.isNotEmpty(factoryCodeList), MdmProductVulcanizing::getFactoryCode, factoryCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(productCodeList), MdmProductVulcanizing::getProductCode, productCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(vulcanizingMachineCodeList), MdmProductVulcanizing::getVulcanizingMachineCode, vulcanizingMachineCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(mouldCodeList), MdmProductVulcanizing::getMouldCode, mouldCodeList);
        List<MdmProductVulcanizing> oldList = mdmProductVulcanizingEntityMapper.selectList(wrapper);
        Function<MdmProductVulcanizing, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(),
                v.getProductCode(), v.getVulcanizingMachineCode(), v.getMouldCode());
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MdmProductVulcanizing::getId, (v1, v2) -> v1));

        List<MdmProductVulcanizing> updateList = new ArrayList<>();
        List<MdmProductVulcanizing> insertList = new ArrayList<>();
        for (MdmProductVulcanizing itemStatus : dataList) {
            String key = keyFunc.apply(itemStatus);
            if (oldMap.containsKey(key)) {
                itemStatus.setId(oldMap.get(key));
                updateList.add(itemStatus);
            } else {
                insertList.add(itemStatus);
            }
        }

        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);
    }
}
