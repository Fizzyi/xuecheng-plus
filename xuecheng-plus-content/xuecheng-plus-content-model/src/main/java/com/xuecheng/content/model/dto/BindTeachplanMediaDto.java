package com.xuecheng.content.model.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "BindTeachplanMediaDto", description = "教学计划-媒资绑定提交数据")
public class BindTeachplanMediaDto {

    @ApiModelProperty(value = "媒资文件Id", required = true)
    private String mediaId;
    @ApiModelProperty(value = "媒资文件名称", required = true)
    private String fileName;
    @ApiModelProperty(value = "课程计划标识", required = true)
    private Long teachplanId;

}
