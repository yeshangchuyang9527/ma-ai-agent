package com.mahaoyang.maaiagent.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.mahaoyang.mapicturebackend.config.CosClientConfig;
import org.mahaoyang.mapicturebackend.exception.BusinessException;
import org.mahaoyang.mapicturebackend.exception.ErrorCode;
import org.mahaoyang.mapicturebackend.exception.ThrowUtils;
import org.mahaoyang.mapicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {

    // 单个文件大小限制
    final long ONE_M = 1024 * 1024;

    // 允许上传的文件后缀集合
    final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "webp");

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 文件上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFileName = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果并返回
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 计算宽高
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = UploadPictureResult.builder()
                    .url(cosClientConfig.getHost() + "/" + uploadPath)
                    .picName(FileUtil.mainName(originFileName))
                    .picSize(FileUtil.size(file))
                    .picWidth(picWidth)
                    .picHeight(picHeight)
                    .picScale(picScale)
                    .picFormat(imageInfo.getFormat())
                    .build();
            // 返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }
    }


    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1.校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 2.校验文件后缀(文件类型
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());

        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "不支持的文件类型");

    }

    /**
     * 临时文件清理
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }

    }
}

