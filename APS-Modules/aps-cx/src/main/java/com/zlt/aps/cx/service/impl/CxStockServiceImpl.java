package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.mapper.CxProductConstructionInfoMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.service.CxStockService;
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
 * 成型库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class CxStockServiceImpl implements CxStockService {

    @Autowired
    private CxStockMapper cxStockMapper;

    @Autowired
    private CxProductConstructionInfoMapper cxProductConstructionInfoMapper;

    /**
     * 查询成型库存信息
     *
     * @param id 成型库存信息ID
     * @return 成型库存信息
     */
    @Override
    public CxStock selectCxStockById(Long id) {
        return cxStockMapper.selectCxStockById(id);
    }

    /**
     * 查询成型库存信息列表
     *
     * @param cxStock 成型库存信息
     * @return 成型库存信息
     */
    @Override
    public List<CxStock> selectCxStockList(CxStock cxStock) {
        if (StringUtils.isNotEmpty(cxStock.getEndTime())) {
            cxStock.setEndTime(cxStock.getEndTime() + " 23:59:59");
        }
        return cxStockMapper.selectCxStockList(cxStock);
    }

    /**
     * 新增成型库存信息
     *
     * @param cxStock 成型库存信息
     * @return 结果
     */
    @Override
    public int insertCxStock(CxStock cxStock) {
        cxStock.setBaseVale(null);
        return cxStockMapper.insertCxStock(cxStock);
    }

    /**
     * 修改成型库存信息
     *
     * @param cxStock 成型库存信息
     * @return 结果
     */
    @Override
    public int updateCxStock(CxStock cxStock) {
        cxStock.setBaseVale(cxStock.getId());
        return cxStockMapper.updateCxStock(cxStock);
    }

    /**
     * 批量删除成型库存信息
     *
     * @param ids 需要删除的成型库存信息ID
     * @return 结果
     */
    @Override
    public int deleteCxStockByIds(Long[] ids) {
        return cxStockMapper.deleteCxStockByIds(ids);
    }

    /**
     * 删除成型库存信息信息
     *
     * @param id 成型库存信息ID
     * @return 结果
     */
    @Override
    public int deleteCxStockById(Long id) {
        return cxStockMapper.deleteCxStockById(id);
    }

    /**
     * 校验成型库存唯一性（根据库存日期+物料编号+id）
     */
    public List<CxStock> checkCxStockListUnic(CxStock cxStock) {
        return cxStockMapper.checkCxStockListUnic(cxStock);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxStock> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxStock> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate()+a.getEmbryoCode()+a.getBomDataVersion()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            CxStock dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getStockDate()+dto.getEmbryoCode()+dto.getBomDataVersion());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.stock.embryoCode");
                String columnName3 = I18nUtil.getMessage("ui.data.column.productStatus.bomDataVersion");
                message=String.format(message,columnName+"+"+columnName2+"+"+columnName3);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                Long StockNum =dto.getStockNum()==null?0L:dto.getStockNum();
                Long ModifyNum =dto.getModifyNum()==null?0L:dto.getModifyNum();
                Long BadNum =dto.getBadNum()==null?0L:Long.valueOf(dto.getBadNum());
                Long dd= StockNum+ModifyNum-BadNum;
                if(dd<0){
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }
                //校验胎胚版本是否存在
                //Joran 2021-12-16 与测试沟通确认导入不进行施工版本校验，先让导入进去
                /*CxProductConstructionInfo pc=new CxProductConstructionInfo();
                pc.setEmbryoCode(dto.getEmbryoCode());
                pc.setEmbryoVersion(dto.getBomDataVersion());
                List<CxProductConstructionInfo> pcList= cxProductConstructionInfoMapper.selectCxProductConstructionInfoList(pc);
                if (CollectionUtils.isEmpty(pcList)) {
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.productConstruction.errorEmbryoVersion"), importErrorLogs);
                    continue;
                }*/
                //Joran 2021-12-16 与测试沟通确认导入不进行施工版本校验，先让导入进去

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
                    cxStockMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxStock newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        List<CxStock> exist = cxStockMapper.checkCxStockListUnic(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
                            successNum++;
                            cxStockMapper.insertCxStock(newItem);
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

    /**
     * 批量释放不可用库存
     *
     * @param ids 需要释放的成型库存信息ID
     * @return 结果
     */
    @Override
    public int releaseStock(Long[] ids) {
        return cxStockMapper.releaseStockByIds(ids, SecurityUtils.getUsername());
    }
}
