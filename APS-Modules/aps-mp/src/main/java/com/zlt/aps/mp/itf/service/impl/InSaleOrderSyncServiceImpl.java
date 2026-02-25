package com.zlt.aps.mp.itf.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.constant.SyncResultCodeConstant;
import com.zlt.aps.maindata.mapper.ItfInterfaceLogEntityMapper;
import com.zlt.aps.mp.api.domain.itf.*;
import com.zlt.aps.mp.demand.service.IMonthPlanSaleOrderService;
import com.zlt.aps.mp.factory.service.IMpHistorySaleQtyService;
import com.zlt.aps.mp.itf.InSaleOrderApiRequestConfig;
import com.zlt.aps.mp.itf.RequestConfigStrategy;
import com.zlt.aps.mp.itf.service.IInSaleOrderSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 内销销售订单实现类
 *
 * @author Chen
 * @date 2025/4/8
 */
@Slf4j
@Service
public class InSaleOrderSyncServiceImpl implements IInSaleOrderSyncService {

    @Value("${itf.inSaleOrder.url}")
    private String inSaleOrderUrl;

    @Value("${itf.inHisSaleOrder.url}")
    private String inHisSaleOrderUrl;

    @Autowired
    private IMonthPlanSaleOrderService monthPlanSaleOrderService;

    @Autowired
    private IMpHistorySaleQtyService mpHistorySaleQtyService;

    @Autowired
    private ItfInterfaceLogEntityMapper interfaceLogEntityMapper;

    /**
     * 将参数转成url参数
     *
     * @param obj 对象
     * @return 结果
     * @throws IllegalAccessException       异常
     * @throws UnsupportedEncodingException 异常
     */
    public static String objectToUrlParams(Object obj) throws IllegalAccessException, UnsupportedEncodingException {
        List<String> params = new ArrayList<>();
        // 获取对象的所有字段
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 设置可访问私有字段
            field.setAccessible(true);
            Object value = field.get(obj);
            if (!"serialVersionUID".equals(field.getName()) && value != null) {
                // 对参数名和参数值进行 URL 编码
                String paramName = URLEncoder.encode(field.getName(), StandardCharsets.UTF_8.name());
                String paramValue = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name());
                params.add(paramName + "=" + paramValue);
            }
        }
        // 使用 & 符号连接所有参数
        return String.join("&", params);
    }

    /**
     * 同步内销销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     */
    @Override
    public AjaxResult syncInSaleOrder(InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException {
        log.info("==========同步内销销售订单接口start==========");
        InSaleOrderRequestVo requestVo = new InSaleOrderRequestVo();
        requestVo.setYears(inSaleOrderDto.getYears());
        requestVo.setMonths(inSaleOrderDto.getMonths());
        requestVo.setPageNo(1);
        String requestBody = "";
        String toUrlParams = objectToUrlParams(requestVo);
        InSaleOrderApiRequestConfig requestConfigStrategy = new InSaleOrderApiRequestConfig(inSaleOrderUrl + "?" + toUrlParams, null, requestBody);
        ResultVo<ListDataVo<InDataListVo>> resultVo = syncProcess(requestConfigStrategy, "内销销售订单");
        log.info("处理内销销售订单返回数据start");
        if (SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(resultVo.getCode())) {
            ListDataVo<InDataListVo> data = resultVo.getData();
            // 第一页的数据
            List<InDataListVo> inDataListVoList = data.getData();
            monthPlanSaleOrderService.handleInSaleOrderSyncResultData(inSaleOrderDto, inDataListVoList);
            log.info("处理内销销售订单返回数据end");
            Integer totalPage = data.getTotalPage();
            for (int i = 2; i <= totalPage; i++) {
                log.info("内销销售订单第{}页请求start", i);
                requestVo.setPageNo(i);
                toUrlParams = objectToUrlParams(requestVo);
                requestConfigStrategy.setRequestUrl(inSaleOrderUrl + "?" + toUrlParams);
                resultVo = syncProcess(requestConfigStrategy, "内销销售订单");
                if (SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(resultVo.getCode())) {
                    data = resultVo.getData();
                    // 第i页的数据
                    inDataListVoList = data.getData();
                    log.info("处理内销销售订单第{}页返回数据start", i);
                    monthPlanSaleOrderService.handleInSaleOrderSyncResultData(inSaleOrderDto, inDataListVoList);
                    log.info("处理内销销售订单第{}页返回数据end", i);
                }
                log.info("内销销售订单第{}页请求end", i);
            }
        }
        log.info("==========同步内销销售订单接口end==========");
        return AjaxResult.success();
    }

    /**
     * 同步内销历史销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult syncInHisSaleOrder(InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException {
        log.info("==========同步内销销售订单接口start==========");
        InHisSaleOrderRequestVo requestVo = new InHisSaleOrderRequestVo();
        requestVo.setDates1(inSaleOrderDto.getDates1());
        requestVo.setDates2(inSaleOrderDto.getDates2());
        requestVo.setPageNo(1);
        String requestBody = "";
        String toUrlParams = objectToUrlParams(requestVo);
        InSaleOrderApiRequestConfig requestConfigStrategy = new InSaleOrderApiRequestConfig(inHisSaleOrderUrl + "?" + toUrlParams, null, requestBody);
        ResultVo<ListDataVo<InDataListVo>> resultVo = syncProcess(requestConfigStrategy, "内销历史销售订单");
        log.info("处理内销历史销售订单返回数据start");
        if (SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(resultVo.getCode())) {
            ListDataVo<InDataListVo> data = resultVo.getData();
            // 第一页的数据
            List<InDataListVo> inDataListVoList = data.getData();
            mpHistorySaleQtyService.handleInHisSaleOrderSyncResultData(inSaleOrderDto, inDataListVoList);
            log.info("处理内销历史销售订单返回数据end");
            Integer totalPage = data.getTotalPage();
            for (int i = 2; i <= totalPage; i++) {
                log.info("内销历史销售订单第{}页请求start", i);
                requestVo.setPageNo(i);
                toUrlParams = objectToUrlParams(requestVo);
                requestConfigStrategy.setRequestUrl(inHisSaleOrderUrl + "?" + toUrlParams);
                resultVo = syncProcess(requestConfigStrategy, "内销历史销售订单");
                if (SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(resultVo.getCode())) {
                    data = resultVo.getData();
                    // 第i页的数据
                    inDataListVoList = data.getData();
                    log.info("处理内销历史销售订单第{}页返回数据start", i);
                    mpHistorySaleQtyService.handleInHisSaleOrderSyncResultData(inSaleOrderDto, inDataListVoList);
                    log.info("处理内销历史销售订单第{}页返回数据end", i);
                }
                log.info("内销历史销售订单第{}页请求end", i);
            }
        }
        log.info("==========同步内销历史销售订单接口end==========");
        return AjaxResult.success();
    }

    /**
     * 同步接口
     *
     * @param requestConfigStrategy 请求策略
     * @return 结果
     */
    private ResultVo<ListDataVo<InDataListVo>> syncProcess(RequestConfigStrategy requestConfigStrategy, String interfaceName) {
        HttpHeaders headers = requestConfigStrategy.getHeaders();
        RestTemplate restTemplate = new RestTemplate();
        // 创建 HttpEntity 对象，包含请求头和请求体
        String url = requestConfigStrategy.getRequestUrl();
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(new HashMap<>(16), headers);
        ItfInterfaceLog interfaceLog = new ItfInterfaceLog();
        interfaceLog.setBaseVale(null);
        interfaceLog.setRequestTime(new Date());
        interfaceLog.setInterfaceName(interfaceName);
        interfaceLog.setRequestUrl(url);
        interfaceLog.setRequestMethod(HttpMethod.POST.name());
        interfaceLog.setRequestHeaders(JSON.toJSONString(requestConfigStrategy.getHeaders()));
        interfaceLog.setRequestBody(requestConfigStrategy.getRequestBody());
        // 记录开始时间
        long start = System.currentTimeMillis();
        try {
            log.info("==========同步接口操作：{}==========", requestConfigStrategy.getLogString());
            // 发送 POST 请求并获取响应
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            // 计算耗时
            long times = System.currentTimeMillis() - start;
            String body = response.getBody();
            log.info("==========同步接口操作：接口返回结果：{}==========", body);
            // 处理返回结果
            ResultVo<ListDataVo<InDataListVo>> syncResponseBodyVo = JSON.parseObject(body, new TypeReference<ResultVo<ListDataVo<InDataListVo>>>() {
            });
            if (syncResponseBodyVo != null) {
                // 保存日志
                Integer code = syncResponseBodyVo.getCode();
                interfaceLog.setResponseTime(new Date());
                interfaceLog.setResponseStatusCode(response.getStatusCode().value());
                interfaceLog.setResponseHeaders(JSON.toJSONString(response.getHeaders()));
                interfaceLog.setResponseBody(body);
                interfaceLog.setRequestDurationMs(interfaceLog.getResponseTime().getTime() - interfaceLog.getRequestTime().getTime());
                interfaceLog.setIsSuccess(SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(code)
                        ? Constant.TRUE : Constant.FALSE);
                interfaceLogEntityMapper.insert(interfaceLog);
                if (!SyncResultCodeConstant.IN_SALE_ORDER_SUCCESS_CODE.equals(code)) {
                    throw new RuntimeException(syncResponseBodyVo.getMessage());
                } else {
                    return syncResponseBodyVo;
                }
            }
            throw new RuntimeException("接口返回结果为空");
        } catch (Exception exception) {
            exception.printStackTrace();
            // 计算耗时
            long times = System.currentTimeMillis() - start;
            interfaceLog.setResponseTime(new Date());
            interfaceLog.setResponseStatusCode(null);
            interfaceLog.setResponseHeaders("");
            interfaceLog.setRequestDurationMs(interfaceLog.getResponseTime().getTime() - interfaceLog.getRequestTime().getTime());
            interfaceLog.setIsSuccess(Constant.FALSE);
            String message = exception.getMessage();
            if (message.length() > 1000) {
                message = message.substring(0, 1000);
            }
            interfaceLog.setResponseBody(message);
            interfaceLogEntityMapper.insert(interfaceLog);
            throw new RuntimeException("error message:" + exception.getMessage());
        }
    }

    /**
     * 同步外销销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult syncOutSaleOrder(InSaleOrderDto inSaleOrderDto) {
        return monthPlanSaleOrderService.syncOutSaleOrder(inSaleOrderDto);
    }
}
