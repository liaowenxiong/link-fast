package cn.linkfast.service;

import cn.linkfast.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
    }

    @Test
    void getUserById() {
        // 准备测试数据
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("testuser");
        
        // 设置模拟行为
        when(userService.findUserById(userId)).thenReturn(mockUser);

        // 执行测试
        User result = userService.findUserById(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userService).findUserById(userId);
    }

    @Test
    void createUser() {
        // 准备测试数据
        User user = new User();
        user.setUsername("newuser");
        user.setAge(18);

        // 设置模拟行为
        when(userService.createUser(any(User.class))).thenReturn(user);

        // 执行测试
        User result = userService.createUser(user);

        // 验证结果
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userService).createUser(any(User.class));
    }
}