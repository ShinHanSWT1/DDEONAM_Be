package com.gorani.ecodrive.common.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class PdfService {

    @Value("${pdf.font-path}")
    private String fontPath;

    public byte[] createContractPdf(
            InsuranceContract contract,
            List<InsuranceCoverage> coverages,
            String signatureImageBase64,
            String contractorName
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // 한글 폰트 로드
        BaseFont bf;
        try {
            bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            log.warn("한글 폰트 로드 실패, 기본 폰트 사용: {}", e.getMessage());
            bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        }

        Font titleFont  = new Font(bf, 20, Font.BOLD, new Color(30, 41, 59));
        Font headerFont = new Font(bf, 13, Font.BOLD, new Color(30, 41, 59));
        Font labelFont  = new Font(bf, 10, Font.BOLD, new Color(100, 116, 139));
        Font valueFont  = new Font(bf, 10, Font.NORMAL, new Color(30, 41, 59));
        Font smallFont  = new Font(bf, 9, Font.NORMAL, new Color(148, 163, 184));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        // ── 제목 ──────────────────────────────────────
        Paragraph title = new Paragraph("EcoDrive 자동차보험 계약서", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4f);
        document.add(title);

        Paragraph subtitle = new Paragraph("전자서명 계약서", smallFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20f);
        document.add(subtitle);

        addLine(document, writer);

        // ── 계약자 정보 ────────────────────────────────
        document.add(new Paragraph("■ 계약자 정보", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 2});
        infoTable.setSpacingAfter(16f);

        addTableRow(infoTable, "계약자명", contractorName, labelFont, valueFont);
        addTableRow(infoTable, "연락처", contract.getPhoneNumber(), labelFont, valueFont);
        addTableRow(infoTable, "주소", contract.getAddress(), labelFont, valueFont);
        addTableRow(infoTable, "계약번호", "ED-" + contract.getId(), labelFont, valueFont);
        addTableRow(infoTable, "계약일", contract.getCreatedAt().format(dtf), labelFont, valueFont);
        document.add(infoTable);

        // ── 상품 정보 ────────────────────────────────
        document.add(new Paragraph("■ 보험 상품 정보", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable productTable = new PdfPTable(2);
        productTable.setWidthPercentage(100);
        productTable.setWidths(new float[]{1, 2});
        productTable.setSpacingAfter(16f);

        String companyName = contract.getInsuranceProduct().getInsuranceCompany().getCompanyName();
        String productName = contract.getInsuranceProduct().getProductName();
        String planType    = contract.getPlanType() != null ? contract.getPlanType().name() : "-";

        addTableRow(productTable, "보험사", companyName, labelFont, valueFont);
        addTableRow(productTable, "상품명", productName, labelFont, valueFont);
        addTableRow(productTable, "플랜",
                planType.equals("BASIC") ? "기본형" : planType.equals("STANDARD") ? "표준형" : "프리미엄형",
                labelFont, valueFont);
        addTableRow(productTable, "기본보험료", String.format("%,d원", contract.getBaseAmount()), labelFont, valueFont);
        addTableRow(productTable, "안전점수 할인", String.format("-%,d원", contract.getDiscountAmount()), labelFont, valueFont);
        addTableRow(productTable, "최종보험료", String.format("%,d원", contract.getFinalAmount()), labelFont, valueFont);
        document.add(productTable);

        // ── 선택 특약 ────────────────────────────────
        document.add(new Paragraph("■ 선택 특약", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable coverageTable = new PdfPTable(3);
        coverageTable.setWidthPercentage(100);
        coverageTable.setWidths(new float[]{2, 3, 2});
        coverageTable.setSpacingAfter(20f);

        // 헤더
        addCoverageHeader(coverageTable, "카테고리", labelFont);
        addCoverageHeader(coverageTable, "특약명", labelFont);
        addCoverageHeader(coverageTable, "보장금액", labelFont);

        for (InsuranceCoverage cov : coverages) {
            long amount = cov.getCoverageAmount();
            String amountStr = amount >= 100_000_000
                    ? String.format("%,d억원", amount / 100_000_000)
                    : String.format("%,d만원", amount / 10_000);

            addCoverageCell(coverageTable, cov.getCategory(), valueFont);
            addCoverageCell(coverageTable, cov.getCoverageName(), valueFont);
            addCoverageCell(coverageTable, amountStr, valueFont);
        }
        document.add(coverageTable);

        // ── 전자서명 ────────────────────────────────
        document.add(new Paragraph("■ 전자서명", headerFont));
        document.add(Chunk.NEWLINE);

        if (signatureImageBase64 != null && !signatureImageBase64.isBlank()) {
            try {
                String base64Data = signatureImageBase64.contains(",")
                        ? signatureImageBase64.split(",")[1]
                        : signatureImageBase64;
                byte[] imgBytes = Base64.getDecoder().decode(base64Data);
                Image sigImage = Image.getInstance(imgBytes);
                sigImage.scaleToFit(300, 100);
                sigImage.setAlignment(Element.ALIGN_LEFT);
                document.add(sigImage);
            } catch (Exception e) {
                log.warn("서명 이미지 삽입 실패: {}", e.getMessage());
            }
        }

        Paragraph sigInfo = new Paragraph(
                "서명일시: " + java.time.LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")), smallFont);
        sigInfo.setSpacingBefore(4f);
        document.add(sigInfo);

        addLine(document, writer);

        Paragraph footer = new Paragraph(
                "본 계약서는 EcoDrive 플랫폼을 통해 전자적으로 체결된 계약서입니다.", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    private void addLine(Document document, PdfWriter writer) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorStroke(new Color(226, 232, 240));
        cb.setLineWidth(0.5f);
        cb.moveTo(50, writer.getVerticalPosition(false) - 8);
        cb.lineTo(545, writer.getVerticalPosition(false) - 8);
        cb.stroke();
        document.add(new Paragraph(" "));
    }

    private void addTableRow(PdfPTable table, String label, String value,
                              Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6f);
        labelCell.setBackgroundColor(new Color(248, 250, 252));

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addCoverageHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(6f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private void addCoverageCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }
}
