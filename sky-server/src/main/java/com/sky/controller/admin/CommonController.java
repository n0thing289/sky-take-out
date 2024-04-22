package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "公共的接口")
@Slf4j
public class CommonController {

    @Resource
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("菜品图片上传")
    public Result uploadDishDish(MultipartFile file) {
        log.info("图片上传: file={}", file);
        try{
            String originalFilename = file.getOriginalFilename();
            //截取后缀名
            String extra = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构建文件名字
            String objectName = UUID.randomUUID() + extra;
            String uploadUrl = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(uploadUrl);
        }catch (IOException e){
            log.info(MessageConstant.UPLOAD_FAILED + ": {}", e.getMessage());
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
