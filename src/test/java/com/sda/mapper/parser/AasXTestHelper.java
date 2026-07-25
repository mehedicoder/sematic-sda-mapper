package com.sda.mapper.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Helper for creating test AASX ZIP fixtures programmatically.
 */
final class AasXTestHelper {

    private AasXTestHelper() {
    }

    static InputStream createAasxWithDataJson(String dataJson) throws IOException {
        return createAasx(new String[]{"data.json"}, new String[]{dataJson});
    }

    static InputStream createAasxWithDataJsonInSubdir(String dataJson) throws IOException {
        return createAasx(new String[]{"aasx/data.json"}, new String[]{dataJson});
    }

    static InputStream createAasxWithEntries(String[] entryNames, String[] contents) throws IOException {
        return createAasx(entryNames, contents);
    }

    static InputStream createEmptyAasx() throws IOException {
        return createAasx(new String[0], new String[0]);
    }

    static InputStream createAasxWithoutDataJson() throws IOException {
        return createAasx(
            new String[]{"aasx-origin", "_rels/aasx-origin.rels", "[Content_Types].xml"},
            new String[]{"", "<Relationships/>", "<Types/>"}
        );
    }

    private static InputStream createAasx(String[] entryNames, String[] contents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (int i = 0; i < entryNames.length; i++) {
                zip.putNextEntry(new ZipEntry(entryNames[i]));
                if (i < contents.length && contents[i] != null) {
                    zip.write(contents[i].getBytes(StandardCharsets.UTF_8));
                }
                zip.closeEntry();
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    static String minimalAasJson() {
        return """
            {
              "assetAdministrationShells": [
                {
                  "idShort": "TestPump",
                  "id": { "idType": "IRI", "identifier": "https://example.org/aas/test-pump" },
                  "administration": { "version": "1.0", "revision": "0" },
                  "assetInformation": {
                    "globalAssetId": "https://example.org/asset/test-pump",
                    "defaultThumbnail": { "path": "images/pump.png" }
                  },
                  "submodels": [
                    {
                      "idShort": "Communication",
                      "properties": [
                        { "idShort": "Protocol", "value": "OPC-UA" },
                        { "idShort": "EndpointUrl", "value": "opc.tcp://localhost:4840" },
                        { "idShort": "AccessMode", "value": "read-write" },
                        { "idShort": "SecurityMode", "value": "SignAndEncrypt" }
                      ]
                    },
                    {
                      "idShort": "Skills",
                      "submodelElements": [
                        {
                          "idShort": "Pumping",
                          "properties": [
                            { "idShort": "EngineeringUnit", "value": "l/min" },
                            { "idShort": "NominalRangeMin", "value": "0.0" },
                            { "idShort": "NominalRangeMax", "value": "100.0" }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ],
              "submodels": [
                {
                  "idShort": "ProcessValues",
                  "submodelElements": [
                    { "idShort": "FlowRate" },
                    { "idShort": "Pressure" }
                  ]
                }
              ]
            }
            """;
    }

    static String multiShellAasJson() {
        return """
            {
              "assetAdministrationShells": [
                {
                  "idShort": "PumpAAS",
                  "id": { "idType": "IRI", "identifier": "https://example.org/aas/pump" },
                  "administration": { "version": "1.0", "revision": "0" },
                  "assetInformation": {
                    "globalAssetId": "https://example.org/asset/pump"
                  },
                  "submodels": [
                    {
                      "idShort": "Communication",
                      "properties": [
                        { "idShort": "Protocol", "value": "OPC-UA" }
                      ]
                    },
                    {
                      "idShort": "Skills",
                      "submodelElements": [
                        { "idShort": "Pumping", "properties": [] }
                      ]
                    }
                  ]
                },
                {
                  "idShort": "ValveAAS",
                  "id": { "idType": "IRI", "identifier": "https://example.org/aas/valve" },
                  "administration": { "version": "2.0", "revision": "1" },
                  "assetInformation": {
                    "globalAssetId": "https://example.org/asset/valve"
                  },
                  "submodels": [
                    {
                      "idShort": "Communication",
                      "properties": [
                        { "idShort": "Protocol", "value": "PROFINET" }
                      ]
                    },
                    {
                      "idShort": "Skills",
                      "submodelElements": [
                        { "idShort": "ValveControl", "properties": [] },
                        { "idShort": "FlowRegulation", "properties": [] }
                      ]
                    }
                  ]
                }
              ],
              "submodels": []
            }
            """;
    }
}
