package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixFinalRubberStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;
import com.zlt.aps.lh.service.MixFinalRubberStockService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼胶库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixFinalRubberStockServiceImpl implements MixFinalRubberStockService
{
    @Autowired
    private MixFinalRubberStockMapper mixFinalRubberStockMapper;

    /**
     * 查询终炼胶库存
     * 
     * @param id 终炼胶库存ID
     * @return 终炼胶库存
     */
    @Override
    public MixFinalRubberStock selectMixFinalRubberStockById(Long id)
    {
        return mixFinalRubberStockMapper.selectMixFinalRubberStockById(id);
    }

    /**
     * 查询终炼胶库存列表
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 终炼胶库存
     */
    @Override
    public List<MixFinalRubberStock> selectMixFinalRubberStockList(MixFinalRubberStock mixFinalRubberStock)
    {
        return mixFinalRubberStockMapper.selectMixFinalRubberStockList(mixFinalRubberStock);
    }

    /**
     * 新增终炼胶库存
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 结果
     */
    @Override
    public int insertMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock)
    {
        mixFinalRubberStock.setBaseVale(null);
        return mixFinalRubberStockMapper.insertMixFinalRubberStock(mixFinalRubberStock);
    }

    /**
     * 修改终炼胶库存
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 结果
     */
    @Override
    public int updateMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock)
    {
        mixFinalRubberStock.setBaseVale(mixFinalRubberStock.getId());
        return mixFinalRubberStockMapper.updateMixFinalRubberStock(mixFinalRubberStock);
    }

    /**
     * 批量删除终炼胶库存
     * 
     * @param ids 需要删除的终炼胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixFinalRubberStockByIds(Long[] ids)
    {
        return mixFinalRubberStockMapper.deleteMixFinalRubberStockByIds(ids);
    }

    /**
     * 删除终炼胶库存信息
     * 
     * @param id 终炼胶库存ID
     * @return 结果
     */
    @Override
    public int deleteMixFinalRubberStockById(Long id)
    {
        return mixFinalRubberStockMapper.deleteMixFinalRubberStockById(id);
    }

    /**
     * 校验终炼胶库存唯一性
     */
    @Override
    public String checkMixFinalRubberStockUnique(MixFinalRubberStock mixFinalRubberStock) {
        if (mixFinalRubberStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixFinalRubberStock> list = mixFinalRubberStockMapper.selectMixFinalRubberStockList(mixFinalRubberStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入终炼胶库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixFinalRubberStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixFinalRubberStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixFinalRubberStock mixFinalRubberStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixFinalRubberStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixFinalRubberStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixFinalRubberStock.setBaseVale(null);
                importList.add(mixFinalRubberStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixFinalRubberStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixFinalRubberStock mixFinalRubberStock = list.get(i);
                    // 错误记录跳过
                    if (mixFinalRubberStock.getId() != null && mixFinalRubberStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixFinalRubberStockUnique(mixFinalRubberStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixFinalRubberStock(mixFinalRubberStock);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
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
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
