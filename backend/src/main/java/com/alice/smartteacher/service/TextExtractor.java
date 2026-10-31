package com.alice.smartteacher.service;

import com.alice.smartteacher.web.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class TextExtractor {

    public String read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            if (name.endsWith(".docx")) {
                try (InputStream in = file.getInputStream();
                     XWPFDocument doc = new XWPFDocument(in);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
            if (name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".md")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            throw new ApiException(400, "Use a PDF, DOCX, or TXT file.");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(400, "Could not read that file. Save it as PDF, DOCX, or TXT.");
        }
    }

    public String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = text.replace('\u0000', ' ').trim();
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, max);
    }
}
