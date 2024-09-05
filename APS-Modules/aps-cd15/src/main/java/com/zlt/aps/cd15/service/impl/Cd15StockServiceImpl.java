package com.zlt.aps.cd15.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.mapper.Cd15StockMapper;
import com.zlt.aps.cd15.service.Cd15StockService;
import com.zlt.aps.common.core.utils.ImportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 15°裁断库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class Cd15StockServiceImpl implements Cd15StockService {
    @Autowired
    private Cd15StockMapper stockMapper;

    /**
     * 查询15°裁断库存信息
     *
     * @param id 15°裁断库存信息ID
     * @return 15°裁断库存信息
     */
    @Override
    public Cd15Stock selectStockById(Long id) {
        return stockMapper.selectStockById(id);
    }

    /**
     * 查询15°裁断库存信息列表
     *
     * @param Cd15Stock 15°裁断库存信息
     * @return 15°裁断库存信息
     */
    @Override
    public List<Cd15Stock> selectStockList(Cd15Stock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        return stockMapper.selectStockList(stock);
    }

    /**
     * 新增15°裁断库存信息
     *
     * @param Cd15Stock 15°裁断库存信息
     * @return 结果
     */
    @Override
    public int insertStock(Cd15Stock stock) {
        stock.setBaseVale(null);
        return stockMapper.insertStock(stock);
    }

    /**
     * 修改15°裁断库存信息
     *
     * @param stock 15°裁断库存信息
     * @return 结果
     */
    @Override
    public int updateStock(Cd15Stock stock) {
        stock.setBaseVale(stock.getId());
        return stockMapper.updateStock(stock);
    }

    /**
     * 批量删除15°裁断库存信息
     *
     * @param ids 需要删除的15°裁断库存信息ID
     * @return 结果
     */
    @Override
    public int deleteStockByIds(Long[] ids) {
        return stockMapper.deleteStockByIds(ids);
    }

    /**
     * 校验15°裁断库存唯一性（根据库存日期+物料编号+id）
     */
    public List<Cd15Stock> checkStockListUnic(Cd15Stock stock) {
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
    public AjaxResult importData(List<Cd15Stock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15Stock> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getMaterialCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            Cd15Stock stock = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(stock.getStockDate()+stock.getMaterialCode());
            if (hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.common.column.gy.steelStripCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, stock);
            if (CollectionUtils.isEmpty(validated)) {

                BigDecimal StockNum =stock.getStockNum()==null?new BigDecimal(0):stock.getStockNum();
                BigDecimal ModifyNum =stock.getModifyNum()==null?new BigDecimal(0):stock.getModifyNum();
                BigDecimal BadNum =stock.getBadNum()==null?new BigDecimal(0):stock.getBadNum();
                BigDecimal dd= StockNum.add(ModifyNum).subtract(BadNum);
                if(dd.compareTo(new BigDecimal(0))<0){
                    failureNum++;
                    stock.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }

                stock.setBaseVale(null);
                importList.add(stock);
            } else {
                failureNum++;
                // 设置错误标识
                stock.setId(-999L);
                importErrorLogs.addAll(validated);
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
                        Cd15Stock excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)){
                            continue;
                        }

                        // 唯一性校验
                        List<Cd15Stock> unic = stockMapper.checkStockListUnic(excelItem);
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
}
