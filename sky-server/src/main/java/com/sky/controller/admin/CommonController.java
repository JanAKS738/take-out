package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.result.Result;

import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

  @Value("${sky.filepath}")
//   private String basePath;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){

        log.info("文件上传：{}",file);




        try {

            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名
            String newName = UUID.randomUUID().toString() + extension;

           String filePath =aliOssUtil.upload(file.getBytes(), newName);

           // File dest=new File(basePath+newName);
           // file.transferTo(dest);
//            String fileName="/LearningProjects/GitRepo/"+newName;
//            HashMap imgmap=new HashMap();
//
//
//            imgmap.put("url",fileName);

            return Result.success(filePath);
        } catch (Exception e) {
           log.error("文件上传失败：{}",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }


//    @GetMapping("/download")
//    public void download(String name, HttpServletResponse response){
//
//        try ( //输入流读取文件内容
//              FileInputStream fileInputStream=new FileInputStream(new File(basePath+name));
//              //输出流将文件写回浏览器，在浏览器展示
//              ServletOutputStream outputStream = response.getOutputStream())
//        {
//
//
//
//            response.setContentType("image/jpeg");
////            IOUtils.copy(fileInputStream,outputStream);
//
//            int len=0;
//            byte[] bytes = new byte[1024];
//            while ((len=fileInputStream.read(bytes))!=-1){
//                outputStream.write(bytes,0,len);
//                outputStream.flush();
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }
}
