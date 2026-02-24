package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.mdm.mapper.MdmMoldingMachineClsEntityMapper;
import com.zlt.aps.mdm.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.mdm.mapper.MdmProductionMoldingEntityMapper;
import com.zlt.aps.mdm.service.IMdmMaterialInfoService;
import com.zlt.aps.mdm.service.IMdmProductionMoldingService;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.mdm.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.mdm.api.domain.vo.MdmProductionMoldingPageVo;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import io.seata.common.util.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;


/**
 * 分厂成型正在生产的品种Service业务层处理
 *
 * @author hsc
 * @date 2021-08-30
 */
@Service
public class MdmProductionMoldingServiceImpl extends BaseService<MdmProductionMolding> implements IMdmProductionMoldingService {

    @Resource
    private MdmProductionMoldingEntityMapper mdmProductionMoldingEntityMapper;
    @Resource
    private MdmMoldingMachineEntityMapper moldingMachineEntityMapper;
    @Resource
    private MdmMoldingMachineClsEntityMapper moldingMachineClsEntityMapper;

    @Resource
    private IMdmMaterialInfoService iMdmMaterialInfoService;

    @Resource
    private BaseDao baseDao;

    /**
     * 查询分厂成型正在生产的品种
     *
     * @param id 分厂成型正在生产的品种主键
     * @return 分厂成型正在生产的品种
     */
    @Override
    public MdmProductionMolding selectFactoryProductionProductById(Long id) {
        return mdmProductionMoldingEntityMapper.selectById(id);
    }

    /**
     * 查询分厂成型正在生产的品种列表
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 分厂成型正在生产的品种
     */
    @Override
    public List<MdmProductionMolding> selectFactoryProductionProductList(MdmProductionMolding mdmProductionMolding) {
        LambdaQueryWrapper<MdmProductionMolding> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(mdmProductionMolding.getId() != null, MdmProductionMolding::getId, mdmProductionMolding.getId());
        wrapper.eq(mdmProductionMolding.getYear() != null, MdmProductionMolding::getYear, mdmProductionMolding.getYear());
        wrapper.eq(mdmProductionMolding.getMonth() != null, MdmProductionMolding::getMonth, mdmProductionMolding.getMonth());
        wrapper.eq(StringUtils.isNotBlank(mdmProductionMolding.getFactoryCode()), MdmProductionMolding::getFactoryCode, mdmProductionMolding.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(mdmProductionMolding.getProductCode()), MdmProductionMolding::getProductCode, mdmProductionMolding.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(mdmProductionMolding.getMoldingMachineCode()), MdmProductionMolding::getMoldingMachineCode, mdmProductionMolding.getMoldingMachineCode());
        wrapper.orderByAsc(MdmProductionMolding::getFactoryCode).orderByAsc(MdmProductionMolding::getMoldingMachineCode);
        return mdmProductionMoldingEntityMapper.selectList(wrapper);
    }

    /**
     * 新增分厂成型正在生产的品种
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 结果
     */
    @Override
    public int insertFactoryProductionProduct(MdmProductionMolding mdmProductionMolding) {
        return mdmProductionMoldingEntityMapper.insert(mdmProductionMolding);
    }

    /**
     * 修改分厂成型正在生产的品种
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 结果
     */
    @Override
    public int updateFactoryProductionProduct(MdmProductionMolding mdmProductionMolding) {
        return mdmProductionMoldingEntityMapper.updateById(mdmProductionMolding);
    }

    /**
     * 批量删除分厂成型正在生产的品种
     *
     * @param ids 需要删除的分厂成型正在生产的品种主键
     * @return 结果
     */
    @Override
    public int deleteFactoryProductionProductByIds(Long[] ids) {
        return mdmProductionMoldingEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 校验分厂成型正在生产的品种唯一性
     */
    @Override
    public String checkFactoryProductionProductUnique(MdmProductionMolding mdmProductionMolding) {
        if (mdmProductionMolding == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int unique = mdmProductionMoldingEntityMapper.checkFactoryProductionProductUnique(mdmProductionMolding);
        if (unique > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入分厂成型正在生产的品种数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MdmProductionMolding> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmProductionMolding> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String noOnlyStr = I18nUtil.getMessage("ui.data.alert.FactoryProductionProduct.noOnly");
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmProductionMolding mdmProductionMolding = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mdmProductionMolding);
            if (CollectionUtils.isNotEmpty(validated)) {
                mdmProductionMolding.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else if (updateSupport) {
                mdmProductionMolding.setBaseVale(mdmProductionMolding.getId());
                importList.add(mdmProductionMolding);
            }
        }

        // 唯一键分组去除重复
        Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(
                item -> (item.getYear() + item.getMonth() + item.getFactoryCode() + item.getProductCode() + item.getMoldingMachineCode()
                ), Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmProductionMolding mdmProductionMolding = list.get(i);
            // 错误记录跳过
            if (mdmProductionMolding.getId() != null && mdmProductionMolding.getId().equals(-999L)) {
                continue;
            }
            //重复记录校验
            String key = mdmProductionMolding.getYear() + mdmProductionMolding.getMonth() + mdmProductionMolding.getFactoryCode() + mdmProductionMolding.getProductCode() + mdmProductionMolding.getMoldingMachineCode();
            Long hasValue = groupMap.get(key);
            if (hasValue > 1) {
                failureNum++;
                mdmProductionMolding.setId(-999L);
                // TODO 国际化导入提示
                String message = String.format(rowCountStr, i + 2) + noOnlyStr;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                importList.remove(mdmProductionMolding);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                mergeByList(importList);
            } else {
                //唯一则新增
                List<MdmProductionMolding> insertList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    MdmProductionMolding mdmProductionMolding = list.get(i);
                    // 错误记录跳过
                    if (mdmProductionMolding.getId() != null && mdmProductionMolding.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkFactoryProductionProductUnique(mdmProductionMolding);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        mdmProductionMolding.setBaseVale(null);
                        insertList.add(mdmProductionMolding);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("lean.factoryProductionProduct.unique.msg"), importErrorLogs);
                    }
                }
                if (CollectionUtils.isNotEmpty(insertList)) {
                    baseDao.insertBatch(insertList);
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
    private void mergeByList(List<MdmProductionMolding> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        List<Integer> yearList = importList.stream().map(MdmProductionMolding::getYear).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Integer> monthList = importList.stream().map(MdmProductionMolding::getMonth).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> factoryCodeList = importList.stream().map(MdmProductionMolding::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = importList.stream().map(MdmProductionMolding::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> moldingMachineCodeList = importList.stream().map(MdmProductionMolding::getMoldingMachineCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<MdmProductionMolding> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CollectionUtils.isNotEmpty(yearList), MdmProductionMolding::getYear, yearList);
        wrapper.in(CollectionUtils.isNotEmpty(monthList), MdmProductionMolding::getMonth, monthList);
        wrapper.in(CollectionUtils.isNotEmpty(factoryCodeList), MdmProductionMolding::getFactoryCode, factoryCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(productCodeList), MdmProductionMolding::getProductCode, productCodeList);
        wrapper.in(CollectionUtils.isNotEmpty(moldingMachineCodeList), MdmProductionMolding::getMoldingMachineCode, moldingMachineCodeList);
        List<MdmProductionMolding> oldList = mdmProductionMoldingEntityMapper.selectList(wrapper);
        Function<MdmProductionMolding, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getMoldingMachineCode());
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MdmProductionMolding::getId, (v1, v2) -> v1));

        List<MdmProductionMolding> updateList = new ArrayList<>();
        List<MdmProductionMolding> insertList = new ArrayList<>();
        for (MdmProductionMolding itemStatus : importList) {
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

    /**
     * 获取成型法
     */
    @Override
    public MdmProductionMoldingPageVo getMachineMethod(MdmProductionMoldingPageVo vo) {
        MdmProductionMoldingPageVo result = new MdmProductionMoldingPageVo();
        if (StringUtils.isBlank(vo.getProductCode())) {
            return result;
        }

        // 查询对应物料信息
        List<MdmMaterialInfo> productInfoList = iMdmMaterialInfoService.selectListByProductCode(Collections.singletonList(vo.getProductCode()));
        if (CollectionUtils.isEmpty(productInfoList)) {
            return result;
        }

        // 查询机台信息
        String productTypeCode = productInfoList.get(0).getProductTypeCode();
        LambdaQueryWrapper<MdmMoldingMachine> machineWrapper = Wrappers.lambdaQuery();
//        machineWrapper.eq(MdmMoldingMachine::getProductTypeCode, productTypeCode);
        machineWrapper.eq(MdmMoldingMachine::getFactoryCode, vo.getFactoryCode());
        machineWrapper.eq(MdmMoldingMachine::getCxMachineCode, vo.getMachineCode());
        List<MdmMoldingMachine> machineList = moldingMachineEntityMapper.selectList(machineWrapper);
        if (CollectionUtils.isEmpty(machineList)) {
            return result;
        }

        // 查询成型法则
//        Long moldingMachineClassId = machineList.get(0).getMoldingMachineClassId();
//        if (moldingMachineClassId == null) {
//            return result;
//        }
//        MdmMoldingMachineCls mdmMoldingMachineCls = moldingMachineClsEntityMapper.selectById(moldingMachineClassId);
//        if (mdmMoldingMachineCls == null) {
//            return result;
//        }

//        result.setMoldingMethod(mdmMoldingMachineCls.getMouldMethod());
        return result;
    }
}
