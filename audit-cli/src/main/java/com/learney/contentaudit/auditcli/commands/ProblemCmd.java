package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditinfrastructure.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchReviewServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import com.learney.contentaudit.revisiondomain.problems.*;
import static com.learney.contentaudit.revisiondomain.problems.ContentProblem.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import picocli.CommandLine;
import picocli.CommandLine.*;

/** Saved data only: every problem command, including help, bypasses the model/NLP bootstrap. */
@Command(name = "problem", mixinStandardHelpOptions = true, description = {
        "Problemas del curso: list, show, inspect, create, edit, annotate, search, case, control, undo, update, request.",
        "No consulta modelos. --course identifica el registro compartido entre tandas.",
        "Escrituras: --revision exige la revisión leída; update usa --actor agent por defecto.",
        "La entrega --artifacts es JSON con changes y results; el operador decide Resuelto."})
final class ProblemCmd implements Callable<Integer> {
    private final Path workdir;
    private final Path project;
    private final PrintStream out;
    @Parameters(index = "0", arity = "0..1") String action;
    @Parameters(index = "1", arity = "0..1") String id;
    @Option(names = "--course") Path course;
    @Option(names = "--title") String title;
    @Option(names = "--criterion") String criterion;
    @Option(names = "--failure") List<String> failures;
    @Option(names = "--part") List<String> parts;
    @Option(names = "--clear-failures") boolean clearFailures;
    @Option(names = "--clear-parts") boolean clearParts;
    @Option(names = "--revision") Long revision;
    @Option(names = "--batch") String batch;
    @Option(names = "--quiz") List<String> quizzes;
    @Option(names = "--content-version") Integer contentVersion;
    @Option(names = "--correction-revision") Integer correctionRevision;
    @Option(names = "--comment") String comment;
    @Option(names = "--expected") String expected;
    @Option(names = "--usage", defaultValue = "exam") String usage;
    @Option(names = "--keep-correction") boolean keepCorrection;
    @Option(names = "--verdicts") String verdicts;
    @Option(names = "--case-id") List<String> caseIds;
    @Option(names = "--actor") String actor;
    @Option(names = "--state") String state;
    @Option(names = "--step") String step;
    @Option(names = "--note") String note;
    @Option(names = "--artifacts") Path artifacts;
    @Option(names = "--search") String search;
    @Option(names = "--field", defaultValue = "miniTheory") String field;
    @Option(names = "--before") boolean before;
    @Option(names = "--regex") boolean regex;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;

    ProblemCmd(Path workdir, Path project, PrintStream out) { this.workdir = workdir; this.project = project; this.out = out; }
    static Integer execute(List<String> args, Path workdir, Path project, PrintStream out, PrintStream err) {
        if (args.isEmpty() || !args.get(0).equals("problem")) return null;
        var command = new CommandLine(new ProblemCmd(workdir, project, out));
        command.setOut(new PrintWriter(out, true)); command.setErr(new PrintWriter(err, true));
        command.setExecutionExceptionHandler((e, cli, parse) -> { err.println("Error: " + e.getMessage()); return 2; });
        return command.execute(args.subList(1, args.size()).toArray(String[]::new));
    }
    @Override public Integer call() throws Exception {
        if (action == null) throw new IllegalArgumentException("Elegí una operación de problem; usá --help.");
        if (!List.of("json", "text").contains(format)) throw new IllegalArgumentException("Formato: json o text.");
        if (course == null) {
            String resolved = CoursePathResolver.resolve(null);
            course = resolved == null ? project.resolve("db/english-course") : Path.of(resolved);
        }
        if (!Files.isDirectory(course)) throw new IllegalArgumentException("No existe la carpeta del curso.");
        course = course.toRealPath();
        var store = new FileSystemProblemStore(course);
        var service = new ProblemService(store, Clock.systemUTC());
        Object result = switch (action) {
            case "list" -> Map.of("problems", service.list(), "course", course.toString());
            case "show" -> service.get(requireId());
            case "inspect" -> ProblemInspection.inspect(project, service.get(requireId()));
            case "create" -> service.create(title, criterion, failures, parts);
            case "edit" -> service.edit(requireId(), requireRevision(), title, criterion,
                    clearFailures && failures == null ? List.of() : failures,
                    clearParts && parts == null ? List.of() : parts);
            case "case" -> service.use(requireId(), requireRevision(), oneCaseId(), use(), actor(false));
            case "control" -> selectMeasuredControl(service);
            case "annotate" -> store.exclusive(() -> annotate(service));
            case "search" -> search();
            case "undo" -> store.exclusive(() -> undo(service));
            case "update" -> service.update(requireId(), requireRevision(), status(), step, note,
                    artifacts == null ? null : readMap(Files.readString(artifacts)), actor(true));
            case "request" -> requestResult(service.get(requireId()));
            default -> throw new IllegalArgumentException("Operación de problem desconocida: " + action);
        };
        if (action.equals("request") && format.equals("text")) out.println(((Map<?, ?>) result).get("text"));
        else out.println(CommandJson.writeCompact(result));
        return 0;
    }

    private ContentProblem selectMeasuredControl(ProblemService service) throws IOException {
        if (artifacts == null) throw new IllegalArgumentException("El control necesita --artifacts con su snapshot y medición.");
        var payload = readMap(Files.readString(artifacts));
        var control = CommandJson.read(CommandJson.writeCompact(payload.get("control")), Case.class);
        @SuppressWarnings("unchecked")
        var delivery = (Map<String, Object>) payload.get("delivery");
        if (control == null || delivery == null) throw new IllegalArgumentException("Falta control o delivery.");
        return service.selectMeasuredControl(requireId(), requireRevision(), control, delivery, actor(false));
    }

    private Map<String, Object> annotate(ProblemService service) {
        if (quizzes == null || quizzes.isEmpty()) throw new IllegalArgumentException("Elegí al menos un --quiz.");
        use();
        if (batch == null) throw new IllegalArgumentException("Falta --batch.");
        Map<String, Map<String, Object>> said = verdicts();
        long reviewCount = new FileSystemBatchStore(workdir).reviews(batch).size();
        var reviews = reviews(workdir);
        BatchReview read = checkedReview(reviews);
        var selected = read.items().stream().filter(i -> quizzes.contains(i.step().getQuizId())).toList();
        // F-NADIE-R005: a quiz without a correction can only be annotated with what each judge said of it.
        for (String quiz : quizzes) if (selected.stream().noneMatch(i -> i.step().getQuizId().equals(quiz)) && !said.containsKey(quiz)) {
            throw new IllegalArgumentException("El quiz no tiene una corrección en esta tanda: " + quiz);
        }
        boolean rejects = !keepCorrection && use() != Use.GOOD && !selected.isEmpty();
        if (rejects && read.report().getStatus() == BatchStatus.PUBLISHED)
            throw new IllegalStateException("La tanda ya está publicada. Anotá con --keep-correction.");
        ContentProblem p = id == null ? service.create(title, criterion, failures, parts) : service.get(id);
        if (id != null && p.revision() != requireRevision()) throw new IllegalStateException("El problema cambió. Volvé a leer.");
        var additions = new ArrayList<Case>();
        CourseEntity loaded = null;
        for (String quiz : new LinkedHashSet<>(quizzes)) {
            var items = selected.stream().filter(i -> i.step().getQuizId().equals(quiz)).toList();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            String identity;
            if (items.isEmpty()) {
                // Nothing changes it in this batch: it is frozen as the course has it, before and after alike.
                if (loaded == null) loaded = new FileSystemCourseRepository(new CourseValidatorImpl()).load(course);
                var element = new DefaultCourseElementLocator().snapshot(loaded, AuditTarget.QUIZ, quiz)
                        .orElseThrow(() -> new IllegalArgumentException("El quiz no está en el curso: " + quiz));
                var frozen = readMap(CommandJson.writeCompact(element));
                snapshot.put("items", List.of());
                snapshot.put("before", frozen);
                snapshot.put("after", frozen);
                snapshot.put("rejectOnAnnotation", false);
                identity = read.batchId() + "\n" + quiz + "\n" + CommandJson.writeCompact(element);
            } else {
                snapshot.put("items", items.stream().map(i -> readMap(CommandJson.writeCompact(i))).toList());
                snapshot.put("before", readMap(CommandJson.writeCompact(items.get(0).before())));
                snapshot.put("after", readMap(CommandJson.writeCompact(items.get(items.size() - 1).after())));
                snapshot.put("rejectOnAnnotation", rejects);
                // Status/notes are not part of identity: clicking Anotar twice cannot duplicate the same content.
                identity = read.batchId() + "\n" + quiz + "\n" + CommandJson.writeCompact(
                        items.stream().map(i -> new BatchReviewDecision.Selection(i.changeId(), i.step(), i.before(), i.after())).toList());
            }
            if (said.containsKey(quiz)) snapshot.put("nobodyObjects", said.get(quiz));
            additions.add(new Case(fingerprint(identity), quiz, read.batchId(), workdir.toAbsolutePath().normalize().toString(),
                    read.contentVersion(), read.correctionRevision(), use(), comment, expected, snapshot, Instant.now()));
        }
        Set<String> existing = new HashSet<>(); p.cases().forEach(c -> existing.add(c.id()));
        additions.removeIf(c -> existing.contains(c.id()));
        if (additions.isEmpty()) return Map.of("problem", p, "newCaseIds", List.of());
        var saved = service.addCases(p.id(), p.revision(), additions);
        if (rejects) {
            try {
                Set<String> addedQuizzes = new HashSet<>(); additions.forEach(c -> addedQuizzes.add(c.quizId()));
                var rejected = selected.stream().filter(i -> addedQuizzes.contains(i.step().getQuizId())
                        && i.status() != BatchReviewDecision.Status.REJECTED).toList();
                if (!rejected.isEmpty()) reviews.decide(new BatchReviewDecision(read.batchId(), selections(rejected),
                        BatchReviewDecision.Status.REJECTED, "Problemas:" + p.id(), Instant.now(), reviewCount));
            } catch (RuntimeException e) {
                // A stale/withdrawn correction must not leave an annotation that claims it was rejected.
                try { service.rollbackAnnotation(p, saved.revision()); }
                catch (RuntimeException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
        return Map.of("problem", saved, "newCaseIds", additions.stream().map(Case::id).toList(),
                "rejected", rejects);
    }

    private static final List<String> JUDGE_STATUSES = List.of("UP_TO_DATE", "EARLIER", "ABSTAINED", "NOT_MEASURED");
    private static final Set<String> SAID_NO_ERROR = Set.of("UP_TO_DATE", "EARLIER");

    /**
     * F-NADIE-R005: what each judge said of each annotated quiz in a state of the current analysis, as the
     * dashboard read it when annotating. The judges that said «sin error», today or with an earlier version,
     * are the ones that missed it; an abstention or a judge that has not judged it yet is only named.
     */
    private Map<String, Map<String, Object>> verdicts() {
        if (verdicts == null || verdicts.isBlank()) return Map.of();
        Map<String, Object> all = readMap(verdicts);
        String state = text(all.get("state")), label = text(all.get("stateLabel"));
        if (state == null || label == null || !(all.get("quizzes") instanceof Map<?, ?> byQuiz) || byQuiz.isEmpty())
            throw new IllegalArgumentException("--verdicts necesita el estado, su nombre y lo que dijo cada juez de cada quiz.");
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (var entry : byQuiz.entrySet()) {
            String quiz = String.valueOf(entry.getKey());
            if (!quizzes.contains(quiz)) throw new IllegalArgumentException("--verdicts nombra un quiz que no se anota: " + quiz);
            if (!(entry.getValue() instanceof Map<?, ?> said) || !(said.get("judges") instanceof List<?> judges) || judges.isEmpty())
                throw new IllegalArgumentException("Falta lo que dijo cada juez de " + quiz + ".");
            var named = new ArrayList<Map<String, Object>>();
            var missed = new ArrayList<String>();
            for (Object value : judges) {
                if (!(value instanceof Map<?, ?> judge) || text(judge.get("judge")) == null || !JUDGE_STATUSES.contains(judge.get("status")))
                    throw new IllegalArgumentException("Cada juez necesita su nombre y su estado: " + String.join(", ", JUDGE_STATUSES) + ".");
                Map<String, Object> clean = new LinkedHashMap<>();
                clean.put("judge", text(judge.get("judge")));
                if (text(judge.get("name")) != null) clean.put("name", text(judge.get("name")));
                clean.put("status", judge.get("status"));
                if (text(judge.get("detail")) != null) clean.put("detail", text(judge.get("detail")));
                named.add(clean);
                if (SAID_NO_ERROR.contains(judge.get("status"))) missed.add(text(judge.get("judge")));
            }
            if (named.stream().noneMatch(j -> "UP_TO_DATE".equals(j.get("status"))))
                throw new IllegalArgumentException("Ningún juez opinó de " + quiz + " con su versión de hoy: no entra en «Nadie le ve problemas».");
            var analyzers = new ArrayList<Map<String, Object>>();
            if (said.get("analyzers") instanceof List<?> list) for (Object value : list) {
                if (!(value instanceof Map<?, ?> analyzer) || text(analyzer.get("analyzer")) == null)
                    throw new IllegalArgumentException("Cada analizador necesita su nombre.");
                Map<String, Object> clean = new LinkedHashMap<>();
                clean.put("analyzer", text(analyzer.get("analyzer")));
                if (text(analyzer.get("name")) != null) clean.put("name", text(analyzer.get("name")));
                analyzers.add(clean);
            }
            Map<String, Object> kept = new LinkedHashMap<>();
            kept.put("state", state);
            kept.put("stateLabel", label);
            kept.put("missedBy", missed);
            kept.put("judges", named);
            kept.put("analyzers", analyzers);
            result.put(quiz, kept);
        }
        return result;
    }
    private static String text(Object value) {
        return value instanceof String s && !s.isBlank() ? s.trim() : null;
    }

    private ContentProblem undo(ProblemService service) {
        var p = service.get(requireId());
        if (p.revision() != requireRevision()) throw new IllegalStateException("El problema cambió. Volvé a leer.");
        if (caseIds == null || caseIds.isEmpty()) throw new IllegalArgumentException("Falta --case-id.");
        var chosen = p.cases().stream().filter(c -> caseIds.contains(c.id())).toList();
        if (chosen.size() != new HashSet<>(caseIds).size()) throw new IllegalArgumentException("No existe alguno de los casos.");
        // Undo only our rejection, never a later decision or a different correction.
        record Restoration(BatchReviewService reviews, String batchId, BatchReview.Item item, String key) {}
        var restorations = new ArrayList<Restoration>();
        Map<String, Long> counts = new HashMap<>();
        for (var c : chosen) {
            if (!Boolean.TRUE.equals(c.snapshot().get("rejectOnAnnotation"))) continue;
            var reviews = reviews(Path.of(c.workdir()));
            String key = c.workdir() + "\n" + c.batchId();
            counts.putIfAbsent(key, (long) new FileSystemBatchStore(Path.of(c.workdir())).reviews(c.batchId()).size());
            var read = reviews.read(c.batchId());
            for (var original : items(c)) {
                var current = read.items().stream().filter(i -> i.changeId().equals(original.changeId())
                        && i.step().equals(original.step()) && Objects.equals(i.before(), original.before())
                        && Objects.equals(i.after(), original.after())).findFirst()
                        .orElseThrow(() -> new IllegalStateException("La corrección cambió; no se puede deshacer automáticamente."));
                if (original.status() == BatchReviewDecision.Status.REJECTED) continue;
                if (current.status() == BatchReviewDecision.Status.REJECTED && ("Problemas:" + p.id()).equals(current.reviewer())) {
                    restorations.add(new Restoration(reviews, c.batchId(), original, key));
                } else if (current.status() != original.status()) {
                    throw new IllegalStateException("La decisión cambió después de anotar. Volvé a leer antes de deshacer.");
                }
            }
        }
        for (var r : restorations) {
            r.reviews().decide(new BatchReviewDecision(r.batchId(), selections(List.of(r.item())),
                    r.item().status(), "Deshacer problema", Instant.now(), counts.get(r.key())));
            counts.put(r.key(), counts.get(r.key()) + 1);
        }
        return service.removeCases(p.id(), p.revision(), new HashSet<>(caseIds));
    }

    private Map<String, Object> search() {
        if (search == null || search.isBlank()) throw new IllegalArgumentException("Escribí qué buscar.");
        if (!List.of("miniTheory", "instructions", "options", "hint").contains(field)) throw new IllegalArgumentException("Parte de búsqueda inválida.");
        Pattern pattern = regex ? Pattern.compile(search, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : null;
        var read = reviews(workdir).read(batch);
        Map<String, LinkedHashSet<String>> groups = new LinkedHashMap<>();
        for (var item : read.items()) {
            var snapshot = before ? item.before() : item.after();
            if (snapshot == null || snapshot.getQuiz() == null) continue;
            Map<String, Object> quiz = readMap(CommandJson.writeCompact(snapshot.getQuiz()));
            Object value = switch (field) {
                case "options" -> quiz.get("form");
                case "hint" -> quiz.get("unmodeledFields") instanceof Map<?, ?> extras ? extras.get("hint") : null;
                default -> quiz.get(field);
            };
            String text = value == null ? "" : value instanceof String s ? s : CommandJson.writeCompact(value);
            boolean matches = pattern == null ? text.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)) : pattern.matcher(text).find();
            if (matches) groups.computeIfAbsent(text, ignored -> new LinkedHashSet<>()).add(item.step().getQuizId());
        }
        return Map.of("batchId", read.batchId(), "contentVersion", read.contentVersion(),
                "correctionRevision", read.correctionRevision(), "groups", groups.entrySet().stream()
                        .map(e -> Map.of("text", e.getKey(), "quizIds", List.copyOf(e.getValue()))).toList());
    }

    private BatchReview checkedReview(BatchReviewService reviews) {
        if (batch == null || contentVersion == null || correctionRevision == null) {
            throw new IllegalArgumentException("Anotar necesita --batch, --content-version y --correction-revision de la tanda visible.");
        }
        var read = reviews.read(batch);
        if (read.contentVersion() != contentVersion || read.correctionRevision() != correctionRevision) {
            throw new IllegalStateException("La tanda cambió. Volvé a leer antes de anotar.");
        }
        return read;
    }
    private static BatchReviewService reviews(Path directory) {
        return new DefaultBatchReviewServiceFactory().create(new FileSystemBatchStore(directory),
                new FileSystemRevisionArtifactStore(directory), new FileSystemRefinementPlanStore(directory),
                new FileSystemBatchReviewCache(directory));
    }
    private static List<BatchReviewDecision.Selection> selections(List<BatchReview.Item> items) {
        return items.stream().map(i -> new BatchReviewDecision.Selection(i.changeId(), i.step(), i.before(), i.after())).toList();
    }
    private static List<BatchReview.Item> items(Case c) {
        if (!(c.snapshot().get("items") instanceof List<?> list)) throw new IllegalArgumentException("El caso no tiene su procedencia.");
        return list.stream().map(v -> CommandJson.read(CommandJson.writeCompact(v), BatchReview.Item.class)).toList();
    }

    private Map<String, Object> requestResult(ContentProblem p) throws IOException {
        return Map.of("text", request(p), "revision", p.revision());
    }
    private String request(ContentProblem p) throws IOException {
        if (p.criterion().isBlank() || p.cases().isEmpty()) throw new IllegalArgumentException("Completá el criterio y al menos un caso.");
        String command = quote(project.resolve("audit-cli.sh").toAbsolutePath().toString())
                + " --workdir " + quote(workdir.toAbsolutePath().toString()) + " problem";
        String flags = " --course " + quote(course.toString());
        String instructions = "Trabajá el problema " + p.id() + ": " + p.title() + ".\n"
                + "Hay dos roles. Vos recibís TODOS los casos y podés elegir algunos para validar. Recomendá ajustar un juez existente, crear uno, una regla fija o la guía del corrector, explicando por qué.\n"
                + "La descripción general se aplica a todos los quizzes de la lista. Inferí cómo se manifiesta el mismo patrón en cada quiz a partir de su contenido congelado; no hace falta que el operador repita la explicación para cada caso.\n"
                + (p.cases().stream().anyMatch(c -> c.snapshot().containsKey("nobodyObjects"))
                    ? "Un caso con nobodyObjects es un ejercicio al que ningún juez ni analizador le vio problemas en ese estado (stateLabel). missedBy son los jueces que dijeron «sin error»: son los que hay que recalibrar. Los que se abstuvieron o todavía no opinaron no se equivocaron. Si items está vacío, el ejercicio no tenía cambios: antes y después son el mismo.\n"
                    : "")
                + "El juez recibe solo una guía general y el quiz evaluado. No copies estos ejemplos ni respuestas esperadas a su contexto.\n"
                + "Probá primero sin consultas. No ejecutes jueces, exámenes ni correcciones con modelos sin autorización explícita y un tope. codex-cli es el proveedor por omisión; el operador puede elegir otro. No hagas fallback silencioso. Registrá proveedor, modelo y versión efectivos.\n"
                + "Actualizar el estado ES parte de tu trabajo. Necesitás acceso al proyecto para ejecutar cambios. Proyecto: " + project.toAbsolutePath() + ".\n"
                + "Al empezar ejecutá:\n" + command + " update " + p.id() + flags + " --revision " + p.revision()
                + " --state en-trabajo --step diagnostico --note 'Pedido recibido'\n"
                + "Leé la revisión actual antes de cada escritura:\n" + command + " show " + p.id() + flags + "\n"
                + "Registrá cada paso o espera con update, --revision leída, --state en-trabajo --step <paso> --note <motivo>.\n"
                + "Para elegir casos de evaluación usá:\n" + command + " case " + p.id() + flags + " --revision <actual> --case-id <id> --usage exam --actor agent\n"
                + "Al entregar usá:\n" + command + " update " + p.id() + flags + " --revision <actual> --state para-probar --step entrega --artifacts /ruta/entrega.json\n"
                + "entrega.json: {\"changes\":[{\"path\":\"...\",\"version\":\"...\",\"summary\":\"...\",\"scope\":\"judge\"}],\"results\":[{\"caseId\":\"...\",\"detected\":true,\"fixed\":true,\"version\":\"...\",\"modelConsulted\":false}]}. scope: judge, corrector o rule. detected dice si el juez ve el problema; fixed, si quedó arreglado. No inventes resultados. Si modelConsulted es true, cada resultado necesita provider, model y runtimeVersion efectivos. Los cambios al juez deben pasar todos los casos Examen y Está bien, con al menos un positivo y un negativo.\n"
                + "Si la evaluación falla, seguí En trabajo y adjuntá el resultado. Resuelto lo decide el operador.\n\n"
                + "GUÍA DE TRABAJO DEL PROYECTO:\n"
                + (Files.isRegularFile(project.resolve("AGENTS.md")) ? Files.readString(project.resolve("AGENTS.md"))
                    : "Sentinel no gobierna este repositorio. No ejecutes su generador. Editá los archivos manualmente, respetando los módulos.\n") + "\n"
                + "FICHA Y CASOS COMPLETOS (antes, después, procedencia, comentarios y expectativas):\n" + CommandJson.write(p) + "\n\nGUÍAS ACTUALES:\n";
        StringBuilder guides = new StringBuilder();
        Set<String> names = new LinkedHashSet<>();
        Set<String> parts = new LinkedHashSet<>(p.parts());
        for (var c : p.cases()) for (var item : items(c))
            item.step().getParts().forEach(part -> parts.add(part.name()));
        for (String part : parts) {
            String lower = part.toLowerCase(Locale.ROOT);
            if (lower.contains("mini") || lower.contains("theory")) names.add("mini-theory-judge");
            if (lower.contains("instruction") || lower.contains("consigna")) names.add("quiz-instruction-validator");
            if (lower.contains("option") || lower.contains("choice") || lower.equals("form") || lower.equals("form_kind")) names.add("choice-quality-judge");
            if (lower.contains("statement") || lower.contains("sentence") || lower.contains("translation")) {
                names.addAll(List.of("naturalness-yes-no", "translation-sense-from-english", "translation-sense-from-translation"));
            }
            if (lower.contains("answer")) names.add("single-answer-judge");
        }
        // F-NADIE-R005: the judges that said «sin error» of a case are the ones to recalibrate.
        for (var c : p.cases()) if (c.snapshot().get("nobodyObjects") instanceof Map<?, ?> said
                && said.get("missedBy") instanceof List<?> missed) {
            for (Object judge : missed) names.addAll(guidesOf(String.valueOf(judge)));
        }
        for (var c : p.cases()) for (var item : items(c)) {
            String rule = String.valueOf(item.ruleId());
            if (rule.contains("mini-theory")) names.add("mini-theory-judge");
            if (rule.contains("instruction")) names.add("quiz-instruction-validator");
            if (rule.contains("choice")) names.add("choice-quality-judge");
        }
        // The correction panel and criteria are relevant even when the chosen judge is not known yet.
        names.addAll(List.of("panel-proposer", "candidate-judgment"));
        if (names.contains("quiz-instruction-validator")) names.add("quiz-instruction-corrector");
        for (String name : names) {
            Path dir = project.resolve(".sentinel/agents").resolve(name);
            if (!Files.isDirectory(dir)) continue;
            try (var files = Files.walk(dir)) {
                for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                    String relative = dir.relativize(f).toString();
                    if (relative.startsWith("eval/") || !(relative.equals("agent.yaml") || relative.startsWith("nodes/"))) continue;
                    String source = Files.readString(f);
                    guides.append("\n--- ").append(project.relativize(f)).append(" | sha256:")
                            .append(sha256(source)).append(" ---\n").append(source);
                }
            }
        }
        if (guides.isEmpty()) guides.append("No se identificó una guía guardada para estas partes. Confirmá cuál corresponde antes de modificar un juez.\n");
        return instructions + guides;
    }

    /** The saved guides of a judge of the analysis: its own folder, or the ones named differently. */
    private static final Map<String, List<String>> JUDGE_GUIDES = Map.of(
            "quiz-instruction", List.of("quiz-instruction-validator"),
            "translation-sense", List.of("translation-sense-from-english", "translation-sense-from-translation"),
            "naturalness", List.of("native-usage-judge"),
            "near-duplicates", List.of("near-duplicates-pair-judge"),
            "multiple-choice-quality", List.of("choice-quality-judge"),
            "multiple-choice-validity", List.of("choice-alternative-judge"));
    static List<String> guidesOf(String judge) {
        if (!judge.matches("[a-z0-9-]+")) return List.of();
        return JUDGE_GUIDES.getOrDefault(judge, List.of(judge + "-judge", judge));
    }

    private String requireId() { if (id == null) throw new IllegalArgumentException("Falta el id del problema."); return id; }
    private long requireRevision() { if (revision == null || revision < 0) throw new IllegalArgumentException("Falta --revision de la ficha leída."); return revision; }
    private String oneCaseId() { if (caseIds == null || caseIds.size() != 1) throw new IllegalArgumentException("Indicá un --case-id."); return caseIds.get(0); }
    private Use use() {
        return switch (usage.toLowerCase(Locale.ROOT)) { case "example", "ejemplo" -> Use.EXAMPLE; case "exam", "examen" -> Use.EXAM; case "good", "esta-bien" -> Use.GOOD; default -> throw new IllegalArgumentException("Uso: example, exam o good."); };
    }
    private Actor actor(boolean defaultAgent) {
        if (actor == null) return defaultAgent ? Actor.AGENT : Actor.OPERATOR;
        return switch (actor) { case "agent" -> Actor.AGENT; case "operator" -> Actor.OPERATOR; default -> throw new IllegalArgumentException("Actor: agent u operator."); };
    }
    private Status status() {
        if (state == null) throw new IllegalArgumentException("Falta --state.");
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "juntando", "collecting" -> Status.COLLECTING;
            case "para-trabajar", "ready" -> Status.READY;
            case "en-trabajo", "working" -> Status.WORKING;
            case "para-probar", "for-testing" -> Status.FOR_TESTING;
            case "resuelto", "resolved" -> Status.RESOLVED;
            default -> throw new IllegalArgumentException("Estado de problema desconocido.");
        };
    }
    private static Map<String, Object> readMap(String json) {
        try { return new ObjectMapper().readValue(json, new TypeReference<>() {}); }
        catch (IOException e) { throw new IllegalArgumentException("JSON inválido.", e); }
    }
    private static String fingerprint(String text) {
        return "CAS-" + sha256(text).substring(0, 24);
    }
    private static String sha256(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String quote(String text) { return "'" + text.replace("'", "'\\''") + "'"; }
}
