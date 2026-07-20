package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import com.sda.mapper.ui.catalog.DiscoveredServiceEntry;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Discovers services from a live OPC UA server by browsing its address space.
 * Maps OPC UA variable nodes to {@link DiscoveredServiceEntry} catalog rows.
 */
@Service
public class OpcUaDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(OpcUaDiscoveryService.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int BROWSE_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 2000;
    private static final int MAX_DEPTH = 3;

    private final OpcUaClientFactory clientFactory;
    private final OpcUaServerRegistry serverRegistry;

    public OpcUaDiscoveryService(OpcUaClientFactory clientFactory,
                                  OpcUaServerRegistry serverRegistry) {
        this.clientFactory = clientFactory;
        this.serverRegistry = serverRegistry;
    }

    /**
     * Discovers all variable nodes from an OPC UA server and returns them as catalog entries.
     *
     * @param endpointUrl the OPC UA endpoint URL (e.g. "opc.tcp://localhost:4840")
     * @return list of discovered service entries
     */
    public List<DiscoveredServiceEntry> discover(String endpointUrl) {
        OpcUaEndpointConfig config = resolveConfig(endpointUrl);
        if (config == null) {
            log.warn("Cannot parse endpoint URL: {}", endpointUrl);
            return List.of();
        }

        OpcUaClientWrapper client = clientFactory.createClient(config);
        try {
            client.connect(config).get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Cannot connect to OPC UA server {}: {}", endpointUrl, e.getMessage());
            return List.of();
        }

        String serviceName = resolveServerName(endpointUrl);
        List<DiscoveredServiceEntry> entries = new ArrayList<>();

        try {
            browseRecursive(client, Identifiers.ObjectsFolder, entries, serviceName, endpointUrl, 0);
        } catch (Exception e) {
            log.warn("Browse failed for {}: {}", endpointUrl, e.getMessage());
        }

        log.info("Discovered {} variable nodes from {}", entries.size(), endpointUrl);
        return entries;
    }

    private void browseRecursive(OpcUaClientWrapper client, NodeId nodeId,
                                  List<DiscoveredServiceEntry> entries,
                                  String serviceName, String endpointUrl, int depth) {
        if (depth >= MAX_DEPTH) return;

        List<ReferenceDescription> refs;
        try {
            refs = client.browse(nodeId).get(BROWSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Browse failed for node {}: {}", nodeId, e.getMessage());
            return;
        }

        var nsTable = client.getNamespaceTable();

        for (ReferenceDescription ref : refs) {
            NodeId childNodeId = ref.getNodeId().toNodeId(nsTable).orElse(null);
            if (childNodeId == null) continue;

            String browseName = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            String displayName = ref.getDisplayName() != null ? ref.getDisplayName().getText() : browseName;
            String nodeIdStr = String.format("ns=%d;%s",
                childNodeId.getNamespaceIndex().intValue(), childNodeId.getIdentifier());
            String nodeClass = ref.getNodeClass() != null ? ref.getNodeClass().name() : "Unknown";

            if ("Variable".equals(nodeClass)) {
                String accessMode = "read-only";
                String currentValue = "";
                String dataType = "";

                try {
                    OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofString(
                        childNodeId.getNamespaceIndex().intValue(),
                        String.valueOf(childNodeId.getIdentifier()));
                    Object rawValue = client.readVariableValue(nodeConfig).get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (rawValue != null) {
                        currentValue = String.valueOf(rawValue);
                        dataType = rawValue.getClass().getSimpleName();
                    }
                } catch (Exception e) {
                    log.debug("Failed to read {}: {}", nodeIdStr, e.getMessage());
                }

                entries.add(new DiscoveredServiceEntry(
                    serviceName,
                    findParentFolderName(refs, ref, client, nsTable),
                    displayName,
                    "OPC-UA",
                    accessMode,
                    currentValue,
                    dataType,
                    nodeIdStr,
                    endpointUrl,
                    "OPC-UA Discovery"
                ));
            } else {
                browseRecursive(client, childNodeId, entries, serviceName, endpointUrl, depth + 1);
            }
        }
    }

    private String findParentFolderName(List<ReferenceDescription> allRefs,
                                         ReferenceDescription variableRef,
                                         OpcUaClientWrapper client,
                                         org.eclipse.milo.opcua.stack.core.NamespaceTable nsTable) {
        return "Objects";
    }

    private String resolveServerName(String endpointUrl) {
        OpcUaServerRegistry.ServerInfo info = serverRegistry.getServer(endpointUrl);
        if (info != null) {
            return info.name();
        }
        try {
            URI uri = new URI(endpointUrl.replace("opc.tcp://", "http://"));
            return "OPC UA Server " + uri.getHost() + ":" + getPortFromUri(uri);
        } catch (Exception e) {
            return "OPC UA Server";
        }
    }

    private int getPortFromUri(URI uri) {
        int port = uri.getPort();
        return port > 0 ? port : 4840;
    }

    private OpcUaEndpointConfig resolveConfig(String endpointUrl) {
        OpcUaServerRegistry.ServerInfo info = serverRegistry.getServer(endpointUrl);
        if (info != null) {
            return OpcUaEndpointConfig.local(info.host(), info.port(), info.path());
        }
        return parseEndpointUrl(endpointUrl);
    }

    private OpcUaEndpointConfig parseEndpointUrl(String url) {
        try {
            String withoutProtocol = url.replace("opc.tcp://", "");
            String[] hostPort = withoutProtocol.split(":", 2);
            String host = hostPort[0];
            String portAndPath = hostPort.length > 1 ? hostPort[1] : "4840/";
            int slashIdx = portAndPath.indexOf('/');
            int port = slashIdx > 0 ? Integer.parseInt(portAndPath.substring(0, slashIdx)) : Integer.parseInt(portAndPath);
            String path = slashIdx > 0 ? portAndPath.substring(slashIdx) : "/";
            return OpcUaEndpointConfig.local(host, port, path);
        } catch (Exception e) {
            log.debug("Failed to parse endpoint URL: {}", url);
            return null;
        }
    }
}
