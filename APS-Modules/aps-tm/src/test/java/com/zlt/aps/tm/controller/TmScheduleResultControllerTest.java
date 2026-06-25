package com.zlt.aps.tm.controller;

import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * 胎面排程结果控制器结构测试。
 *
 * <p>不启动 Spring 容器，仅校验本轮新增接口仍挂在兼容的 `/tmScheduleResult` 路径下。</p>
 */
public class TmScheduleResultControllerTest {

    @Test
    public void controllerShouldKeepCompatibleBasePath() {
        RequestMapping requestMapping = TmScheduleResultController.class.getAnnotation(RequestMapping.class);

        assertArrayEquals(new String[]{"/tmScheduleResult"}, requestMapping.value());
    }

    @Test
    public void controllerShouldExposeStructuralScheduleEndpoints() throws NoSuchMethodException {
        assertPostPath("validateAutoPlan", "/validateAutoPlan");
        assertPostPath("autoPlan", "/autoPlan");
        assertPostPath("board", "/board");
        assertPostPath("insertTask", "/insertTask");
        assertPostPath("changeQty", "/changeQty");
        assertPostPath("publishValidate", "/publishValidate");
        assertPostPath("publish", "/publish");
    }

    /**
     * 校验指定方法的 PostMapping 路径。
     *
     * @param methodName 方法名称
     * @param path       期望路径
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    private void assertPostPath(String methodName, String path) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);

        assertEquals(path, postMapping.value()[0]);
    }

    /**
     * 按方法名查找控制器方法。
     *
     * @param methodName 方法名称
     * @return 控制器方法
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    private Method findMethod(String methodName) throws NoSuchMethodException {
        for (Method method : TmScheduleResultController.class.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
