package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;

final class EnglishQualityAnalyzer implements ContentAnalyzer {
    private final EnglishQualityAnalyzerProvider.Kind kind;
    private final JudgeSessions opened;
    private final JudgeRuntime runtime;
    private final EnglishQualitySubjectBuilder builder;
    private final EnglishQualityVerdictReader reader;
    private final List<AuditNode> reached = new ArrayList<>();
    private final Set<AuditNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<AuditNode,List<FindingDraft>> findings = new IdentityHashMap<>();
    private final Map<String,Integer> counts = new LinkedHashMap<>();
    private boolean completed;
    EnglishQualityAnalyzer(EnglishQualityAnalyzerProvider.Kind kind, JudgeSessions opened, JudgeRuntime runtime,
            EnglishQualitySubjectBuilder builder, EnglishQualityVerdictReader reader) {
        this.kind=kind; this.opened=opened; this.runtime=runtime; this.builder=builder; this.reader=reader;
    }
    @Override public Void onQuiz(AuditNode node) {
        if (node != null && node.getEntity() instanceof AuditableQuiz && seen.add(node)
                && (kind != EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC || "A1".equalsIgnoreCase(EnglishQualitySubjectBuilder.level(node)))) reached.add(node);
        return null;
    }
    @Override public Void onKnowledge(AuditNode n) { return null; }
    @Override public Void onTopic(AuditNode n) { return null; }
    @Override public Void onMilestone(AuditNode n) { return null; }
    @Override public String getName() { return kind.name; }
    @Override public AuditTarget getTarget() { return AuditTarget.QUIZ; }
    @Override public String getDescription() { return kind.label; }
    @Override public List<FindingDraft> findingsAt(AuditNode n) { return findings.getOrDefault(n,List.of()); }
    @Override public Void onCourseComplete(AuditNode root) {
        if (completed) return null; completed=true;
        var policy = opened == null ? null : opened.getEffectivePolicy();
        var plan = runtime.plan(reached,policy);
        var units = plan == null || plan.getUnits() == null ? List.<AuditNode>of() : plan.getUnits();
        var modes = plan == null || plan.getModes() == null ? List.<EvaluationRequestMode>of() : plan.getModes();
        var asked = new ArrayList<AuditNode>(); var subjects = new ArrayList<EvaluationSubject>();
        var requests = new ArrayList<EvaluationRequest>();
        for (int i=0;i<units.size();i++) {
            var n=units.get(i); clear(n);
            try { var subject=builder.build(n); subjects.add(subject); asked.add(n);
                requests.add(new EvaluationRequest(subject,i<modes.size() && modes.get(i)!=null ? modes.get(i) : EvaluationRequestMode.REUSE_OR_EVALUATE));
            } catch (RuntimeException badInput) { increment("INPUT_UNAVAILABLE"); }
        }
        List<EvaluationResolution> answers=List.of();
        if (!requests.isEmpty() && opened != null && opened.getSessions()!=null && !opened.getSessions().isEmpty()) {
            try { var r=opened.getSessions().get(0).resolveAll(requests,policy!=null && policy.getMaxConcurrentEvaluations()!=null ? Math.max(1,policy.getMaxConcurrentEvaluations()) : 1);
                if (r!=null) answers=r;
            } catch (RuntimeException unavailable) { /* unavailable is pending, never approval */ }
        }
        int evaluated=0; var versions=new LinkedHashMap<String,Integer>();
        for (int i=0;i<asked.size();i++) {
            var a=i<answers.size()?answers.get(i):null;
            if (judge(asked.get(i),subjects.get(i),a)) { evaluated++; if(a.getEvaluatorVersion()!=null) versions.merge(a.getEvaluatorVersion(),1,Integer::sum); }
        }
        if (root!=null) {
            if(root.getMetadata()==null) root.setMetadata(new LinkedHashMap<>());
            root.getMetadata().put(getName()+"Counts",new LinkedHashMap<>(counts));
            if(opened!=null) {
                if(!(root.getDiagnoses() instanceof DefaultCourseDiagnoses)) root.setDiagnoses(new DefaultCourseDiagnoses());
                var c=runtime.coverage(opened,new JudgeUnitTally(AuditTarget.QUIZ,units.size(),evaluated,units.size()-evaluated,versions,
                        plan==null?null:plan.getUnmatchedDeclaredIds(),new JudgeQuizTally(units.size(),evaluated,units.size()-evaluated)));
                if(c!=null) ((DefaultCourseDiagnoses)root.getDiagnoses()).addJudgeCoverage(c);
            }
        }
        return null;
    }
    private boolean judge(AuditNode n, EvaluationSubject subject, EvaluationResolution answer) {
        if(answer==null || (answer.getKind()!=EvaluationResolutionKind.EVALUATED && answer.getKind()!=EvaluationResolutionKind.REUSED)) { increment("PENDING_PROVIDER_OR_BUDGET"); return false; }
        try {
            var drafts=new ArrayList<FindingDraft>(); Object verdict; Double score; boolean pending=false;
            if(kind==EnglishQualityAnalyzerProvider.Kind.NATURALNESS) {
                var v=reader.readNaturalness(answer.getPayload()); verdict=v;
                var expected=builder.sentences(n);
                if(v.assessments().size()!=expected.size() || !new HashSet<>(expected).equals(v.assessments().stream().map(NativeUsageVerdict.Assessment::sentence).collect(java.util.stream.Collectors.toSet())))
                    throw new IllegalArgumentException("Cobertura exacta de variantes incompleta");
                validateEquivalentVariants(v);
                boolean fail=false, applicable=false;
                for(var a:v.assessments()) {
                    increment(a.status().name());
                    if(a.status()!=NativeUsageVerdict.Status.NOT_APPLICABLE) applicable=true;
                    if(a.status()==NativeUsageVerdict.Status.REVIEW) pending=true;
                    if(a.status()==NativeUsageVerdict.Status.FAIL) { fail=true; drafts.add(finding("unidiomatic-formulation",a.sentence(),a.severity(),a.reason(),subject)); }
                }
                score = !applicable || (pending && !fail) ? null : fail ? 0.0 : 1.0;
            } else {
                var v=reader.readDidactic(answer.getPayload()); verdict=v;
                if(!subject.getSubjectRef().equals(v.quizId()) || !new HashSet<>(builder.sentences(n)).containsAll(v.affectedSentences()))
                    throw new IllegalArgumentException("Veredicto de otro ejercicio o variantes ajenas");
                increment(v.status().name()); pending=v.status()==A1DidacticVerdict.Status.REVIEW;
                boolean ownDefect = v.issue()!=null && (v.status()==A1DidacticVerdict.Status.IMPROVE || v.status()==A1DidacticVerdict.Status.REWRITE_REQUIRED);
                score = pending || v.status()==A1DidacticVerdict.Status.NOT_APPLICABLE ? null : ownDefect ? 0.0 : 1.0;
                // Handoffs are evidence, not a second finding for another checker's defect.
                if(ownDefect)
                    drafts.add(finding("a1-example-quality",v.affectedSentences().isEmpty()
                                    ? subject.getContent().get("exercise") : String.join(" | ",v.affectedSentences()),
                            v.status()==A1DidacticVerdict.Status.REWRITE_REQUIRED?FindingSeverity.HIGH:FindingSeverity.LOW,v.reason()+" Propuesta: "+v.proposedAction(),subject));
            }
            n.getMetadata().put(getName()+"Verdict",verdict);
            n.getMetadata().put(getName()+"Version",answer.getEvaluatorVersion());
            n.getMetadata().put(getName()+"Reused",answer.getKind()==EvaluationResolutionKind.REUSED);
            n.getMetadata().put(getName()+"InputContract",EnglishQualitySubjectBuilder.CONTRACT);
            var versions=new LinkedHashMap<String,Object>();
            if(n.getMetadata().get("verdictVersions") instanceof Map<?,?> prior) prior.forEach((k,v)->versions.put(k.toString(),v));
            versions.put(getName(),answer.getEvaluatorVersion()); n.getMetadata().put("verdictVersions",versions);
            findings.put(n,List.copyOf(drafts));
            if(score!=null) n.getScores().put(getName(),score);
            n.getUnevaluatedBy().remove(getName());
            if(pending) {
                if(score!=null) n.getPartiallyEvaluatedBy().add(getName());
                else n.getUnevaluatedBy().add(getName());
            }
            return !pending;
        } catch(RuntimeException invalid) { increment("OUTPUT_UNUSABLE"); n.getMetadata().put(getName()+"OutputIssue",invalid.getMessage()); return false; }
    }
    private FindingDraft finding(String rule,String marker,FindingSeverity severity,String reason,EvaluationSubject subject) {
        var examined = List.of(
                new EvidencePart("Consigna visible",subject.getContent().get("instructions")),
                new EvidencePart("Enunciado visible",subject.getContent().get("exercise")),
                new EvidencePart("Variante o ejemplo",marker)).stream()
                .filter(part -> part.getText()!=null && !part.getText().isBlank()).toList();
        return new FindingDraft(rule,marker,severity,FindingResolution.PANEL,new FindingEvidence(examined,reason,List.of()));
    }
    private void clear(AuditNode n) {
        if(n.getScores()==null) n.setScores(new LinkedHashMap<>()); n.getScores().remove(getName());
        if(n.getMetadata()==null) n.setMetadata(new LinkedHashMap<>());
        for(var suffix:List.of("Verdict","Version","Reused","InputContract","OutputIssue")) n.getMetadata().remove(getName()+suffix);
        if(n.getMetadata().get("verdictVersions") instanceof Map<?,?> oldVersions) {
            var remaining=new LinkedHashMap<String,Object>();
            oldVersions.forEach((key,value) -> { if(!getName().equals(key.toString())) remaining.put(key.toString(),value); });
            n.getMetadata().put("verdictVersions",remaining);
        }
        if(n.getUnevaluatedBy()==null) n.setUnevaluatedBy(new ArrayList<>());
        n.getUnevaluatedBy().remove(getName()); n.getUnevaluatedBy().add(getName());
        if(n.getPartiallyEvaluatedBy()==null) n.setPartiallyEvaluatedBy(new ArrayList<>()); n.getPartiallyEvaluatedBy().remove(getName());
        if(kind==EnglishQualityAnalyzerProvider.Kind.NATURALNESS && n.getDiagnoses() instanceof DefaultQuizDiagnoses d) d.setNaturalnessDiagnosis(null);
        findings.remove(n);
    }
    private void increment(String status) { counts.merge(status,1,Integer::sum); }
    static void validateEquivalentVariants(NativeUsageVerdict verdict) {
        var states=new HashMap<String,NativeUsageVerdict.Status>();
        for(var a:verdict.assessments()) {
            var key=a.sentence().toLowerCase(Locale.ROOT).replace('’','\'');
            for(var pair:List.of(new String[]{"won't","will not"},new String[]{"can't","cannot"},new String[]{"don't","do not"},new String[]{"doesn't","does not"},new String[]{"didn't","did not"},new String[]{"haven't","have not"},new String[]{"hasn't","has not"},new String[]{"isn't","is not"},new String[]{"aren't","are not"},new String[]{"i'm","i am"},new String[]{"'re"," are"},new String[]{"'ll"," will"})) key=key.replace(pair[0],pair[1]);
            var previous=states.putIfAbsent(key,a.status());
            if(previous!=null && previous!=a.status()) throw new IllegalArgumentException("Veredictos contradictorios entre variantes equivalentes");
        }
    }
}
