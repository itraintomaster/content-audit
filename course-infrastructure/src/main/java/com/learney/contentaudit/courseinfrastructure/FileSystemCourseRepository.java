package com.learney.contentaudit.courseinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.CourseValidationException;
import com.learney.contentaudit.coursedomain.CourseValidator;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filesystem adapter for course persistence.
 * Reads and writes the hierarchical directory structure with MongoDB Extended JSON format.
 */
public class FileSystemCourseRepository implements CourseRepository {

    private static final String COURSE_FILE = "_course.json";
    private static final String MILESTONE_FILE = "_milestone.json";
    private static final String TOPIC_FILE = "_topic.json";
    private static final String KNOWLEDGE_FILE = "_knowledge.json";
    private static final String QUIZZES_FILE = "quizzes.json";

    private final CourseValidator courseValidator;
    private final ObjectMapper objectMapper;

    public FileSystemCourseRepository(CourseValidator courseValidator) {
        this.courseValidator = courseValidator;
        this.objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // CourseRepository.load
    // -------------------------------------------------------------------------

    @Override
    public CourseEntity load(Path path) {
        String pathStr = path.toString();

        if (!Files.isDirectory(path)) {
            throw new CourseValidationException(pathStr,
                    "La ruta no existe o no es un directorio");
        }

        Path courseFile = path.resolve(COURSE_FILE);
        if (!Files.exists(courseFile)) {
            throw new CourseValidationException(pathStr,
                    "El directorio no contiene el archivo descriptor esperado '" + COURSE_FILE + "'");
        }

        try {
            // 1. Read _course.json
            Map<String, Object> courseJson = readJsonMap(courseFile);
            String courseId = extractOid(courseJson.get("_id"));
            String courseTitle = (String) courseJson.get("title");
            List<String> knowledgeIds = extractOidList(courseJson.get("knowledgeIds"));
            String courseSlug = path.getFileName().toString();

            // 2. Read ROOT node (stored in _course.json under "root" key)
            Map<String, Object> rootJson = asMap(courseJson.get("root"));
            if (rootJson == null) {
                throw new CourseValidationException(pathStr,
                        "El archivo _course.json no contiene el nodo ROOT");
            }
            String rootId = extractOid(rootJson.get("_id"));
            String rootCode = (String) rootJson.get("code");
            String rootLabel = (String) rootJson.get("label");
            List<String> rootChildren = extractOidList(rootJson.get("children"));

            // 3. Discover milestones from subdirectories, ordered by ROOT children
            Map<String, MilestoneEntity> milestonesById = new LinkedHashMap<>();
            File[] entries = path.toFile().listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    if (entry.isDirectory()) {
                        Path milestoneDir = entry.toPath();
                        Path milestoneFile = milestoneDir.resolve(MILESTONE_FILE);
                        if (Files.exists(milestoneFile)) {
                            MilestoneEntity milestone = loadMilestone(milestoneDir, milestoneFile, pathStr);
                            milestonesById.put(milestone.getId(), milestone);
                        }
                    }
                }
            }

            // 4. Order milestones by ROOT children list and assign order
            List<MilestoneEntity> orderedMilestones = new ArrayList<>();
            for (int i = 0; i < rootChildren.size(); i++) {
                String milestoneId = rootChildren.get(i);
                MilestoneEntity milestone = milestonesById.get(milestoneId);
                if (milestone == null) {
                    throw new CourseValidationException(pathStr,
                            "Inconsistencia de IDs: ROOT.children referencia el ID '"
                                    + milestoneId + "' que no existe en la estructura");
                }
                milestone.setOrder(i + 1);
                orderedMilestones.add(milestone);
            }

            // 5. Build ROOT node entity
            RootNodeEntity root = new RootNodeEntity(
                    rootId, rootCode, NodeKind.ROOT, rootLabel,
                    rootChildren, orderedMilestones
            );

            // 6. Build CourseEntity
            CourseEntity course = new CourseEntity(
                    courseId, courseTitle, knowledgeIds, root, courseSlug
            );

            // 7. Validate
            courseValidator.validate(course);

            return course;

        } catch (CourseValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new CourseValidationException(pathStr,
                    "Error al leer el archivo JSON: " + e.getMessage());
        } catch (Exception e) {
            throw new CourseValidationException(pathStr,
                    "Error inesperado durante la carga: " + e.getMessage());
        }
    }

    private MilestoneEntity loadMilestone(Path milestoneDir, Path milestoneFile, String coursePath)
            throws IOException {
        Map<String, Object> json = readJsonMap(milestoneFile);

        String id = extractOid(json.get("_id"));
        String code = (String) json.get("code");
        String label = (String) json.get("label");
        String oldId = (String) json.get("oldId");
        String parentId = extractOid(json.get("parentId"));
        List<String> children = extractOidList(json.get("children"));
        String slug = milestoneDir.getFileName().toString();

        // Discover topics ordered by milestone children
        Map<String, TopicEntity> topicsById = new LinkedHashMap<>();
        File[] entries = milestoneDir.toFile().listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    Path topicDir = entry.toPath();
                    Path topicFile = topicDir.resolve(TOPIC_FILE);
                    if (Files.exists(topicFile)) {
                        TopicEntity topic = loadTopic(topicDir, topicFile, coursePath);
                        topicsById.put(topic.getId(), topic);
                    }
                }
            }
        }

        List<TopicEntity> orderedTopics = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            String topicId = children.get(i);
            TopicEntity topic = topicsById.get(topicId);
            if (topic == null) {
                throw new CourseValidationException(coursePath,
                        "Inconsistencia de IDs: Milestone '" + label + "' (" + id
                                + ") referencia el topic ID '" + topicId + "' que no existe");
            }
            topic.setOrder(i + 1);
            orderedTopics.add(topic);
        }

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setId(id);
        milestone.setCode(code);
        milestone.setKind(NodeKind.MILESTONE);
        milestone.setLabel(label);
        milestone.setOldId(oldId);
        milestone.setParentId(parentId);
        milestone.setChildren(children);
        milestone.setOrder(0); // will be set by caller
        milestone.setSlug(slug);
        milestone.setTopics(orderedTopics);
        return milestone;
    }

    private TopicEntity loadTopic(Path topicDir, Path topicFile, String coursePath)
            throws IOException {
        Map<String, Object> json = readJsonMap(topicFile);

        String id = extractOid(json.get("_id"));
        String code = (String) json.get("code");
        String label = (String) json.get("label");
        String oldId = (String) json.get("oldId");
        String parentId = extractOid(json.get("parentId"));
        List<String> children = extractOidListNullable(json.get("children")); // always null per R010
        List<String> ruleIds = extractOidList(json.get("ruleIds"));
        String slug = topicDir.getFileName().toString();

        // Discover knowledges ordered by ruleIds
        Map<String, KnowledgeEntity> knowledgesById = new LinkedHashMap<>();
        File[] entries = topicDir.toFile().listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    Path knowledgeDir = entry.toPath();
                    Path knowledgeFile = knowledgeDir.resolve(KNOWLEDGE_FILE);
                    if (Files.exists(knowledgeFile)) {
                        KnowledgeEntity knowledge = loadKnowledge(knowledgeDir, knowledgeFile, coursePath);
                        knowledgesById.put(knowledge.getId(), knowledge);
                    }
                }
            }
        }

        List<KnowledgeEntity> orderedKnowledges = new ArrayList<>();
        for (int i = 0; i < ruleIds.size(); i++) {
            String knowledgeId = ruleIds.get(i);
            KnowledgeEntity knowledge = knowledgesById.get(knowledgeId);
            if (knowledge == null) {
                throw new CourseValidationException(coursePath,
                        "Inconsistencia de IDs: Topic '" + label + "' (" + id
                                + ") referencia el knowledge ID '" + knowledgeId + "' que no existe");
            }
            knowledge.setOrder(i + 1);
            orderedKnowledges.add(knowledge);
        }

        TopicEntity topic = new TopicEntity();
        topic.setId(id);
        topic.setCode(code);
        topic.setKind(NodeKind.TOPIC);
        topic.setLabel(label);
        topic.setOldId(oldId);
        topic.setParentId(parentId);
        topic.setChildren(children); // preserve null per R010
        topic.setRuleIds(ruleIds);
        topic.setOrder(0); // will be set by caller
        topic.setSlug(slug);
        topic.setKnowledges(orderedKnowledges);
        return topic;
    }

    private KnowledgeEntity loadKnowledge(Path knowledgeDir, Path knowledgeFile, String coursePath)
            throws IOException {
        Map<String, Object> knowledgeJson = readJsonMap(knowledgeFile);

        String id = extractOid(knowledgeJson.get("_id"));
        String code = (String) knowledgeJson.get("code");
        String label = (String) knowledgeJson.get("label");
        String oldId = (String) knowledgeJson.get("oldId");
        String parentId = extractOid(knowledgeJson.get("parentId"));
        boolean isRule = Boolean.TRUE.equals(knowledgeJson.get("isRule"));
        String instructions = (String) knowledgeJson.get("instructions");
        String slug = knowledgeDir.getFileName().toString();

        // Load quizzes
        Path quizzesFile = knowledgeDir.resolve(QUIZZES_FILE);
        List<QuizTemplateEntity> quizTemplates = new ArrayList<>();
        if (Files.exists(quizzesFile)) {
            quizTemplates = loadQuizzes(quizzesFile, coursePath);
        }

        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId(id);
        knowledge.setCode(code);
        knowledge.setKind(NodeKind.KNOWLEDGE);
        knowledge.setLabel(label);
        knowledge.setOldId(oldId);
        knowledge.setParentId(parentId);
        knowledge.setIsRule(isRule);
        knowledge.setInstructions(instructions);
        knowledge.setOrder(0); // will be set by caller
        knowledge.setSlug(slug);
        knowledge.setQuizTemplates(quizTemplates);
        return knowledge;
    }

    @SuppressWarnings("unchecked")
    private List<QuizTemplateEntity> loadQuizzes(Path quizzesFile, String coursePath)
            throws IOException {
        List<Object> jsonArray = objectMapper.readValue(quizzesFile.toFile(), List.class);
        List<QuizTemplateEntity> quizzes = new ArrayList<>();

        for (Object item : jsonArray) {
            Map<String, Object> q = asMap(item);
            if (q == null) continue;

            String oidId = extractOid(q.get("_id"));
            String id = (String) q.get("id");
            // R011: both id and oidId contain the same value
            if (id == null) id = oidId;

            String kind = (String) q.get("kind");
            String knowledgeId = extractOid(q.get("knowledgeId"));
            String title = (String) q.get("title");
            String instructions = (String) q.get("instructions");
            String translation = (String) q.get("translation");
            String theoryId = (String) q.get("theoryId");
            String topicName = (String) q.get("topicName");
            double difficulty = extractNumberDouble(q.get("difficulty"));
            double retries = extractNumberDouble(q.get("retries"));
            double noScoreRetries = extractNumberDouble(q.get("noScoreRetries"));
            String code = (String) q.get("code");
            String audioUrl = (String) q.get("audioUrl");
            String imageUrl = (String) q.get("imageUrl");
            String answerAudioUrl = (String) q.get("answerAudioUrl");
            String answerImageUrl = (String) q.get("answerImageUrl");
            String miniTheory = (String) q.get("miniTheory");
            String successMessage = (String) q.get("successMessage");

            FormEntity form = loadForm(asMap(q.get("form")));

            QuizTemplateEntity quiz = new QuizTemplateEntity(
                    id, oidId, kind, knowledgeId, title, instructions, translation,
                    theoryId, topicName, form, difficulty, retries, noScoreRetries,
                    code, audioUrl, imageUrl, answerAudioUrl, answerImageUrl,
                    miniTheory, successMessage
            );
            quizzes.add(quiz);
        }
        return quizzes;
    }

    private FormEntity loadForm(Map<String, Object> formJson) {
        if (formJson == null) return null;
        String kind = (String) formJson.get("kind");
        double incidence = extractNumberDouble(formJson.get("incidence"));
        String label = (String) formJson.get("label");
        String name = (String) formJson.get("name");

        List<SentencePartEntity> sentenceParts = new ArrayList<>();
        Object partsObj = formJson.get("sentenceParts");
        if (partsObj instanceof List<?> partsList) {
            for (Object part : partsList) {
                Map<String, Object> partMap = asMap(part);
                if (partMap != null) {
                    sentenceParts.add(loadSentencePart(partMap));
                }
            }
        }

        return new FormEntity(kind, incidence, label, name, sentenceParts);
    }

    @SuppressWarnings("unchecked")
    private SentencePartEntity loadSentencePart(Map<String, Object> partJson) {
        String kindStr = (String) partJson.get("kind");
        SentencePartKind kind = SentencePartKind.valueOf(kindStr);
        String text = (String) partJson.get("text");
        List<String> options = null;
        Object optObj = partJson.get("options");
        if (optObj instanceof List<?> optList) {
            options = (List<String>) optList;
        }
        return new SentencePartEntity(kind, text, options);
    }

    // -------------------------------------------------------------------------
    // CourseRepository.save
    // -------------------------------------------------------------------------

    @Override
    public void save(CourseEntity course, Path path) {
        if (course == null) {
            throw new IllegalArgumentException("course must not be null");
        }

        courseValidator.validate(course);

        String pathStr = path.toString();
        try {
            Files.createDirectories(path);

            // 1. Write _course.json (includes ROOT node)
            writeCourseJson(course, path);

            // 2. Write each milestone
            RootNodeEntity root = course.getRoot();
            if (root != null && root.getMilestones() != null) {
                for (MilestoneEntity milestone : root.getMilestones()) {
                    writeMilestone(milestone, path, pathStr);
                }
            }

        } catch (CourseValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new CourseValidationException(pathStr,
                    "Error al escribir los archivos: " + e.getMessage());
        }
    }

    private void writeCourseJson(CourseEntity course, Path courseDir) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();

        // _id as $oid
        json.put("_id", oidWrapper(course.getId()));

        // title
        json.put("title", course.getTitle());

        // knowledgeIds as array of $oid wrappers
        List<Map<String, String>> knowledgeIdsJson = new ArrayList<>();
        if (course.getKnowledgeIds() != null) {
            for (String kid : course.getKnowledgeIds()) {
                knowledgeIdsJson.add(oidWrapper(kid));
            }
        }
        json.put("knowledgeIds", knowledgeIdsJson);

        // ROOT node embedded
        RootNodeEntity root = course.getRoot();
        if (root != null) {
            Map<String, Object> rootJson = new LinkedHashMap<>();
            rootJson.put("_id", oidWrapper(root.getId()));
            rootJson.put("code", root.getCode());
            rootJson.put("kind", "ROOT");
            rootJson.put("label", root.getLabel());
            List<Map<String, String>> childrenJson = new ArrayList<>();
            if (root.getChildren() != null) {
                for (String cid : root.getChildren()) {
                    childrenJson.add(oidWrapper(cid));
                }
            }
            rootJson.put("children", childrenJson);
            json.put("root", rootJson);
        }

        Path courseFile = courseDir.resolve(COURSE_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(courseFile.toFile(), json);
    }

    private void writeMilestone(MilestoneEntity milestone, Path courseDir, String coursePath)
            throws IOException {
        String slug = resolveSlug(milestone.getSlug(), milestone.getLabel(), milestone.getId(), coursePath);
        Path milestoneDir = courseDir.resolve(slug);
        Files.createDirectories(milestoneDir);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("_id", oidWrapper(milestone.getId()));
        json.put("children", oidWrapperList(milestone.getChildren()));
        json.put("code", milestone.getCode());
        json.put("kind", "MILESTONE");
        json.put("label", milestone.getLabel());
        json.put("oldId", milestone.getOldId());
        json.put("parentId", oidWrapper(milestone.getParentId()));

        Path milestoneFile = milestoneDir.resolve(MILESTONE_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(milestoneFile.toFile(), json);

        // Write topics
        if (milestone.getTopics() != null) {
            for (TopicEntity topic : milestone.getTopics()) {
                writeTopic(topic, milestoneDir, coursePath);
            }
        }
    }

    private void writeTopic(TopicEntity topic, Path milestoneDir, String coursePath)
            throws IOException {
        String slug = resolveSlug(topic.getSlug(), topic.getLabel(), topic.getId(), coursePath);
        Path topicDir = milestoneDir.resolve(slug);
        Files.createDirectories(topicDir);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("_id", oidWrapper(topic.getId()));
        json.put("children", topic.getChildren()); // preserve null per R010
        json.put("code", topic.getCode());
        json.put("kind", "TOPIC");
        json.put("label", topic.getLabel());
        json.put("oldId", topic.getOldId());
        json.put("parentId", oidWrapper(topic.getParentId()));
        json.put("ruleIds", oidWrapperList(topic.getRuleIds()));

        Path topicFile = topicDir.resolve(TOPIC_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(topicFile.toFile(), json);

        // Write knowledges
        if (topic.getKnowledges() != null) {
            for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                writeKnowledge(knowledge, topicDir, coursePath);
            }
        }
    }

    private void writeKnowledge(KnowledgeEntity knowledge, Path topicDir, String coursePath)
            throws IOException {
        String slug = resolveSlug(knowledge.getSlug(), knowledge.getLabel(), knowledge.getId(), coursePath);
        Path knowledgeDir = topicDir.resolve(slug);
        Files.createDirectories(knowledgeDir);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("_id", oidWrapper(knowledge.getId()));
        json.put("code", knowledge.getCode());
        json.put("isRule", knowledge.isIsRule());
        json.put("kind", "KNOWLEDGE");
        json.put("label", knowledge.getLabel());
        json.put("oldId", knowledge.getOldId());
        json.put("parentId", oidWrapper(knowledge.getParentId()));
        json.put("instructions", knowledge.getInstructions());

        Path knowledgeFile = knowledgeDir.resolve(KNOWLEDGE_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(knowledgeFile.toFile(), json);

        // Write quizzes
        writeQuizzes(knowledge.getQuizTemplates(), knowledgeDir);
    }

    private void writeQuizzes(List<QuizTemplateEntity> quizzes, Path knowledgeDir)
            throws IOException {
        if (quizzes == null) quizzes = new ArrayList<>();

        List<Map<String, Object>> jsonArray = new ArrayList<>();
        for (QuizTemplateEntity quiz : quizzes) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("_id", oidWrapper(quiz.getOidId() != null ? quiz.getOidId() : quiz.getId()));
            q.put("id", quiz.getId());
            q.put("kind", quiz.getKind());
            q.put("knowledgeId", oidWrapper(quiz.getKnowledgeId()));
            q.put("title", quiz.getTitle());
            q.put("instructions", quiz.getInstructions());
            q.put("translation", quiz.getTranslation());
            q.put("theoryId", quiz.getTheoryId());
            q.put("topicName", quiz.getTopicName());
            q.put("difficulty", numberDoubleWrapper(quiz.getDifficulty()));
            q.put("retries", numberDoubleWrapper(quiz.getRetries()));
            q.put("noScoreRetries", numberDoubleWrapper(quiz.getNoScoreRetries()));
            q.put("code", quiz.getCode());
            q.put("audioUrl", quiz.getAudioUrl());
            q.put("imageUrl", quiz.getImageUrl());
            q.put("answerAudioUrl", quiz.getAnswerAudioUrl());
            q.put("answerImageUrl", quiz.getAnswerImageUrl());
            q.put("miniTheory", quiz.getMiniTheory());
            q.put("successMessage", quiz.getSuccessMessage());
            q.put("form", formToJson(quiz.getForm()));
            jsonArray.add(q);
        }

        Path quizzesFile = knowledgeDir.resolve(QUIZZES_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(quizzesFile.toFile(), jsonArray);
    }

    private Map<String, Object> formToJson(FormEntity form) {
        if (form == null) return null;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("kind", form.getKind());
        json.put("incidence", numberDoubleWrapper(form.getIncidence()));
        json.put("label", form.getLabel());
        json.put("name", form.getName());

        List<Map<String, Object>> parts = new ArrayList<>();
        if (form.getSentenceParts() != null) {
            for (SentencePartEntity part : form.getSentenceParts()) {
                parts.add(sentencePartToJson(part));
            }
        }
        json.put("sentenceParts", parts);
        return json;
    }

    private Map<String, Object> sentencePartToJson(SentencePartEntity part) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("kind", part.getKind() != null ? part.getKind().name() : null);
        json.put("text", part.getText());
        json.put("options", part.getOptions());
        return json;
    }

    // -------------------------------------------------------------------------
    // Slug generation (R016)
    // -------------------------------------------------------------------------

    private String resolveSlug(String existingSlug, String label, String id, String coursePath) {
        if (existingSlug != null && !existingSlug.isBlank()) {
            return existingSlug;
        }
        String generated = generateSlug(label);
        if (generated.isEmpty()) {
            throw new CourseValidationException(coursePath,
                    "No se pudo generar un slug valido para la entidad '" + label + "' (" + id + ")");
        }
        return generated;
    }

    static String generateSlug(String label) {
        if (label == null) return "";
        // Normalize unicode (accents, etc.)
        String normalized = Normalizer.normalize(label, Normalizer.Form.NFD);
        // Remove combining characters (accents)
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Convert to lowercase
        normalized = normalized.toLowerCase();
        // Replace spaces with hyphens
        normalized = normalized.replaceAll("\\s+", "-");
        // Remove punctuation and special chars (keep alphanumeric and hyphens)
        normalized = normalized.replaceAll("[^a-z0-9\\-]", "");
        // Collapse multiple hyphens
        normalized = normalized.replaceAll("-{2,}", "-");
        // Remove leading/trailing hyphens
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized;
    }

    // -------------------------------------------------------------------------
    // MongoDB Extended JSON helpers
    // -------------------------------------------------------------------------

    private Map<String, String> oidWrapper(String oid) {
        if (oid == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("$oid", oid);
        return map;
    }

    private List<Map<String, String>> oidWrapperList(List<String> ids) {
        if (ids == null) return null;
        List<Map<String, String>> result = new ArrayList<>();
        for (String id : ids) {
            result.add(oidWrapper(id));
        }
        return result;
    }

    private Map<String, String> numberDoubleWrapper(double value) {
        Map<String, String> map = new LinkedHashMap<>();
        // Format: integer values as "0.0", decimals as normal
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            map.put("$numberDouble", String.valueOf((int) value) + ".0");
        } else {
            map.put("$numberDouble", String.valueOf(value));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private String extractOid(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof Map<?, ?> map) {
            Object oid = map.get("$oid");
            if (oid instanceof String s) return s;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractOidList(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                String oid = extractOid(item);
                if (oid != null) result.add(oid);
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<String> extractOidListNullable(Object value) {
        if (value == null) return null;
        return extractOidList(value);
    }

    private double extractNumberDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Map<?, ?> map) {
            Object nd = map.get("$numberDouble");
            if (nd instanceof String s) {
                try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
            }
            if (nd instanceof Number n) return n.doubleValue();
        }
        return 0.0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(Path file) throws IOException {
        return objectMapper.readValue(file.toFile(), Map.class);
    }
}
