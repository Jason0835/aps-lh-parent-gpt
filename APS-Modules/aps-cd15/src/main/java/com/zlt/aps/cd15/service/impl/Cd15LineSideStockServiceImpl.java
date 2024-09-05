package com.zlt.aps.cd15.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;
import com.zlt.aps.cd15.common.handle.Cd15SyncDataHandle;
import com.zlt.aps.cd15.mapper.Cd15LineSideStockMapper;
import com.zlt.aps.cd15.service.Cd15LineSideStockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.domain.SyncDataLogs;
import com.zlt.aps.common.engine.service.SyncDataLogsService;

/**
 * 15°裁断库存信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-31
 */
@Service
public class Cd15LineSideStockServiceImpl implements Cd15LineSideStockService {
    @Autowired
    private Cd15LineSideStockMapper cd15LineSideStockMapper;
    @Autowired
    private Cd15SyncDataHandle cd15SyncDataHandle;
    @Autowired
    private SyncDataLogsService syncDataLogsService;

    /**
     * 查询15°裁断库存信息列表
     *
     * @param Cd15Stock 15°裁断库存信息
     * @return 15°裁断库存信息
     */
    @Override
    public List<Cd15LineSideStock> selectStockList(Cd15LineSideStock stock) {
        if (StringUtils.isNotEmpty(stock.getEndTime())) {
            stock.setEndTime(stock.getEndTime() + " 23:59:59");
        }
        return cd15LineSideStockMapper.selectStockList(stock);
    }


    /**
     * 到MES同步库存数据
     *
     * @return 
     */
    public AjaxResult syncStock() {
    	AjaxResult ajaxResult = cd15SyncDataHandle.syncLineSideStock(); // 调用结果
    	if (ajaxResult != null && new Integer(HttpStatus.SUCCESS).equals(ajaxResult.get(AjaxResult.CODE_TAG))) {
    		Object data = ajaxResult.get(AjaxResult.DATA_TAG);
    		if (data != null && !data.toString().equals("")) {
    			String dataVersion = String.valueOf(data);
    			SyncDataLogs logs = syncDataLogsService.getReqDataResult(dataVersion); // 检测生成的版本状态
    			if (ApsConstant.IS_RELEASE.equals(logs.getStatus())) { // 成功
    				String userName = SecurityUtils.getUsername();
    				cd15LineSideStockMapper.deleteCd15LineSideStock(dataVersion, userName); // 删除当天数据
    				cd15LineSideStockMapper.insertCd15LineSideStock(dataVersion, userName); // 保存数据
    				ajaxResult = AjaxResult.success();
    			} else {
    				// 失败，需要返回异常信息
    				ajaxResult = AjaxResult.error(logs.getMsg());
    			}
    		}
    	}
    	return ajaxResult;
    }
}
