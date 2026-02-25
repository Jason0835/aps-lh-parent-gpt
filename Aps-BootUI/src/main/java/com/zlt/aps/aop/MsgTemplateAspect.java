package com.zlt.aps.aop;
import com.alibaba.nacos.shaded.com.google.common.reflect.TypeToken;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.service.IMdmMsgTemplateUserRelRemoteService;
import com.zlt.msg.message.domain.entity.MsgTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
public class MsgTemplateAspect{

    @Autowired
    private IMdmMsgTemplateUserRelRemoteService mdmMsgTemplateUserRelRemoteService;

    // 定义切点：拦截 IMsgTemplateRemoteService.list 方法
    @Pointcut("execution(* com.zlt.msg.message.api.IMsgTemplateRemoteService.list(..))")
    public void targetMethod() {}

    // 环绕通知
    // 环绕通知
    @Around("targetMethod()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 执行原方法
        Object result = joinPoint.proceed();

        // 2. 处理结果
        if (result instanceof TableDataInfo) {
            TableDataInfo tableData = (TableDataInfo) result;
            List<Object> rows = (List<Object>) tableData.getRows();

            if (rows != null && !rows.isEmpty()) {

                // 【第一步】提取所有模板编码
                // 安全处理：兼容 Map 和 实体对象 两种情况
                List<String> templateCodes = rows.stream()
                        .map(row -> {
                            if (row instanceof Map) {
                                return ((Map<String, Object>) row).get("templateCode") != null
                                        ? ((Map<String, Object>) row).get("templateCode").toString()
                                        : null;
                            } else if (row instanceof MsgTemplate) {
                                return ((MsgTemplate) row).getTemplateCode();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                // 【第二步】批量获取用户 Map
                if (!templateCodes.isEmpty()) {
                    Map<String, String> userMap = mdmMsgTemplateUserRelRemoteService.batchGetAssociatedUsers(templateCodes);

                    if (userMap != null && !userMap.isEmpty()) {

                        // 【第三步】循环塞值
                        // 因为我们不确定 rows 里面是 LinkedHashMap 还是 MsgTemplate，
                        // 最稳妥的方法是统一转成 Map 再操作。

                        List<Map<String, Object>> newRows = new ArrayList<>();
                        for (Object row : rows) {
                            Map<String, Object> map;

                            // 如果本来就是 Map，直接用
                            if (row instanceof Map) {
                                map = (Map<String, Object>) row;
                            }
                            // 如果是实体对象，转成 Map (可以用 Gson, Jackson, 或者 BeanMap)
                            else {
                                // 这里用 Gson 转换比较方便，或者用 new BeanMap(row)
                                map = new Gson().fromJson(new Gson().toJson(row), new TypeToken<Map<String, Object>>(){}.getType());
                            }

                            // 塞入 userName 字段
                            Object codeObj = map.get("templateCode");
                            if (codeObj != null) {
                                String users = userMap.get(codeObj.toString());
                                map.put("userName", users);
                            }

                            newRows.add(map);
                        }

                        // 【第四步】更新结果集
                        tableData.setRows(newRows);
                    }
                }
            }
        }
        return result;
    }

}
