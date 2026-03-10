package cn.linkfast;

import java.sql.Connection;
import java.sql.DriverManager;

public class SimpleTest {
    public static void main(String[] args) {
        // 请修改为你实际的数据库地址、账号和密码
      /*  String url = "jdbc:mysql://112.124.51.99:3306/link_fast?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String user = "root";
        String password = "LinkFast888!";*/

        String url="jdbc:mysql://112.124.51.99:3306/link_fast?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
        String username="root";
        String password="LinkFast888!";

        try {
            System.out.println("正在尝试连接数据库...");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("连接成功！连接对象: " + conn);
            conn.close();
        } catch (Exception e) {
            System.err.println("连接失败！错误详情如下：");
            e.printStackTrace();
        }
    }
}