package com.zlt.aps.aop;

import com.zlt.aps.monthplan.api.service.IMdmMsgTemplateUserRelRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Intercepts({
        @Signature(
                type = ResultSetHandler.class,
                method = "handleResultSets",
                args = {Statement.class}
        )
})
@Slf4j
public class MsgTemplateInterceptor implements Interceptor{

    @Autowired
    private IMdmMsgTemplateUserRelRemoteService mdmMsgTemplateUserRelRemoteService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取 Statement 对象
        Statement statement = (Statement) invocation.getArgs()[0];

        // 2. 获取 Statement ID（精确匹配）
        String statementId = getStatementId(statement);

        // 3. 判断是否为目标方法
        if ("com.zlt.msg.message.api.IMsgTemplateRemoteService.list".equals(statementId)) {
            // 4. 执行原方法获取结果集
            Object result = invocation.proceed();

            if (result != null && result instanceof List) {
                List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
                if (!resultList.isEmpty()) {
                    // 5. 处理结果集
                    processResult(resultList);
                }
            }
            return result;
        }

        // 6. 非目标方法直接返回
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可选：读取配置属性
    }

    // 获取 Statement ID
    private String getStatementId(Statement statement) {
        try {
            MetaObject metaObject = SystemMetaObject.forObject(statement);
            Object delegate = metaObject.getValue("delegate");
            if (delegate != null) {
                MetaObject delegateMeta = SystemMetaObject.forObject(delegate);
                return (String) delegateMeta.getValue("mappedStatement.id");
            }
        } catch (Exception e) {
            // 记录日志但不抛出异常
            log.warn("获取 Statement ID 失败", e);
        }
        return null;
    }

    // 处理结果集
    private void processResult(List<Map<String, Object>> resultList) {
        // 获取模板ID列表
        List<Object> templateIds = resultList.stream()
                .map(row -> row.get("template_id"))
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (templateIds.isEmpty()) {
            return;
        }

        // 批量查询关联用户（在 APS 项目中实现）
//        Map<Object, String> userMap = apsUserService.batchGetAssociatedUsers(templateIds);
//
//        // 添加关联用户字段
//        for (Map<String, Object> row : resultList) {
//            Object templateId = row.get("template_id");
//            if (templateId != null && userMap.containsKey(templateId)) {
//                row.put("associated_users", userMap.get(templateId));
//            }
//        }
    }
}
