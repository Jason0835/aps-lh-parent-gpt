package com.zlt.aps.gdyy.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.entity.GdyyParams;
import com.zlt.aps.gdyy.mapper.GdyyParamsMapper;
import com.zlt.aps.gdyy.mapper.GdyyStockMapper;
import com.zlt.aps.gdyy.service.GdyyStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢带压延库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class GdyyStockServiceImpl implements GdyyStockService {
    @Autowired
    private GdyyStockMapper stockMapper;
    @Autowired
    private GdyyParamsMapper gdyyParamsMapper;

    /**
     * 查询钢带压延库存信息
     *
     * @param id 钢带压延库存信息ID
     * @return 钢带压延库存信息
     */
    @Override
    public GdyyStock selectStockById(Long id) {
        return stockMapper.selectStockById(id);
    }

    /**
     * 查询钢带压延库存信息列表
     *
     * @param GdyyStock 钢带压延库存信息
     * @return 钢带压延库存信息
     */
    @Override
    public List<GdyyStock> selectStockList(GdyyStock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        return stockMapper.selectStockList(stock);
    }

    /**
     * 新增钢带压延库存信息
     *
     * @param stock 钢带压延库存信息
     * @return 结果
     */
    @Override
    public int insertStock(GdyyStock stock) {
        stock.setBaseVale(null);
        return stockMapper.insertStock(stock);
    }

    /**
     * 修改钢带压延库存信息
     *
     * @param stock 钢带压延库存信息
     * @return 结果
     */
    @Override
    public int updateStock(GdyyStock stock) {
        stock.setBaseVale(stock.getId());
        return stockMapper.updateStock(stock);
    }

    /**
     * 批量删除钢带压延库存信息
     *
     * @param ids 需要删除的钢带压延库存信息ID
     * @return 结果
     */
    @Override
    public int deleteStockByIds(Long[] ids) {
        return stockMapper.deleteStockByIds(ids);
    }

    /**
     * 校验钢带压延库存唯一性（根据库存日期+物料编号+id）
     */
    public List<GdyyStock> checkStockListUnic(GdyyStock stock) {
        return stockMapper.checkStockListUnic(stock);
    }


    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GdyyStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyyStock> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(v -> (v.getMaterialCode() + DateUtil.formatDate(v.getStockDate())), Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GdyyStock stock = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(stock.getMaterialCode() + DateUtil.formatDate(stock.getStockDate()));
			// 输入空值当0处理
			stock.setStockRollNum(Optional.ofNullable(stock.getStockRollNum()).orElse(BigDecimal.ZERO));
			stock.setModifyNum(Optional.ofNullable(stock.getModifyNum()).orElse(BigDecimal.ZERO));
			stock.setBadNum(Optional.ofNullable(stock.getBadNum()).orElse(BigDecimal.ZERO));
			if (hasValue > 1) {
                stock.setId(-999L);
				String columnName1 = I18nUtil.getMessage("ui.common.column.gy.bigRollCode");
				String columnName2 = I18nUtil.getMessage("ui.data.column.stock.stockDate");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"),
								columnName1 + "+" + columnName2),
						importErrorLogs);
				failureNum++;
				continue;
			}
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, stock);
            if (CollectionUtils.isNotEmpty(validated)) {
                stock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                BigDecimal StockNum =stock.getStockNum()==null?new BigDecimal(0):stock.getStockNum();
                BigDecimal ModifyNum =stock.getModifyNum()==null?new BigDecimal(0):stock.getModifyNum();
                BigDecimal BadNum =stock.getBadNum()==null?new BigDecimal(0):stock.getBadNum();
                BigDecimal dd= StockNum.add(ModifyNum).subtract(BadNum);
				if (dd.compareTo(new BigDecimal(0)) < 0) {
					failureNum++;
					stock.setId(-999L);
					addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"),
							importErrorLogs);
					continue;
				}

                stock.setBaseVale(null);
                importList.add(stock);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    stockMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GdyyStock excelItem = list.get(i);

                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }

                        // 唯一性校验
                        List<GdyyStock> unic = stockMapper.checkStockListUnic(excelItem);
                        if (CollectionUtils.isEmpty(unic)) {
                            //不存在插入
                            successNum++;
                            stockMapper.insertStock(excelItem);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.stock.message.unique"), importErrorLogs);
                            continue;
                        }

                    }
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
     * 判断是否按大卷计算库存
     */
    @Override
    public boolean isRollStock() {
    	GdyyParams params = new GdyyParams();
    	params.setParamCode(EngineConstants.GDYY_STOCK_ROLL_SWITCH);
    	String paramsValue = gdyyParamsMapper.listParams(params).stream().findFirst().map(GdyyParamsDto::getParamValue).orElse("");
        return EngineConstants.GDYY_STOCK_ROLL_SWITCH_ON.equals(paramsValue);
    }
}
