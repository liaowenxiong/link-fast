package cn.linkfast.controller;

import cn.linkfast.common.Result;
import cn.linkfast.entity.User;
import cn.linkfast.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *处理用户相关的HTTP请求
 */
@Controller
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    @ResponseBody
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        return Result.success(users);
    }

    /**
     *根据ID获取用户
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error(404, "用户不存在");
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    @ResponseBody
    public Result<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return Result.success(createdUser);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @ResponseBody
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            return Result.success(updatedUser);
        } else {
            return Result.error(404, "用户不存在");
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<String> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return Result.success("用户删除成功");
        } else {
            return Result.error(404, "用户不存在");
        }
    }
}