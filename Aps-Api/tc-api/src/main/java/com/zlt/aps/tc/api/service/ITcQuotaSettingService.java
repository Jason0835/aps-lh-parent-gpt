package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcQuotaSetting;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎侧定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-28
 */
@FeignClient(contextId = "ITcQuotaSettingService" , value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcQuotaSettingService {


    /**
     * 查询胎侧定额设定列表
     */
    @PostMapping("/tc/quota/list")
    TableDataInfo list(@RequestBody TcQuotaSetting tcQuotaSetting);


    /**
     * 新增胎侧定额设定
     */
    @PostMapping("/tc/quota/add")
    AjaxResult add(@RequestBody TcQuotaSetting tcQuotaSetting);


    /**
     * 修改胎侧定额设定
     */
    @PostMapping("/tc/quota/edit")
    AjaxResult edit(@RequestBody TcQuotaSetting tcQuotaSetting);


    /**
     * 删除胎侧定额设定
     */
    @DeleteMapping("/tc/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tc/quota/{id}")
    TcQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验胎侧定额设定唯一性
     */
    @PostMapping("/tc/quota/checkTcQuotaSettingUnique")
    String checkTcQuotaSettingUnique(@RequestBody TcQuotaSetting tcQuotaSetting);


    /**
     * 导出胎侧定额设定列表
     */
    @PostMapping("/tc/quota/getList")
    List<TcQuotaSetting> getList(@RequestBody TcQuotaSetting tcQuotaSetting);

    /**
     * 数据导入
     */
    @PostMapping("/tc/quota/importData")
    AjaxResult importData(@RequestBody List<TcQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);



}
