package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Append-only operator decisions, deliberately outside judge-exams. */
public final class FileSystemJudgePromotionRegistry implements JudgePromotionRegistry {
    private final Path baseDir;
    private final ObjectMapper json=new ObjectMapper();
    public FileSystemJudgePromotionRegistry(Path baseDir) { this.baseDir=baseDir; }
    private Path file(String judge) {
        if (judge==null || !judge.matches("[A-Za-z0-9._-]+")) throw new IllegalArgumentException("Juez inválido");
        return baseDir.resolve(".content-audit/judge-promotions").resolve(judge+".jsonl");
    }
    @Override public void record(JudgePromotionRecord record) {
        ObjectNode n=json.createObjectNode();n.put("judgeName",record.judgeName());n.put("judgeVersion",record.judgeVersion());
        n.put("providerName",record.runtimeIdentity().getProviderName());n.put("modelName",record.runtimeIdentity().getModelName());
        n.put("reasoningEffort",record.runtimeIdentity().getReasoningEffort());n.put("reviewer",record.reviewer());n.put("reason",record.reason());
        n.put("evidenceFile",record.evidenceFile());n.put("evidenceSha256",record.evidenceSha256());n.put("promotedAt",record.promotedAt().toString());
        Path f=file(record.judgeName());
        try {
            Files.createDirectories(f.getParent());
            try(var channel=FileChannel.open(f,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.APPEND);
                    var lock=channel.lock()) {
                var bytes=ByteBuffer.wrap((json.writeValueAsString(n)+"\n").getBytes(StandardCharsets.UTF_8));
                while(bytes.hasRemaining())channel.write(bytes);
                channel.force(true);
            }
        } catch(IOException e) { throw new UncheckedIOException("No se pudo registrar la promoción",e); }
    }
    @Override public Optional<JudgePromotionRecord> latest(String judge,String version,EvaluationRuntimeIdentity identity) {
        if (version==null || identity==null || !identity.isExplicit())return Optional.empty();
        JudgePromotionRecord latest=null;
        for(JudgePromotionRecord record:history(judge))
            if(version.equals(record.judgeVersion()) && identity.equals(record.runtimeIdentity())) latest=record;
        return Optional.ofNullable(latest);
    }
    @Override public List<JudgePromotionRecord> history(String judge) {
        Path f=file(judge);if(!Files.exists(f))return List.of();
        List<JudgePromotionRecord> records=new ArrayList<>();
        try {
            for(String line:Files.readAllLines(f,StandardCharsets.UTF_8)) {
                try {
                    var n=json.readTree(line);
                    var record=new JudgePromotionRecord(n.path("judgeName").asText(),n.path("judgeVersion").asText(),
                            EvaluationRuntimeIdentity.explicit(n.path("providerName").asText(),n.path("modelName").asText(),n.path("reasoningEffort").asText()),
                            n.path("reviewer").asText(),n.path("reason").asText(),n.path("evidenceFile").asText(),n.path("evidenceSha256").asText(),Instant.parse(n.path("promotedAt").asText()));
                    if(judge.equals(record.judgeName())) records.add(record);
                } catch(IOException | RuntimeException incomplete) { /* Cut or invalid records cannot grant approval. */ }
            }
        } catch(IOException e) { throw new UncheckedIOException("No se pudo leer la promoción",e); }
        return List.copyOf(records);
    }
}
