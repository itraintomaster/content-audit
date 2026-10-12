package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Per-unit evidence, copied from the returned verdict, never from the configured judge. */
public final class RecordedVerdictVersions {
    private RecordedVerdictVersions() {}
    public static void stamp(AuditNode node, String analyzer, String version) {
        if (node.getMetadata() == null) node.setMetadata(new LinkedHashMap<>());
        var versions = new LinkedHashMap<String, Object>();
        Object previous = node.getMetadata().get("verdictVersions");
        if (previous instanceof Map<?,?> map) map.forEach((key,value) -> versions.put(key.toString(), value));
        if (version == null) versions.remove(analyzer); else versions.put(analyzer, version);
        node.getMetadata().put("verdictVersions", versions);
    }
}
