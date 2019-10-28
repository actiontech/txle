/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.dao;

import com.actionsky.txle.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserDao {
//    /**
//     * 通过名字查询用户信息
//     */
//    @Select("SELECT * FROM user WHERE name = #{name}")
//    User findUserByName(@Param("name") String name);
//
//    @Select("SELECT * FROM txle_sample_user")
//    List<UserEntity> findAllUser();

    @Select("SELECT * FROM txle_sample_user T WHERE T.id = #{userId}")
    UserEntity findOne(long userId);
//
//    /**
//     * 插入用户信息
//     */
//    @Insert("INSERT INTO user(name, age,money) VALUES(#{name}, #{age}, #{money})")
//    void insertUser(@Param("name") String name, @Param("age") Integer age, @Param("money") Double money);
//
//    /**
//     * 根据 id 更新用户信息
//     */
//    @Update("UPDATE  user SET name = #{name},age = #{age},money= #{money} WHERE id = #{id}")
//    void updateUser(@Param("name") String name, @Param("age") Integer age, @Param("money") Double money,
//                    @Param("id") int id);
//
//    /**
//     * 根据 id 删除用户信息
//     */
//    @Delete("DELETE from user WHERE id = #{id}")
//    void deleteUser(@Param("id") int id);

    @Update("UPDATE txle_sample_user T SET T.balance = T.balance - #{balance} WHERE id = #{userId}")
    int updateBalanceByUserId(@Param("userId") long userId, @Param("balance") double balance);
}