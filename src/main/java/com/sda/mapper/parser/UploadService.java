package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Service that orchestrates file upload parsing via {@link ParserFactory}.
 * Wraps the input stream in a {@link BufferedInputStream} to guarantee mark/reset
 * support for content sniffing.
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final ParserFactory parserFactory;

    public UploadService(ParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    /**
     * Parses an uploaded file using the appropriate parser selected by filename extension
     * and content sniffing.
     *
     * @param filename the original filename
     * @param input    the file content stream
     * @return the parse result with triple count and details
     * @throws ParserException if the file type is unsupported or parsing fails
     */
    public ParseResult parseUpload(String filename, InputStream input) {
        log.info("Parsing upload: filename={}", filename);
        long start = System.currentTimeMillis();

        InputStream buffered = new BufferedInputStream(input);
        Parser parser = parserFactory.getParser(filename, buffered);
        ParseResult result = parser.parse(buffered);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Parse completed in {}ms: {} triples, type={}",
            elapsed, result.tripleCount(), result.fileType());

        return result;
    }
}
