package com.tw.darkhorse.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Api("测试接口文档")
@RestController
@RequestMapping("/test")
public class TestController {


    @GetMapping
    public String getTestStr() {
        return "hello word";
    }
}
