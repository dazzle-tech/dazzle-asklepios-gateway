package com.dazzle.asklepios.service;

import com.dazzle.asklepios.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";

    @Value("${asklepios.mail.base-url}")
    private String baseUrl;

    @Value("${asklepios.mail.username}")
    private String fromEmail;

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
            isMultipart, isHtml, to, subject, content
        );

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(new InternetAddress(fromEmail));
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    public void sendStyledPasswordResetMail(User user) {
        if (user.getEmail() == null) return;

        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);

        context.setVariable(USER, user);

        String resetPasswordUrl = baseUrl + "/#/reset-password?key=" +
            URLEncoder.encode(user.getResetKey(), StandardCharsets.UTF_8);
        context.setVariable("resetPasswordUrl", resetPasswordUrl);

        context.setVariable("title", messageSource.getMessage("email.reset.title", null, locale));
        context.setVariable("greeting", messageSource.getMessage(
            "email.reset.greeting",
            new Object[]{ user.getFirstName() != null ? user.getFirstName() : user.getLogin() },
            locale
        ));
        context.setVariable("text1", messageSource.getMessage("email.reset.text1", null, locale));
        context.setVariable("text2", messageSource.getMessage("email.reset.text2", null, locale));
        context.setVariable("text3", messageSource.getMessage("email.reset.text3", null, locale));
        context.setVariable("text4", messageSource.getMessage("email.reset.text4", null, locale));
        context.setVariable("signature", messageSource.getMessage("email.signature", null, locale));

        boolean rtl = "ar".equalsIgnoreCase(user.getLangKey());
        context.setVariable("dir", rtl ? "rtl" : "ltr");

        String content = templateEngine.process("mail/passwordResetEmail", context);
        String subject = messageSource.getMessage("email.reset.title", null, locale);

        sendEmailWithInlineLogoSync(user.getEmail(), subject, content);
    }

    // Existing
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendStyledPasswordResetMail(user);
    }

    /**
     * New: One-time secure set-password link email (subject + body includes link).
     *
     * @param user target user
     * @param oneTimeToken single-use token generated/stored/validated elsewhere
     */
    public void sendOneTimeSetPasswordLinkMail(User user, String oneTimeToken) {
        if (user.getEmail() == null) return;

        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);

        context.setVariable(USER, user);

        String encodedToken = URLEncoder.encode(oneTimeToken, StandardCharsets.UTF_8);
        String setPasswordUrl = baseUrl + "/#/create-password?key=" + encodedToken;
        context.setVariable("setPasswordUrl", setPasswordUrl);

        context.setVariable("title", messageSource.getMessage("email.setpassword.title", null, locale));
        context.setVariable("greeting", messageSource.getMessage(
            "email.setpassword.greeting",
            new Object[]{ user.getFirstName() != null ? user.getFirstName() : user.getLogin() },
            locale
        ));
        context.setVariable("text1", messageSource.getMessage("email.setpassword.text1", null, locale));
        context.setVariable("text2", messageSource.getMessage("email.setpassword.text2", null, locale));
        context.setVariable("text3", messageSource.getMessage("email.setpassword.text3", null, locale));
        context.setVariable("text4", messageSource.getMessage("email.setpassword.text4", null, locale));
        context.setVariable("signature", messageSource.getMessage("email.signature", null, locale));

        // Optional RTL support
        boolean rtl = "ar".equalsIgnoreCase(user.getLangKey());
        context.setVariable("dir", rtl ? "rtl" : "ltr");

        String content = templateEngine.process("mail/newUserPasswordEmail", context);
        String subject = messageSource.getMessage("email.setpassword.title", null, locale);

        sendEmailWithInlineLogoSync(user.getEmail(), subject, content);
    }
    private void sendEmailWithInlineLogoSync(String to, String subject, String htmlContent) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(
                mimeMessage, true, StandardCharsets.UTF_8.name()
            );

            message.setTo(to);
            message.setFrom(new InternetAddress(fromEmail));
            message.setSubject(subject);
            message.setText(htmlContent, true);

            // Inline logo referenced as: <img src="cid:logo" ...>
            Resource logo = new ClassPathResource("templates/mail/images/logo_blue.svg");
            message.addInline("logo", logo);

            javaMailSender.send(mimeMessage);
            LOG.debug("Sent styled email with logo to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

}
