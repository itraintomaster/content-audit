package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

/** Explicit human acceptance after reviewing evidence; never invokes an evaluator. */
@Command(name="promote-judge",mixinStandardHelpOptions=true,
        description="Promueve la versión vigente por revisión del operador, conservando los resultados del examen.")
public final class PromoteJudgeCmd implements Callable<Integer> {
    private final JudgePromotionRegistry registry;
    private final Supplier<Map<String,JudgeExamStanding>> active;
    @Parameters(index="0") private String judge;
    @Option(names="--expected-version",required=true) private String version;
    @Option(names="--reviewer",required=true) private String reviewer;
    @Option(names="--evidence",required=true) private Path evidence;
    @Option(names="--reason",required=true) private String reason;
    public PromoteJudgeCmd(JudgePromotionRegistry registry,Supplier<Map<String,JudgeExamStanding>> active) {
        this.registry=Objects.requireNonNull(registry);this.active=Objects.requireNonNull(active);
    }
    @Override public Integer call() throws Exception {
        var standing=active.get().get(judge);
        if(standing==null || standing.getJudgeVersion()==null || !version.equals(standing.getJudgeVersion())) {
            System.err.println("No se promovió: el juez o la versión vigente no coincide con la revisada.");return 2;
        }
        if(!Files.isRegularFile(evidence)) {System.err.println("No se promovió: falta el archivo de evidencia revisada.");return 2;}
        var file=evidence.toAbsolutePath().normalize();
        var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        var record=new JudgePromotionRecord(judge,version,standing.getRuntimeIdentity(),reviewer,reason,file.toString(),hash,Instant.now());
        var previous=registry.latest(judge,version,standing.getRuntimeIdentity());
        if(previous.isEmpty() || !previous.get().reviewer().equals(reviewer) || !previous.get().reason().equals(reason)
                || !previous.get().evidenceSha256().equals(hash)) registry.record(record);
        System.out.println("Promovido: "+judge+" · "+standing.getRuntimeIdentity().getProviderName()+"/"+standing.getRuntimeIdentity().getModelName()
                +" · versión "+version+" · aprobado por "+reviewer+". Las consultas siguen requiriendo un tope explícito.");
        return 0;
    }
}
