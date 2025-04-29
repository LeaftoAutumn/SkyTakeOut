package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 上传文件
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public Result<String> upload(MultipartFile file) {
        log.info("上传文件: {}", file);

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        try {
            // 获取文件后缀名
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 生成新的文件名
            StringBuffer objectName = new StringBuffer("dishImage/");
            objectName.append(UUID.randomUUID().toString());
            objectName.append(suffix);

            // 上传文件到阿里云OSS
            String uploadPath = aliOssUtil.upload(file.getBytes(), objectName.toString());

            log.info("上传文件到阿里云OSS成功：{}", originalFilename);
            return Result.success(uploadPath);
        } catch (IOException e) {
            log.info("上传文件到阿里云OSS失败：{}", originalFilename);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
}
