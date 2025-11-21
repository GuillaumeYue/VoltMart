package com.example.voltmart.utils;

import android.os.Looper;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * 邮件发送工具类
 * 使用JavaMail API通过Gmail SMTP服务器发送邮件
 * 用于发送订单确认邮件等
 */
public class EmailSender {

    // Gmail SMTP配置
    private static final String emailUsername = "YOUR_EMAIL";           // 发件人邮箱（需要配置）
    private static final String emailPassword = "YOUR_GENERATED_PASSWORD"; // 发件人邮箱密码或应用专用密码（需要配置）

    private String subject;         // 邮件主题
    private String messageBody;     // 邮件正文
    private String recipientEmail;  // 收件人邮箱

    /**
     * 构造函数
     * @param subject 邮件主题
     * @param messageBody 邮件正文
     * @param email 收件人邮箱
     */
    public EmailSender(String subject, String messageBody, String email) {
        this.subject = subject;
        this.messageBody = messageBody;
        this.recipientEmail = email;
    }

    /**
     * 发送邮件
     * 在后台线程中异步发送邮件，避免阻塞UI线程
     */
    public void sendEmail() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare(); // 准备Looper用于线程消息处理

                // 配置SMTP服务器属性
                Properties properties = new Properties();
                properties.put("mail.smtp.host", "smtp.gmail.com");              // Gmail SMTP服务器
                properties.put("mail.smtp.socketFactory.port", "465");            // SSL端口
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL工厂类
                properties.put("mail.smtp.auth", "true");                        // 需要认证
                properties.put("mail.smtp.port", "465");                          // SMTP端口

                // 创建邮件会话，使用认证器
                Session session = Session.getInstance(properties,
                        new Authenticator() {
                            /**
                             * 获取密码认证
                             * @return 密码认证对象
                             */
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(emailUsername, emailPassword);
                            }
                        });

                try {
                    // 检查收件人邮箱是否有效
                    if (recipientEmail != null && !recipientEmail.isEmpty()) {
                        Log.i("Email", "Sending email to: " + recipientEmail);
                        // 创建邮件消息
                        MimeMessage message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(emailUsername)); // 设置发件人
                        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail)); // 设置收件人
                        message.setSubject(subject); // 设置主题
                        message.setText(messageBody); // 设置正文

                        Transport.send(message); // 发送邮件
                        Log.i("Email", "Email sent successfully");
                    } else {
                        // 收件人邮箱为空，跳过发送
                        Log.w("Email", "Recipient email is null or empty, skipping email send");
                    }
                }
                catch (MessagingException e) {
                    // 发送失败，记录错误日志
                    Log.e("Email", "Failed to send email: " + e.getMessage());
                    e.printStackTrace();
                }
                Looper.loop(); // 启动Looper循环
            }
        }).start(); // 启动后台线程
    }
}
