package com.dazzle.asklepios.service;

import com.dazzle.asklepios.domain.User;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";
    private static final String DEFAULT_LOGO = "templates/mail/images/logo.png";
    private static final int MAX_LOGO_BYTES = 2_000_000;
    private static final Duration LOGO_TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient LOGO_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    @Value("${asklepios.mail.base-url}")
    private String baseUrl;

    @Value("${asklepios.mail.username}")
    private String fromEmail;

    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;
    private final SystemConfigurationService systemConfigurationService;

    public MailService(
        JavaMailSender javaMailSender,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine,
        SystemConfigurationService systemConfigurationService
    ) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
        this.systemConfigurationService = systemConfigurationService;
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
        context.setVariable("userName", user.getLogin() != null ? user.getLogin() : "user");

        context.setVariable("userNameLabel",
            messageSource.getMessage(
                "email.reset.userName",
                null,
                locale
            )
        );
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
        context.setVariable("userName", user.getLogin() != null ? user.getLogin() : "user");

        context.setVariable("userNameLabel",
            messageSource.getMessage(
                "email.setpassword.userName",
                null,
                locale
            )
        );
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
        deliverEmailWithInlineLogo(to, subject, htmlContent);
    }

    private void deliverEmailWithInlineLogo(String to, String subject, String htmlContent) {
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
            InlineLogo logo = resolveInlineLogo();
            message.addInline("logo", logo.resource(), logo.contentType());

            javaMailSender.send(mimeMessage);
            LOG.debug("Sent styled email with logo to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }
    /**
     * New: One-time secure set-password link email for patient (subject + body includes link).
     *
     * @param language ,  id , fullName, document id, email  for target patient
     * @param oneTimeToken single-use token generated/stored/validated elsewhere
     */
    public void sendOneTimeSetPasswordLinkMailForPatient(String language, Long id, String name, String documnetId, String email, String oneTimeToken) {
        if (email == null) return;

        Locale locale = Locale.forLanguageTag(language);
        Context context = new Context(locale);

        String encodedToken = URLEncoder.encode(oneTimeToken, StandardCharsets.UTF_8);
        String setPasswordUrl = baseUrl + "/#/create-patient-password?key=" + encodedToken;
        context.setVariable("setPasswordUrl", setPasswordUrl);

        context.setVariable("title", messageSource.getMessage("email.setpassword.title", null, locale));
        context.setVariable("greeting", messageSource.getMessage(
            "email.setpassword.greeting",
            new Object[]{ name != null ? name: documnetId },
            locale
        ));
        context.setVariable("text1", messageSource.getMessage("email.setpassword.text1", null, locale));
        context.setVariable("text2", messageSource.getMessage("email.setpassword.text2", null, locale));
        context.setVariable("text3", messageSource.getMessage("email.setpassword.text3", null, locale));
        context.setVariable("text4", messageSource.getMessage("email.setpassword.text4", null, locale));
        context.setVariable("signature", messageSource.getMessage("email.signature", null, locale));

        // Optional RTL support
        boolean rtl = "ar".equalsIgnoreCase(language);
        context.setVariable("dir", rtl ? "rtl" : "ltr");

        String content = templateEngine.process("mail/newPatientPasswordEmail", context);
        String subject = messageSource.getMessage("email.setpassword.title", null, locale);

        sendEmailWithInlineLogoSync(email, subject, content);
    }

    /**
     * Uses the theme SYSTEM_LOGO configured in setup. Falls back to the packaged logo
     * when none is configured or the image cannot be downloaded.
     */
    private InlineLogo resolveInlineLogo() {
        try {
            return loadInlineLogo();
        } catch (Exception ex) {
            LOG.warn("Configured system logo could not be loaded for email. Using the default logo.", ex);
            return defaultLogo();
        }
    }

    private InlineLogo loadInlineLogo() {
        // Drop the caller's reactive transaction. Reset password used to read the logo
        // on that connection and fall back to the packaged image when the read could not finish.
        String logoUrl = systemConfigurationService
            .getResolvedSystemLogo()
            .contextWrite(context -> reactor.util.context.Context.empty())
            .block(LOGO_TIMEOUT);
        if (logoUrl == null || logoUrl.isBlank()) {
            LOG.debug("No system logo configured. Using the default email logo.");
            return defaultLogo();
        }
        if (!logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")) {
            LOG.warn("System logo [{}] is not a public URL. Using the default email logo.", logoUrl);
            return defaultLogo();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(logoUrl))
                .timeout(LOGO_TIMEOUT)
                .GET()
                .build();
            HttpResponse<byte[]> response = LOGO_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            String contentType = contentType(logoUrl, response.headers().firstValue("content-type").orElse(null));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || body == null || body.length == 0) {
                LOG.warn("System logo download failed. status={}, url={}", response.statusCode(), logoUrl);
                return defaultLogo();
            }
            if (body.length > MAX_LOGO_BYTES || contentType == null) {
                LOG.warn("System logo is not a usable image. contentType={}, bytes={}, url={}", contentType, body.length, logoUrl);
                return defaultLogo();
            }
            if (isSvg(body, contentType, logoUrl)) {
                byte[] png = rasterizeSvg(body);
                if (png != null && png.length > 0) {
                    LOG.info("Using configured SVG system logo as PNG for email: {}", logoUrl);
                    return new InlineLogo(new ByteArrayResource(png), "image/png");
                }
                LOG.warn("SVG system logo could not be converted to PNG. Attaching the original file. url={}", logoUrl);
            }
            LOG.info("Using configured system logo for email: {}", logoUrl);
            return new InlineLogo(new ByteArrayResource(body), contentType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("System logo download was interrupted for [{}]. Using the default email logo.", logoUrl);
            return defaultLogo();
        } catch (Exception ex) {
            LOG.warn("System logo download failed for [{}]. Using the default email logo.", logoUrl, ex);
            return defaultLogo();
        }
    }

    private static InlineLogo defaultLogo() {
        return new InlineLogo(new ClassPathResource(DEFAULT_LOGO), "image/png");
    }

    private static boolean isSvg(byte[] body, String contentType, String logoUrl) {
        if (contentType != null && contentType.contains("svg")) {
            return true;
        }
        String path = logoUrl == null ? "" : logoUrl.toLowerCase(Locale.ROOT);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.endsWith(".svg")) {
            return true;
        }
        String start = new String(body, 0, Math.min(body.length, 300), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return start.contains("<svg");
    }

    /**
     * Mail clients do not render SVG. Draw the configured logo onto a PNG instead.
     */
    private static byte[] rasterizeSvg(byte[] svg) {
        try {
            SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(svg));
            if (document == null) {
                return null;
            }
            double width = document.size().getWidth();
            double height = document.size().getHeight();
            if (width <= 1 || height <= 1) {
                width = 480;
                height = 160;
            }
            double scale = width > 480 ? 480d / width : 1d;
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));

            BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.scale(scale, scale);
            document.render(null, graphics);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                return null;
            }
            return output.toByteArray();
        } catch (Exception ex) {
            LOG.warn("Failed to rasterize the SVG system logo", ex);
            return null;
        }
    }

    private static String contentType(String logoUrl, String header) {
        if (header != null) {
            String mime = header.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (mime.startsWith("image/")) {
                return mime;
            }
            if (!mime.isBlank() && !"application/octet-stream".equals(mime)) {
                return null;
            }
        }
        String path = logoUrl;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return header == null ? "image/png" : null;
    }

    private record InlineLogo(Resource resource, String contentType) {}

}
