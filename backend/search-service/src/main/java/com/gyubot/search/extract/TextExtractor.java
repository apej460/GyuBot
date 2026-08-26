package com.gyubot.search.extract;

import com.gyubot.search.exception.UnsupportedFileFormatException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/*
 * document-service는 PDF·HWP 업로드를 둘 다 허용하지만(REQ-F-012), 자바 생태계에는 HWP를 안정적으로
 * 파싱하는 성숙한 라이브러리가 마땅치 않다. 그래서 지금은 PDF만 지원하고, HWP는 명확한 예외로
 * 걷어내 색인을 건너뛴다 (DocumentEventListener가 로그만 남기고 넘어감) — 나중에 HWP 파서를
 * 붙이게 되면 이 클래스에 분기만 추가하면 된다.
 */
@Component
public class TextExtractor {

    public String extract(String originalFilename, byte[] content) {
        String lower = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return extractPdf(content);
        }
        if (lower.endsWith(".hwp")) {
            throw new UnsupportedFileFormatException("HWP는 아직 텍스트 추출을 지원하지 않습니다: " + originalFilename);
        }
        throw new UnsupportedFileFormatException("지원하지 않는 파일 형식입니다: " + originalFilename);
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 텍스트 추출에 실패했습니다.", e);
        }
    }
}
