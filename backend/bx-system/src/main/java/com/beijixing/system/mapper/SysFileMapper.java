package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统文件 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 根据文件Key查询
     *
     * @param fileKey 文件Key
     * @return SysFile
     */
    SysFile selectByFileKey(@Param("fileKey") String fileKey);

    /**
     * 根据用户ID查询文件列表
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    List<SysFile> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据标签查询文件列表
     *
     * @param tag 标签
     * @param userId 用户ID
     * @return 文件列表
     */
    List<SysFile> selectByTag(@Param("tag") String tag, @Param("userId") Long userId);
}
