package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
//        //1.获取session
//        HttpSession session = request.getSession();
//        //2.获取session中的用户
//        UserDTO user = (UserDTO) session.getAttribute("user");
        //3.判断用户是否存在
        if(UserHolder.getUser() == null){
            //4.不存在，拦截，返回401状态码,401 状态码表示“未授权”，即请求缺少有效的身份验证凭证
            response.setStatus(401);
            return false;
        }
        //5.存在，保存用户信息到ThreadLocal
//        UserHolder.saveUser(user);
        //6.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex){
        UserHolder.removeUser();
    }
}
