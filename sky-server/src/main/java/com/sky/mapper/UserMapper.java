package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 插入用户信息
     * @param user
     */
    void insert(User user);

    /**
     * 根据openId查询用户信息
     * @param openId
     */
    @Select("SELECT * FROM user WHERE openid = #{openId}")
    User selectByOpenId(String openId);

    /**
     * 根据id查询用户信息
     * @param id
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);

    /**
     *根据状态和截止时间获取当日新增用户数量
     * @param map
     * @return
     */
    Integer getNewUserByDate(Map<String, Object> map);
}
