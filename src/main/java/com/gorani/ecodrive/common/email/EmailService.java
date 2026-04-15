package com.gorani.ecodrive.common.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendContractEmail(String to, String contractorName, byte[] pdfBytes) {
        if (to == null || to.isBlank()) {
            log.warn("이메일 주소가 없어 발송을 건너뜁니다.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[EcoDrive] 보험 계약서가 발급되었습니다.");
            helper.setText(buildEmailBody(contractorName), true); // true = HTML
            helper.addAttachment("EcoDrive_계약서.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("계약서 이메일 발송 완료: {}", to);

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);
        }
    }

    private String buildEmailBody(String contractorName) {
        return """
                <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #1e293b; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 22px;">EcoDrive</h1>
                    </div>
                    <div style="background: #f8fafc; padding: 32px; border-radius: 0 0 12px 12px; border: 1px solid #e2e8f0;">
                        <p style="font-size: 16px; color: #1e293b;">안녕하세요, <strong>%s</strong>님!</p>
                        <p style="color: #475569;">EcoDrive 자동차보험에 가입해 주셔서 감사합니다.</p>
                        <p style="color: #475569;">첨부된 PDF 파일에서 계약서를 확인하실 수 있습니다.</p>
                        <div style="background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 24px 0;">
                            <p style="margin: 0; color: #64748b; font-size: 13px;">
                                📎 첨부파일: EcoDrive_계약서.pdf
                            </p>
                        </div>
                        <p style="color: #94a3b8; font-size: 12px;">
                            본 메일은 자동 발송된 메일입니다.
                        </p>
                    </div>
                </div>
                """.formatted(contractorName);
    }
}
