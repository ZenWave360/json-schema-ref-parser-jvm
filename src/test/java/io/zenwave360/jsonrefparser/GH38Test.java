package io.zenwave360.jsonrefparser;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Tests for GH-38: mergeResolvedAndReplaceOriginal resolved relative $ref paths against
 * the root spec URI instead of the file that contains the $ref. This causes crashes when
 * '../' overshoots the root's directory depth, and silent wrong resolution otherwise.
 */
public class GH38Test {

    /**
     * Dereference a multi-file spec where an external file has a $ref with sibling
     * keywords (description). The '../../../' in the $ref is valid relative to the
     * external file (models/events/publish/) but not relative to the root spec.
     *
     * Verifies resolved content, preserved siblings, and correct URI in replacedRefsList.
     */
    @Test
    public void testRefWithSiblingKeywordsResolvesAgainstCorrectFileUri() throws Exception {
        $RefParser refParser = new $RefParser(new File("src/test/resources/GH-38/root.yaml"));
        $Refs refs = refParser.parse().dereference().getRefs();

        Map<String, Object> order = (Map<String, Object>) ((Map) refs.schema().get("properties")).get("order");
        Map<String, Object> orderProps = (Map<String, Object>) order.get("properties");
        Map<String, Object> deliveryAddress = (Map<String, Object>) orderProps.get("deliveryAddress");

        Assert.assertNotNull("deliveryAddress should exist", deliveryAddress);
        Assert.assertFalse("$ref should be removed after dereferencing", deliveryAddress.containsKey("$ref"));
        Assert.assertEquals("Sibling keyword should be preserved",
                "Delivery address for the order", deliveryAddress.get("description"));

        Map<String, Object> addressProps = (Map<String, Object>) deliveryAddress.get("properties");
        Assert.assertNotNull("Resolved Address properties should be present", addressProps);
        Assert.assertTrue(addressProps.containsKey("street"));
        Assert.assertTrue(addressProps.containsKey("city"));
        Assert.assertTrue(addressProps.containsKey("zipCode"));

        // The key assertion: the replaced ref must be resolved against OrderCreated.yaml's
        // URI (the file containing the $ref), not the root spec URI.
        URI expectedUri = new File("src/test/resources/GH-38/shared/Address.yml").toURI();
        List<Pair<$Ref, Object>> replacedRefs = refs.getReplacedRefsList();

        boolean resolvedCorrectly = replacedRefs.stream()
                .filter(pair -> pair.getKey().getRef().equals("../../../shared/Address.yml"))
                .anyMatch(pair -> pair.getKey().getURI().equals(expectedUri));

        Assert.assertTrue("Replaced ref should resolve to " + expectedUri
                + ", actual: " + replacedRefs, resolvedCorrectly);
    }
}
