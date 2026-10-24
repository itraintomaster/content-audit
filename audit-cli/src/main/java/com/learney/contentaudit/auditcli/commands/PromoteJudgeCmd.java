package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRegressionCatalog;
import com.learney.contentaudit.auditdomain.judging.JudgeExamException;
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
    private final JudgeExamRegistry exams;
    private final JudgeRegressionCatalog regressions;
    @Parameters(index="0") private String judge;
    @Option(names="--expected-version",required=true) private String version;
    @Option(names="--reviewer",required=true) private String reviewer;
    @Option(names="--evidence",required=true) private Path evidence;
    @Option(names="--reason",required=true) private String reason;
    @Option(names="--exam-fingerprint") private String examFingerprint;
    @Option(names="--regression-suite-fingerprint",
            description="Huella de la suite; 'none' declara explícitamente que no hay regresiones permanentes.")
    private String suiteFingerprint;
    @Option(names="--exam-recorded-at") private String examRecordedAt;
    @Option(names="--accept-regression-case",split=",") private List<String> acceptedRegressionCases = new ArrayList<>();
    @Option(names="--accept-case",split=",") private List<String> acceptedCases = new ArrayList<>();
    public PromoteJudgeCmd(JudgePromotionRegistry registry,Supplier<Map<String,JudgeExamStanding>> active) {
        this(registry,active,null,JudgeRegressionCatalog.empty());
    }
    public PromoteJudgeCmd(JudgePromotionRegistry registry,Supplier<Map<String,JudgeExamStanding>> active,
            JudgeExamRegistry exams,JudgeRegressionCatalog regressions) {
        this.registry=Objects.requireNonNull(registry);this.active=Objects.requireNonNull(active);
        this.exams=exams;this.regressions=Objects.requireNonNull(regressions);
    }
    @Override public Integer call() throws Exception {
        var standing=active.get().get(judge);
        if(standing==null || standing.getJudgeVersion()==null || !version.equals(standing.getJudgeVersion())) {
            System.err.println("No se promovió: el juez o la versión vigente no coincide con la revisada.");return 2;
        }
        if(!Files.isRegularFile(evidence)) {System.err.println("No se promovió: falta el archivo de evidencia revisada.");return 2;}
        var file=evidence.toAbsolutePath().normalize();
        var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        boolean exceptionRequested=examFingerprint!=null || suiteFingerprint!=null || examRecordedAt!=null
                || !acceptedRegressionCases.isEmpty() || !acceptedCases.isEmpty();
        var suite=regressions.suite(judge);
        if (!suite.cases().isEmpty() && !exceptionRequested) {
            System.err.println("No se promovió: el examen permanente requiere una excepción explícita ligada a la medición y a sus casos discrepantes.");return 2;
        }
        JudgePromotionRecord record;
        try {
            JudgeExamException exception=null;
            if(exceptionRequested) {
                if(exams==null || examFingerprint==null || suiteFingerprint==null || examRecordedAt==null)
                    throw new IllegalArgumentException("La excepción necesita examen, suite explícita, fecha de medición y los casos discrepantes aceptados.");
                String acceptedSuiteFingerprint=suiteFingerprint;
                if("none".equals(suiteFingerprint)) {
                    if(!suite.cases().isEmpty() || !acceptedRegressionCases.isEmpty())
                        throw new IllegalArgumentException("'none' sólo corresponde a una suite sin regresiones permanentes ni casos permanentes aceptados.");
                    acceptedSuiteFingerprint=null;
                }
                exception=new JudgeExamException(examFingerprint,acceptedSuiteFingerprint,Instant.parse(examRecordedAt),
                        acceptedRegressionCases,acceptedCases);
            }
            record=new JudgePromotionRecord(judge,version,standing.getRuntimeIdentity(),reviewer,reason,file.toString(),hash,Instant.now(),exception);
            if(exception!=null) {
                var measured=exams.latest(judge,version,standing.getRuntimeIdentity());
                if(measured==null || measured.isEmpty() || !record.coversExamException(measured.get(),suite))
                    throw new IllegalArgumentException("La excepción no coincide exactamente con la última medición completa, su versión, runtime, suite y discrepancias.");
            }
        } catch(IllegalArgumentException | java.time.format.DateTimeParseException invalid) {
            System.err.println("No se promovió: "+invalid.getMessage());return 2;
        }
        var previous=registry.latest(judge,version,standing.getRuntimeIdentity());
        if(previous.isEmpty() || !previous.get().reviewer().equals(reviewer) || !previous.get().reason().equals(reason)
                || !previous.get().evidenceSha256().equals(hash)
                || !Objects.equals(previous.get().examException(),record.examException())) registry.record(record);
        System.out.println("Promovido: "+judge+" · "+standing.getRuntimeIdentity().getProviderName()+"/"+standing.getRuntimeIdentity().getModelName()
                +" · versión "+version+" · aprobado por "+reviewer+". Las consultas siguen requiriendo un tope explícito.");
        return 0;
    }
}
