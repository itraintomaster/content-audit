package com.learney.contentaudit.revisiondomain.panel;

import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLights;

/** Shared only within a correction run. Proposals already in flight may use an older snapshot. */
public interface PanelVocabularyContext {
    TopicWordLights lightsFor(TopicWork work);

    /** Called only after the accepted decision has been saved successfully. */
    void accepted(TopicWork work, KnowledgeEntity accepted);
}
