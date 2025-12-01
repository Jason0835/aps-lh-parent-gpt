package com.zlt.aps.lh.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.aps.lh.service.LhTestScheduleResultService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertParamDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportFileDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.dto.LhSpecCodeParamDTO;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhDispatcherLogVo;
import com.zlt.aps.lh.api.domain.vo.LhOrderInsertMachineInfoVO;
import com.zlt.aps.lh.handle.LhScheduleResultCheckHandle;
import com.zlt.aps.lh.service.ILhDispatcherLogService;
import com.zlt.aps.lh.service.LhScheduleAdjustService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
@Slf4j
@Api(tags = "硫化Test排程")
@RestController
@RequestMapping("/lhTestScheduleResult")
public class LhTestScheduleResultController extends AbstractDocBizController<LhTestScheduleResult> {


    @Autowired
    private LhTestScheduleResultService lhTestScheduleResultService;

    @Autowired
    RedisService redisService;


    @ApiOperation("自动排程")
    @PostMapping("/autoLhTestScheduleResult")
    public AjaxResult autoLhTestScheduleResult(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO){
        lhTestScheduleResultService.autoLhTestScheduleResult(autoLhScheduleResultDTO);
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return lhTestScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "LH2025213";
    }
}
