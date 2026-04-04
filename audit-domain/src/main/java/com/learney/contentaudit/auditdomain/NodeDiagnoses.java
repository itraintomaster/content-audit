package com.learney.contentaudit.auditdomain;

public sealed interface NodeDiagnoses
        permits CourseDiagnoses, LevelDiagnoses, TopicDiagnoses, KnowledgeDiagnoses, QuizDiagnoses {
}
