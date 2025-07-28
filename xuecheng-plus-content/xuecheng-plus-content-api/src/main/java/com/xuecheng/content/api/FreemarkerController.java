package com.xuecheng.content.api;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView testFreemarker(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("test");
        modelAndView.addObject("name","zhaohangyi");
        return modelAndView;
    }
}
