package com.zlt.aps.mp.i18n.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.client.RedissonLockClient;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.common.core.constant.I18nConstant;
import com.zlt.aps.maindata.service.I18nChangeService;
import com.zlt.aps.monthplan.api.domain.entity.I18nChange;
import com.zlt.aps.monthplan.api.domain.vo.I18nJsonVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Api(tags = "国际化变更Controller")
@RestController
@RequestMapping("/i18nChange")
public class I18nChangeController extends BaseController<I18nChange> {

    @Autowired
    private I18nChangeService i18nChangeService;

    @Autowired
    private RedissonLockClient redissonLockClient;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody I18nChange query) {
        try {
            startPage(getOrderBy(query));
            List<I18nChange> list = i18nChangeService.selectList(query);
            return getDataTable(list);
        } finally {
            clearPage();
        }
    }

    protected String getOrderBy(I18nChange queryVO) {
        Map<String, Object> params = queryVO.getParams();
        if (params != null && params.containsKey("orderBy")) {
            String orderByField = (String) params.get("orderBy");
            String dbField = AppUtils.transCamelCase(orderByField);
            String isAscStr = (String) params.get("isAsc");
            return dbField + " " + (isAscStr.equals("1") ? "asc" : "desc");
        } else {
            return null;
        }
    }

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/selectRelList")
    List<I18nChange> selectRelList(@RequestBody I18nChange query) {
        return i18nChangeService.selectList(query);
    }

    @ApiOperation("查询单条数据")
    @GetMapping(value = "/{id}")
    public I18nChange getInfo(@PathVariable("id") Long id) {
        return i18nChangeService.getInfo(id);
    }

    @Log(title = "ui.data.column.i18nChange.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody I18nChange change) {
        RLock lock = redissonLockClient.getLock(I18nConstant.REDIS_I18N_CHANGE_LOCK);
        try {
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.i18nChange.changeError"));
            }

            return i18nChangeService.save(change);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.save.error.msg"));
        } finally {
            // 释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询页面JSON
     */
    @ApiOperation("查询页面JSON")
    @PostMapping("/pageJson")
    public AjaxResult pageJson(@RequestBody I18nJsonVo jsonVo) {
        return i18nChangeService.pageJson(jsonVo);
    }

    /**
     * 下载国际化
     */
    @GetMapping("/download")
    @ApiOperation("下载国际化")
    public byte[] download() {
        return i18nChangeService.download();
    }

}
