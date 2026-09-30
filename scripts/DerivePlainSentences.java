import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Deriva form.sentences con el mismo codigo que usa content-audit al aplicar una correccion
 * (QuizSentenceConverter.toPlainSentences(form, sentenceMode), F-SMODE-R003/R006).
 *
 * Lo invoca scripts/import_backup.py para los ejercicios cuyo contenido cambio respecto del
 * arbol anterior. Se corre como programa de un solo archivo, con el classpath de audit-cli.sh:
 *
 *   java -cp "$CP" scripts/DerivePlainSentences.java entrada.json salida.json
 *
 * Entrada: [{"id": "...", "mode": "FILL"|"REWRITE"|null, "kind": "CLOZE"|"MULTIPLE_CHOICE",
 *            "sentenceParts": [{kind, text, options}], "items": [{id, label, incidence}]}]
 *          (kind e items son opcionales; un MULTIPLE_CHOICE sin items falla con su mensaje)
 * Salida:  {"sentences": {"<id>": ["..."]}, "errors": {"<id>": "mensaje"}}
 */
public class DerivePlainSentences {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("uso: DerivePlainSentences <entrada.json> <salida.json>");
            System.exit(2);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode input = mapper.readTree(new File(args[0]));
        QuizSentenceConverter converter = DefaultQuizSentenceConverter.create();

        ObjectNode output = mapper.createObjectNode();
        ObjectNode sentences = output.putObject("sentences");
        ObjectNode errors = output.putObject("errors");

        for (JsonNode item : input) {
            String id = item.get("id").asText();
            try {
                SentenceMode mode = item.hasNonNull("mode")
                        ? SentenceMode.valueOf(item.get("mode").asText())
                        : null;
                String kind = item.hasNonNull("kind") ? item.get("kind").asText() : "CLOZE";
                FormEntity form = new FormEntity(kind, 1.0, "", "", parts(item.get("sentenceParts")));
                if (item.hasNonNull("items")) {
                    form.setMultipleChoice(new MultipleChoiceEntity("SINGLE", items(item.get("items"))));
                }
                List<String> derived = converter.toPlainSentences(form, mode);
                sentences.set(id, mapper.valueToTree(derived));
            } catch (RuntimeException e) {
                errors.put(id, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(args[1]), output);
    }

    private static List<MultipleChoiceItemEntity> items(JsonNode array) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (JsonNode item : array) {
            items.add(new MultipleChoiceItemEntity(
                    item.hasNonNull("id") ? item.get("id").asText() : null,
                    item.path("incidence").asDouble(0.0),
                    item.hasNonNull("label") ? item.get("label").asText() : null));
        }
        return items;
    }

    private static List<SentencePartEntity> parts(JsonNode array) {
        List<SentencePartEntity> parts = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return parts;
        }
        for (JsonNode part : array) {
            SentencePartKind kind = SentencePartKind.valueOf(part.get("kind").asText());
            String text = part.hasNonNull("text") ? part.get("text").asText() : null;
            List<String> options = null;
            if (part.hasNonNull("options")) {
                options = new ArrayList<>();
                for (JsonNode option : part.get("options")) {
                    options.add(option.asText());
                }
            }
            parts.add(new SentencePartEntity(kind, text, options));
        }
        return parts;
    }
}
