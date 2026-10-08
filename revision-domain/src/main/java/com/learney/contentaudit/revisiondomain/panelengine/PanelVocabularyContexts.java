package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.WorkingVocabulary;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQueryCriteria;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLights;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLightsQuery;
import com.learney.contentaudit.revisiondomain.panel.PanelVocabularyContext;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import java.util.Objects;
import java.util.Optional;

/** Creates an ephemeral context from the composed version, not the batch's original audit. */
public final class PanelVocabularyContexts {
    private PanelVocabularyContexts() { }

    public static PanelVocabularyContext open(AuditEngine engine, CourseMapper mapper, CourseEntity composed,
            TopicWordLightsQuery lights, String sourceAuditId) {
        Objects.requireNonNull(lights, "word lights query");
        WorkingVocabulary vocabulary = Objects.requireNonNull(engine.openWorkingVocabulary(mapper.map(composed)),
                "El motor no creó el contexto léxico");
        return new PanelVocabularyContext() {
            @Override
            public TopicWordLights lightsFor(TopicWork work) {
                return vocabulary.query((report, version) -> {
                    TopicWordLights result = lights.lightsFor(report, sourceAuditId, work.getKnowledge().getId(),
                            new SuggestedLemmaQueryCriteria(Optional.empty(), Optional.empty(), Optional.empty(), false,
                                    Optional.empty()));
                    result.setWorkingVersion(version);
                    return result;
                });
            }

            @Override
            public void accepted(TopicWork work, KnowledgeEntity accepted) {
                // Mapping/NLP only for the changed topic. No model calls or full audits here.
                AuditableCourse cut = mapper.map(CatalogTopicMeter.cut(work, accepted));
                vocabulary.replaceKnowledge(cut.getMilestones().get(0).getTopics().get(0).getKnowledge().get(0));
            }
        };
    }
}
