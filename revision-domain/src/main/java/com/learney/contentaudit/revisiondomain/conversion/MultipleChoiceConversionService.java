package com.learney.contentaudit.revisiondomain.conversion;

public interface MultipleChoiceConversionService {
    MultipleChoiceConversion.State execute(MultipleChoiceConversion.Request request);
}
