package com.bbmovie.auth.service.student;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class OcrExtractionService {

	public Optional<String> extractText(MultipartFile file) {
		String filename = file.getOriginalFilename();
		if (filename == null) filename = "document";
		String lower = filename.toLowerCase();
		try {
			if (lower.endsWith(".pdf")) {
				return Optional.ofNullable(extractFromPdf(file));
			} else {
				return Optional.ofNullable(extractWithTesseract());
			}
		} catch (Exception ex) {
			log.warn("OCR extraction failed: {}", ex.getMessage());
			return Optional.empty();
		}
	}

	private String extractFromPdf(MultipartFile file) throws IOException {
		try (PDDocument doc = PDDocument.load(file.getInputStream())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(doc);
		}
	}

	private String extractWithTesseract()  {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}


