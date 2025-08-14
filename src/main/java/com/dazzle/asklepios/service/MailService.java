package com.dazzle.asklepios.service;

import com.dazzle.asklepios.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Service for sending emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    @Value("${asklepios.mail.password}")
    private static final String BASE_URL = "baseUrl";


    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    public MailService(
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine
    ) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        Mono.defer(() -> {
            sendEmailSync(to, subject, content, isMultipart, isHtml);
            return Mono.empty();
        }).subscribe();
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            //TODO: add this to to yml file
            message.setFrom(new InternetAddress("systemask73@gmail.com"));
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        Mono.defer(() -> {
            sendEmailFromTemplateSync(user, templateName, titleKey);
            return Mono.empty();
        }).subscribe();
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        //TODO: move this to yml
        context.setVariable(BASE_URL, "http://localhost:3100");
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/passwordResetEmail", "email.reset.title");
    }

    public void sendNewUserPasswordMail(User user, String plainPassword) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }

        LOG.debug("Sending new user password email to '{}'", user.getEmail());

        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);


        context.setVariable(USER, user);
        context.setVariable("password", plainPassword);
        context.setVariable(BASE_URL, "http://localhost:3100");


        String content = templateEngine.process("mail/newUserPasswordEmail", context);
        String subject = messageSource.getMessage("email.newuser.title", null, locale);

        sendEmailSync(user.getEmail(), subject, content, false, true);
    }

}
