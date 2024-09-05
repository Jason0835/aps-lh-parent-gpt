package com.zlt.aps.tm.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.mapper.TmStockMapper;
import com.zlt.aps.tm.service.TmStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 胎面库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class TmStockServiceImpl implements TmStockService {

    @Autowired
    private TmStockMapper tTmStockMapper;


    /**
     * 查询胎面库存信息
     *
     * @param id 胎面库存信息ID
     * @return 胎面库存信息
     */
    @Override
    public TmStock selectTmStockById(Long id) {
        return tTmStockMapper.selectTmStockById(id);
    }

    /**
     * 查询胎面库存信息列表
     *
     * @param tTmStock 胎面库存信息
     * @return 胎面库存信息
     */
    @Override
    public List<TmStock> selectTmStockList(TmStock tTmStock) {
        if (StringUtils.isNotEmpty(tTmStock.getEndTime())) {
            tTmStock.setEndTime(tTmStock.getEndTime() + " 23:59:59");
        }
        return tTmStockMapper.selectTmStockList(tTmStock);
    }

    /**
     * 新增胎面库存信息
     *
     * @param tTmStock 胎面库存信息
     * @return 结果
     */
    @Override
    public int insertTmStock(TmStock tTmStock) {
        tTmStock.setBaseVale(null);
        return tTmStockMapper.insertTmStock(tTmStock);
    }

    /**
     * 修改胎面库存信息
     *
     * @param tTmStock 胎面库存信息
     * @return 结果
     */
    @Override
    public int updateTmStock(TmStock tTmStock) {
        tTmStock.setBaseVale(tTmStock.getId());
        return tTmStockMapper.updateTmStock(tTmStock);
    }

    /**
     * 批量删除胎面库存信息
     *
     * @param ids 需要删除的胎面库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTmStockByIds(Long[] ids) {
        return tTmStockMapper.deleteTmStockByIds(ids);
    }

    /**
     * 删除胎面库存信息信息
     *
     * @param id 胎面库存信息ID
     * @return 结果
     */
    @Override
    public int deleteTmStockById(Long id) {
        return tTmStockMapper.deleteTmStockById(id);
    }

    /**
     * 校验胎面库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TmStock> checkTmStockListUnic(TmStock tTmStock) {
        return tTmStockMapper.checkTmStockListUnic(tTmStock);
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
    public AjaxResult importData(List<TmStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmStock> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getMaterialCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TmStock stock = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(stock.getStockDate()+stock.getMaterialCode());
            if (hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.quota.treadCode");
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
                stock.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tTmStockMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmStock excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    List<TmStock> unic = tTmStockMapper.checkTmStockListUnic(excelItem);
                    if (CollectionUtils.isEmpty(unic)) {
                        //不存在插入
                        successNum++;
                        tTmStockMapper.insertTmStock(excelItem);
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
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
