package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parser Factory")
class ParserFactoryTest {

    private final ParserFactory factory = new ParserFactory(
        new MTPAmlParser(), new UaServerDatasheetParser());

    @Test
    @DisplayName("Should return MTPAmlParser for .xml extension")
    void shouldReturnMtpAmlParserForXml() {
        Parser parser = factory.getParser("test-file.xml");
        assertInstanceOf(MTPAmlParser.class, parser);
    }

    @Test
    @DisplayName("Should return MTPAmlParser for .aml extension")
    void shouldReturnMtpAmlParserForAml() {
        Parser parser = factory.getParser("test-file.aml");
        assertInstanceOf(MTPAmlParser.class, parser);
    }

    @Test
    @DisplayName("Should return UaServerDatasheetParser for .json extension")
    void shouldReturnUaServerDatasheetParserForJson() {
        Parser parser = factory.getParser("test-file.json");
        assertInstanceOf(UaServerDatasheetParser.class, parser);
    }

    @Test
    @DisplayName("Should throw ParserException for unknown extension")
    void shouldThrowForUnknownExtension() {
        assertThrows(ParserException.class, () -> factory.getParser("test-file.txt"));
        assertThrows(ParserException.class, () -> factory.getParser("test-file.csv"));
    }
}
