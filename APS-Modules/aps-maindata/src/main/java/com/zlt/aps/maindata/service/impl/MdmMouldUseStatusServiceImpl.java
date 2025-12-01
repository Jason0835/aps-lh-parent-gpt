package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.enums.SystemBaseEnums;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMouldUseStatusEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IMdmMouldUseStatusService;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldUseStatus;
import com.zlt.aps.monthplan.api.domain.vo.MdmMouldUseStatusVo;
import com.zlt.aps.monthplan.api.domain.vo.PeriodInfo;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;


/**
 * 模具可用状态Service业务层处理
 *
 * @author leo
 * @date 2021-08-27
 */
@Slf4j
@Service
public class MdmMouldUseStatusServiceImpl extends BaseService<MdmMouldUseStatus> implements IMdmMouldUseStatusService {
    @Autowired
    private MdmMouldUseStatusEntityMapper mdmMouldUseStatusEntityMapper;
    @Autowired
    private MdmModelInfoEntityMapper modelInfoMapper;
    @Autowired
    private MdmProductModelRelationEntityMapper productModelRelationMapper;

    @Resource
    private BaseDao baseDao;

    /**
     * 查询模具可用状态
     * mouldUseStatusMapper
     *
     * @param id 模具可用状态主键
     * @return 模具可用状态
     */
    @Override
    public MdmMouldUseStatus selectMouldUseStatusById(Long id) {
        return mdmMouldUseStatusEntityMapper.selectById(id);
    }

    /**
     * 查询模具可用状态列表
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 模具可用状态
     */
    @Override
    public List<MdmMouldUseStatus> selectMouldUseStatusList(MdmMouldUseStatus mdmMouldUseStatus) {
        return mdmMouldUseStatusEntityMapper.selectMouldUseStatusList(mdmMouldUseStatus);
    }

    @Override
    public List<MdmMouldUseStatus> selectMouldUseStatusListForProductCode(MdmMouldUseStatus mdmMouldUseStatus) {
        return mdmMouldUseStatusEntityMapper.selectMouldUseStatusListForProductCode(mdmMouldUseStatus);
    }

    /**
     * 新增模具可用状态
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 结果
     */
    @Override
    public int insertMouldUseStatus(MdmMouldUseStatus mdmMouldUseStatus) {
        setOwerFactoryCode(mdmMouldUseStatus);
        mdmMouldUseStatus.setBaseVale(null);
        String msg = "";
        LambdaQueryWrapper<MdmModelInfo> infoWrapper = Wrappers.lambdaQuery();
        infoWrapper.eq(MdmModelInfo::getMouldCode, mdmMouldUseStatus.getMouldCode());
        infoWrapper.eq(MdmModelInfo::getFactoryCode, mdmMouldUseStatus.getFactoryCode());
        List<MdmModelInfo> modelInfoList = modelInfoMapper.selectList(infoWrapper);
        Map<String, MdmModelInfo> modelInfoMap = modelInfoList.stream().collect(Collectors.toMap(v->GenerageMapKeyUtils.createMapKey(v.getFactoryCode(),v.getMouldCode()), Function.identity()));
        if (!modelInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(mdmMouldUseStatus.getFactoryCode(), mdmMouldUseStatus.getMouldCode()))) {
            msg = I18nUtil.getMessage("biz.mouldUseStatus.modelInfoNotExist");
        }
        if (StringUtils.isNotBlank(msg)) {
            throw new RuntimeException(msg);
        }
        return mdmMouldUseStatusEntityMapper.insert(mdmMouldUseStatus);
    }

    /**
     * 修改模具可用状态
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 结果
     */
    @Override
    public int updateMouldUseStatus(MdmMouldUseStatus mdmMouldUseStatus) {
        setOwerFactoryCode(mdmMouldUseStatus);
        return mdmMouldUseStatusEntityMapper.updateById(mdmMouldUseStatus);
    }

    /**
     * 批量删除模具可用状态
     *
     * @param ids 需要删除的模具可用状态主键
     * @return 结果
     */
    @Override
    public int deleteMouldUseStatusByIds(Long[] ids) {
        return mdmMouldUseStatusEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除模具可用状态信息
     *
     * @param id 模具可用状态主键
     * @return 结果
     */
    @Override
    public int deleteMouldUseStatusById(Long id) {
        return mdmMouldUseStatusEntityMapper.deleteById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkMouldUseStatusUnique(MdmMouldUseStatus mdmMouldUseStatus) {
        if (mdmMouldUseStatus == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MdmMouldUseStatus> list = mdmMouldUseStatusEntityMapper.checkMouldUseStatusUnique(mdmMouldUseStatus);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult copy(PeriodInfo vo) {
        mdmMouldUseStatusEntityMapper.deleteMouldUseStatusByTime(vo.getCompanyCode(), vo.getFactoryCode(), vo.getToyear(), vo.getTomonth());
        mergeByPeriod(vo);
        return AjaxResult.success();
    }

    /**
     * 复制指定年月、分厂数据，有则更新，无则插入
     */
    private void mergeByPeriod(PeriodInfo vo) {
        LambdaQueryWrapper<MdmMouldUseStatus> fromWrapper = Wrappers.lambdaQuery();
        fromWrapper.eq(MdmMouldUseStatus::getYear, vo.getFromyear());
        fromWrapper.eq(MdmMouldUseStatus::getMonth, vo.getFrommonth());
        fromWrapper.eq(StringUtils.isNotBlank(vo.getFactoryCode()), MdmMouldUseStatus::getFactoryCode, vo.getFactoryCode());
        List<MdmMouldUseStatus> fromList = mdmMouldUseStatusEntityMapper.selectList(fromWrapper);

        LambdaQueryWrapper<MdmMouldUseStatus> copyWrapper = Wrappers.lambdaQuery();
        copyWrapper.eq(MdmMouldUseStatus::getYear, vo.getToyear());
        copyWrapper.eq(MdmMouldUseStatus::getMonth, vo.getTomonth());
        copyWrapper.eq(StringUtils.isNotBlank(vo.getFactoryCode()), MdmMouldUseStatus::getFactoryCode, vo.getFactoryCode());
        List<MdmMouldUseStatus> copyList = mdmMouldUseStatusEntityMapper.selectList(copyWrapper);
        Map<String, Long> copyMap = copyList.stream().filter(v -> StringUtils.isNotBlank(v.getMouldCode()))
                .collect(Collectors.toMap(MdmMouldUseStatus::getMouldCode, MdmMouldUseStatus::getId, (v1, v2) -> v1));

        List<MdmMouldUseStatus> updateList = new ArrayList<>();
        List<MdmMouldUseStatus> insertList = new ArrayList<>();
        for (MdmMouldUseStatus itemStatus : fromList) {
            itemStatus.setYear(Long.valueOf(vo.getToyear()));
            itemStatus.setMonth(Long.valueOf(vo.getTomonth()));
            itemStatus.setId(null);
            itemStatus.setBaseVale(null);
            if (copyMap.containsKey(itemStatus.getMouldCode())) {
                Long copyId = copyMap.get(itemStatus.getMouldCode());
                itemStatus.setId(copyId);
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

    @Override
    public AjaxResult merge(PeriodInfo vo) {
        mergeByPeriod(vo);
        return AjaxResult.success();
    }


    /**
     * 导入excel
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MdmMouldUseStatus> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMouldUseStatus> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String noOnlyStr = I18nUtil.getMessage("ui.data.alert.MouldUseStatus.noOnly");
        String notUniqueStr = I18nUtil.getMessage("ui.data.alert.mouldUseStatus.notUnique");
        String modelInfoNotExistMsg = I18nUtil.getMessage("biz.mouldUseStatus.modelInfoNotExist");
        Map<String, MdmModelInfo> modelInfoMap = new HashMap<>();
        Set<String> mouldCodeList = list.stream().map(MdmMouldUseStatus::getMouldCode).collect(Collectors.toSet());
        Set<String> factoryList = list.stream().map(MdmMouldUseStatus::getFactoryCode).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(mouldCodeList)) {
            LambdaQueryWrapper<MdmModelInfo> infoWrapper = Wrappers.lambdaQuery();
            infoWrapper.in(MdmModelInfo::getMouldCode, mouldCodeList);
            infoWrapper.in(CollectionUtils.isNotEmpty(factoryList), MdmModelInfo::getFactoryCode, factoryList);
            List<MdmModelInfo> modelInfoList = modelInfoMapper.selectList(infoWrapper);
            modelInfoMap = modelInfoList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMouldCode()), Function.identity(), (s1, s2) -> s1));
        }
        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldUseStatus info = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, info);
            int rowIndex = i + 2;
            String formattedRowIndex;
            if ("第%s行".equals(rowCountStr)) {
                formattedRowIndex = String.format("第%s行", rowIndex); // 中文环境下
            } else {
                formattedRowIndex = String.format("Row %d", rowIndex); // 英文环境下
            }
            String msg = "";
            
            // 模具信息校验
            if (!modelInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getMouldCode()))) {
                msg = modelInfoNotExistMsg;
            }
            if (StringUtils.isNotBlank(msg)) {
                String errorMessage = formattedRowIndex + msg;
                addImportErrorLog(importLogId, errorNum, errorMessage, validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                info.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                info.setBaseVale(info.getId());
                importList.add(info);
            }
        }

        // 唯一键分组
        Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getYear(), item.getMonth(), item.getMouldCode()), Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldUseStatus info = list.get(i);
            // 错误记录跳过
            if (info.getId() != null && info.getId().equals(-999L)) {
                continue;
            }
            //重复记录校验
            Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(info.getFactoryCode(), info.getYear(), info.getMonth(), info.getMouldCode()));
            if (hasValue > 1) {
                failureNum++;
                info.setId(-999L);
                // TODO 国际化导入提示
                String message = String.format(rowCountStr, i + 2) + noOnlyStr;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                importList.remove(info);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport) {
                successNum = importList.size();
                this.mergeByList(importList);
            } else {
                //唯一则新增
                List<MdmMouldUseStatus> insertList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    MdmMouldUseStatus info = list.get(i);
                    // 错误记录跳过
                    if (info.getId() != null && info.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMouldUseStatusUnique(info);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        setOwerFactoryCode(info);
                        info.setBaseVale(null);
                        insertList.add(info);
                    } else {
                        //不允许覆盖
                        //failureNum++;
                        //String message = String.format(rowCountStr, i + 2) + I18nUtil.getMessage("ui.data.alert.mouldUseStatus.notUnique");//该年月、模具号的数据在系统中已经存在，不能重复！
                        //ImportExcelValidatedUtils.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);

                        //允许覆盖
                        successNum++;
                        List<MdmMouldUseStatus> uniqueList = mdmMouldUseStatusEntityMapper.checkMouldUseStatusUnique(info);
                        if (uniqueList.size() == 1) {
                            uniqueList.get(0).setFactoryCode(info.getFactoryCode());
                            uniqueList.get(0).setMouldStatus(info.getMouldStatus());
                            uniqueList.get(0).setRemark(info.getRemark());
                            this.updateMouldUseStatus(uniqueList.get(0));
                        } else {
                            failureNum++;
                            String message = String.format(rowCountStr, i + 2) + notUniqueStr;//导入替换失败了，替换数据不是唯一！
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                        }
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
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 查询统计
     */
    @Override
    public MdmMouldUseStatusVo listTotal(MdmMouldUseStatus mdmMouldUseStatus) {
        MdmMouldUseStatusVo mouldUseStatusVo = new MdmMouldUseStatusVo();
        int mouldQty = 0;
        int noMouldQty = 0;
        if (StringUtils.isNotEmpty(mdmMouldUseStatus.getProductCode())) {
            mdmMouldUseStatus.setMouldStatus(1L);
            mouldQty = mdmMouldUseStatusEntityMapper.selectCountMouldUseStatusListForProductCode(mdmMouldUseStatus);
            mdmMouldUseStatus.setMouldStatus(0L);
            noMouldQty = mdmMouldUseStatusEntityMapper.selectCountMouldUseStatusListForProductCode(mdmMouldUseStatus);
        } else {
            mdmMouldUseStatus.setMouldStatus(1L);
            mouldQty = mdmMouldUseStatusEntityMapper.selectCountMouldUseStatusList(mdmMouldUseStatus);
            mdmMouldUseStatus.setMouldStatus(0L);
            noMouldQty = mdmMouldUseStatusEntityMapper.selectCountMouldUseStatusList(mdmMouldUseStatus);
        }
        mouldUseStatusVo.setMouldQty(mouldQty);
        mouldUseStatusVo.setNoMouldQty(noMouldQty);
        mouldUseStatusVo.setTotalMouldQty(mouldQty + noMouldQty);
        return mouldUseStatusVo;
    }

    /**
     * 根据分厂，年，月查询模具可用状态
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    @Override
    public List<MdmMouldUseStatus> queryByFactoryCodeYearMonth(String factoryCode, int year,int month,Set<String> mouldCodes){
        // 将 Set 转换为 List，便于切分批次
        List<String> codeList = new ArrayList<>(mouldCodes);
        //定义最终返回的List
        List<MdmMouldUseStatus> finalList  = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (codeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(codeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<MdmMouldUseStatus> queryList = mdmMouldUseStatusEntityMapper.queryByFactoryCodeYearMonth(factoryCode,year,month,splitItemList);
                finalList.addAll(queryList);
            }
        }else{
            finalList = mdmMouldUseStatusEntityMapper.queryByFactoryCodeYearMonth(factoryCode,year,month,codeList);
        }
        return finalList;
    }

    /**
     * 有则更新，无则插入
     */
    private void mergeByList(List<MdmMouldUseStatus> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        LambdaQueryWrapper<MdmMouldUseStatus> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList,
                MdmMouldUseStatus::getYear,
                MdmMouldUseStatus::getMonth,
                MdmMouldUseStatus::getFactoryCode,
                MdmMouldUseStatus::getMouldCode);
        List<MdmMouldUseStatus> oldList = mdmMouldUseStatusEntityMapper.selectList(wrapper);
        Function<MdmMouldUseStatus, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getMouldCode());
        Map<String, Long> oldMap = oldList.stream().collect(Collectors.toMap(keyFunc, MdmMouldUseStatus::getId, (v1, v2) -> v1));

        List<MdmMouldUseStatus> updateList = new ArrayList<>();
        List<MdmMouldUseStatus> insertList = new ArrayList<>();
        for (MdmMouldUseStatus itemStatus : importList) {
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
     * 根据模具信息表获取信息写入模具可用状态表的对应字段
     *
     * @param mdmMouldUseStatus 模具状态信息
     */
    private void setOwerFactoryCode(MdmMouldUseStatus mdmMouldUseStatus) {
        LambdaQueryWrapper<MdmModelInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmModelInfo::getMouldCode, mdmMouldUseStatus.getMouldCode());
        List<MdmModelInfo> modelInfoList = modelInfoMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(modelInfoList)) {
            MdmModelInfo modelInfo = modelInfoList.get(0);
            mdmMouldUseStatus.setOwerFactoryCode(modelInfo.getFactoryCode());
            mdmMouldUseStatus.setSpecifications(modelInfo.getSpecifications());
            mdmMouldUseStatus.setPattern(modelInfo.getPattern());
            mdmMouldUseStatus.setMouldType(modelInfo.getMouldType());
        }
    }
}
