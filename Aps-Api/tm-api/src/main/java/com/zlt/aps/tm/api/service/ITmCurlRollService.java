package com.zlt.aps.tm.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;

/**
 * 胎面卷曲信息对外暴露接口
 */
@FeignClient(contextId = "iTmCurlRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmCurlRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @GetMapping("/curlRoll/listCurlRoll")
    TableDataInfo listCurlRoll(@SpringQueryMap TmCurlRoll dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/curlRoll/getCurlRoll/{id}")
    TmCurlRoll getCurlRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/curlRoll/saveCurlRoll")
    AjaxResult saveCurlRoll(@RequestBody TmCurlRoll dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/curlRoll/checkCurlRollCodeUnique")
    String checkCurlRollCodeUnique(@RequestBody TmCurlRoll dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/curlRoll/deleteCurlRoll/{ids}")
    AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/curlRoll/exportData")
    List<TmCurlRoll> exportData(@SpringQueryMap TmCurlRoll dto);

    /**
     * 导入数据
     */
    @PostMapping("/curlRoll/importData")
    public AjaxResult importData(@RequestBody List<TmCurlRoll> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
