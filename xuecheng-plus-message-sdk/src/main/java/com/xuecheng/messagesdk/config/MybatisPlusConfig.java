package com.xuecheng.messagesdk.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * <P>
 * 		Mybatis-Plus 配置
 * </p>
 */
@Configuration("messagesdk_mpconfig")
@MapperScan(basePackages = "com.xuecheng.messagesdk.mapper",annotationClass = Mapper.class)
public class MybatisPlusConfig {


}