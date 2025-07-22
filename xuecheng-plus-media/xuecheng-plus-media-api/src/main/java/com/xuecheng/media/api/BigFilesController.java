package com.xuecheng.media.api;


import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * 大文件上传接口
 */
@Api(tags = "大文件上传", value = "大文件上传")
@RestController
public class BigFilesController {


    @Autowired
    private MediaFileService mediaFileService;

    @ApiOperation("文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkFile(@RequestParam("fileMd5") String fileMd5) {
        return mediaFileService.checkFile(fileMd5);
    }

    @ApiOperation("分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkChunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) {
        return mediaFileService.checkChunk(fileMd5, chunk);
    }

    @ApiOperation("上传分块文件")
    @PostMapping(value = "/upload/uploadchunk")
    public RestResponse<Boolean> uploadChunk(@RequestParam("file") MultipartFile file,
                                             @RequestParam("fileMd5") String fileMd5,
                                             @RequestParam("chunk") int chunk) {

        return mediaFileService.uploadChunk(fileMd5, chunk, file);
    }

    @ApiOperation("合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse<String> mergeChunks(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("fileName") String fileName,
                                            @RequestParam("chunkTotal") int chunkTotal) {
        Long companyId = 1232141425L;
        UploadFileParamsDto dto = new UploadFileParamsDto();
        dto.setFileType("001002");
        dto.setTags("课程视频");
        dto.setFilename(fileName);
        return mediaFileService.mergeChunks(companyId, fileMd5, chunkTotal, dto);
    }
}
