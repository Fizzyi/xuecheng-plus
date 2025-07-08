package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程查询参数Dto
 * @date 2022/9/6 14:36
 */
@Data
@ToString
public class QueryCourseParamsDto {

    @ApiModelProperty(value = "课程审核状态")
    private String auditStatus;
    @ApiModelProperty(value = "课程名称")
    private String courseName;
    @ApiModelProperty(value = "课程发布状态")
    private String publishStatus;

}