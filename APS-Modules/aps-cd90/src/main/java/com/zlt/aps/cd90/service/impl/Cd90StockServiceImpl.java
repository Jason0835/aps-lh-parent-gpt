package com.zlt.aps.cd90.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.entity.Cd90Params;
import com.zlt.aps.cd90.mapper.Cd90CurlLengthEntityMapper;
import com.zlt.aps.cd90.mapper.Cd90ParamsMapper;
import com.zlt.aps.cd90.mapper.Cd90StockMapper;
import com.zlt.aps.cd90.service.Cd90StockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 90°裁断库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class Cd90StockServiceImpl implements Cd90StockService {
    @Autowired
    private Cd90StockMapper stockMapper;

    @Autowired
    private Cd90CurlLengthEntityMapper curlLengthEntityMapper;

    @Autowired
    private Cd90ParamsMapper paramsMapper;

    /**
     * 查询90°裁断库存信息
     *
     * @param id 90°裁断库存信息ID
     * @return 90°裁断库存信息
     */
    @Override
    public Cd90Stock selectStockById(Long id) {
        Cd90Stock stock = stockMapper.selectStockById(id);
        LambdaQueryWrapper<Cd90CurlLength> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90CurlLength::getClothCode, stock.getMaterialCode());
        wrapper.eq(Cd90CurlLength::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        Cd90CurlLength curlRoll = curlLengthEntityMapper.selectOne(wrapper);
        if (curlRoll != null) {
            stock.setCurlLength(curlRoll.getCurlLength());
        }
        return stock;
    }

    /**
     * 查询90°裁断库存信息列表
     *
     * @param Cd90Stock 90°裁断库存信息
     * @return 90°裁断库存信息
     */
    @Override
    public List<Cd90Stock> selectStockList(Cd90Stock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        List<Cd90Stock> stockList = stockMapper.selectStockList(stock);
        if (CollectionUtils.isNotEmpty(stockList)) {
            List<String> codeList = stockList.stream().map(Cd90Stock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, BigDecimal> lengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(codeList)) {
                LambdaQueryWrapper<Cd90CurlLength> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(Cd90CurlLength::getClothCode, codeList);
                wrapper.eq(Cd90CurlLength::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
                List<Cd90CurlLength> curlRollList = curlLengthEntityMapper.selectList(wrapper);
                lengthMap = curlRollList.stream().collect(Collectors.toMap(Cd90CurlLength::getClothCode, Cd90CurlLength::getCurlLength));
            }
            for (Cd90Stock cd90Stock : stockList) {
                String materialCode = cd90Stock.getMaterialCode();
                if (lengthMap.containsKey(materialCode)) {
                    BigDecimal length = lengthMap.get(materialCode);
                    cd90Stock.setCurlLength(length);
                }
            }
        }
        return stockList;
    }

    /**
     * 新增90°裁断库存信息
     *
     * @param Cd90Stock 90°裁断库存信息
     * @return 结果
     */
    @Override
    public int insertStock(Cd90Stock stock) {
        stock.setBaseVale(null);
        return stockMapper.insertStock(stock);
    }

    /**
     * 修改90°裁断库存信息
     *
     * @param stock 90°裁断库存信息
     * @return 结果
     */
    @Override
    public int updateStock(Cd90Stock stock) {
        stock.setBaseVale(stock.getId());
        return stockMapper.updateStock(stock);
    }

    /**
     * 批量删除90°裁断库存信息
     *
     * @param ids 需要删除的90°裁断库存信息ID
     * @return 结果
     */
    @Override
    public int deleteStockByIds(Long[] ids) {
        return stockMapper.deleteStockByIds(ids);
    }

    /**
     * 校验90°裁断库存唯一性（根据库存日期+物料编号+id）
     */
    public List<Cd90Stock> checkStockListUnic(Cd90Stock stock) {
        return stockMapper.checkStockListUnic(stock);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<Cd90Stock> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<Cd90Stock> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        LambdaUpdateWrapper<Cd90Params> paramsWrapper = new LambdaUpdateWrapper<>();
        paramsWrapper.eq(Cd90Params::getParamCode, "CRIMP_LENGTH");
        Cd90Params params = paramsMapper.selectOne(paramsWrapper);

		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getMaterialCode() + DateUtil.formatDate(v.getStockDate())), Collectors.counting()));

        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        List<Cd90CurlLength> curlRollList = new ArrayList<>();
        List<String> codeList = list.stream().map(Cd90Stock::getMaterialCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        List<List<String>> splitList = CollectionUtil.splitList(codeList, 100);
        for (List<String> stringList : splitList) {
            LambdaUpdateWrapper<Cd90CurlLength> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(Cd90CurlLength::getClothCode, stringList);
            wrapper.eq(Cd90CurlLength::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
            curlRollList.addAll(curlLengthEntityMapper.selectList(wrapper));
        }
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(Cd90CurlLength::getClothCode, Cd90CurlLength::getCurlLength, (m1, m2) -> m1));
        }

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            Cd90Stock dto = list.get(i);

			// excel内业务主键唯一校验
            String materialCode = dto.getMaterialCode();
            Long hasValue = groupMap.get(materialCode + DateUtil.formatDate(dto.getStockDate()));
			if (hasValue > 1) {
				dto.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.data.column.stock.clothCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.stock.stockDate");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
								columnName1 + "+" + columnName2),
						importErrorLogs);
				failureNum++;
				continue;
			}

            // 库存量(米)和库存量(卷)不能同时为空
            if (ObjectUtils.allNull(dto.getStockNum(), dto.getRollStockNum())) {
                failureNum++;
                dto.setId(-999L);
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.stock.stockNumAndRollNumNotNull"), importErrorLogs);
                continue;
            }

            // 卷数转换成米数，或米数转换成卷数
            /*if (!curlRollMap.containsKey(materialCode)) {
                failureNum++;
                dto.setId(-999L);
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.data.column.cd90.stock.curlLengthNotExist"), importErrorLogs);
                continue;
            }*/

            BigDecimal curlLength = curlRollMap.getOrDefault(materialCode, new BigDecimal(params.getParamValue()));
            BigDecimal rollStockNum = dto.getRollStockNum();
            if (rollStockNum != null) {
                BigDecimal stockNum = rollStockNum.multiply(curlLength);
                dto.setStockNum(stockNum);
            } else if (dto.getStockNum() != null) {
                dto.setRollStockNum(dto.getStockNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }

            BigDecimal rollModifyNum = dto.getRollModifyNum();
            if (rollModifyNum != null) {
                BigDecimal modifyNum = rollModifyNum.multiply(curlLength);
                dto.setModifyNum(modifyNum);
            } else if (dto.getModifyNum() != null) {
                dto.setRollModifyNum(dto.getModifyNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }

            BigDecimal rollBadNum = dto.getRollBadNum();
            if (rollBadNum != null) {
                BigDecimal badNum = rollBadNum.multiply(curlLength);
                dto.setBadNum(badNum);
            } else if (dto.getBadNum() != null) {
                dto.setRollBadNum(dto.getBadNum().divide(curlLength, 2, RoundingMode.HALF_UP));
            }
            
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                BigDecimal StockNum =dto.getStockNum()==null?new BigDecimal(0):dto.getStockNum();
                BigDecimal ModifyNum =dto.getModifyNum()==null?new BigDecimal(0):dto.getModifyNum();
                BigDecimal BadNum =dto.getBadNum()==null?new BigDecimal(0):dto.getBadNum();
                BigDecimal dd= StockNum.add(ModifyNum).subtract(BadNum);
                if(dd.compareTo(new BigDecimal(0))<0){
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }

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
                    stockMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        Cd90Stock newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }

                        newItem.setBaseVale(null);
                        List<Cd90Stock> exist = stockMapper.checkStockListUnic(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            stockMapper.insertStock(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                            continue;
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
