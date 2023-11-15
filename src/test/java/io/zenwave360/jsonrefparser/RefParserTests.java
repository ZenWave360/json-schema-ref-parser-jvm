package io.zenwave360.jsonrefparser;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Test that files are not locked after parsing.
 */
public class RefParserTests {


	private static final Logger log = LoggerFactory.getLogger(RefParserTests.class.getName());

	@Test
	public void testParserWithUri() throws Exception {
		Files.copy(Path.of("src/test/resources/indexed-array.yml"), Path.of("target/indexed-array.yml"),	StandardCopyOption.REPLACE_EXISTING);
		Map<String, Object> parsed = new $RefParser(Path.of("target/indexed-array.yml").toUri()).parse().getRefs().schema();
		Files.delete(Path.of("target/indexed-array.yml"));
	}

	@Test
	public void testParserWithFile() throws Exception {
		Files.copy(Path.of("src/test/resources/indexed-array.yml"), Path.of("target/indexed-array.yml"),	StandardCopyOption.REPLACE_EXISTING);
		Map<String, Object> parsed = new $RefParser(new File("target/indexed-array.yml")).parse().getRefs().schema();
		Files.delete(Path.of("target/indexed-array.yml"));
	}

}
