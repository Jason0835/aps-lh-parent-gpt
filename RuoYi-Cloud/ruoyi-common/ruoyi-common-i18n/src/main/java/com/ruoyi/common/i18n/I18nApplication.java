package com.ruoyi.common.i18n;

import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/***
 * @author zhangjinfa
 */
@RestController
@SpringBootApplication
public class I18nApplication {

    public static void main(String[] args){
        SpringApplication.run(I18nApplication.class);
    }

    @RequestMapping("/test")
    public String test(){
        System.out.println(I18nUtil.getMessage("user.login"));
        System.out.println(">>>>>>>>>>>>多个");
        System.out.println(I18nUtil.getMessage("user.msg",new Object[]{"joran.Zhang","太帅了"}));
        return I18nUtil.getMessage("user.msg",new Object[]{"joran.Zhang","太帅了"});
    }

    /*@RequestMapping("/test1")
    public void test1(@Validated Test test, BindingResult result){
        System.out.println(test);
        if (result.hasFieldErrors()){
            result.getFieldErrors().forEach(error -> {
                System.out.print("field："+error.getField());
                System.out.println(" ==> message："+error.getDefaultMessage());
            });
        }

    }*/
}
