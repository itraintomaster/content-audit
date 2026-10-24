package com.learney.contentaudit.auditcli.bootstrap;

import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderClass;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolution;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolved;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolver;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * F-CMOTOR-R001: a judge built from its configuration on first use (hook C-14) -- reading its id never builds it -- that
 * can be built again with another runtime: the same configuration with that provider, model and effort. It only moves
 * to a runtime of a subscription, which the operator's local session consults without a key; a runtime that needs a
 * key or an address is never taken in place of the profile's (inv. 3). Moving it consults nothing.
 *
 * @param <C> the configuration the judge is built from
 */
public final class MovableJudge<C> implements Evaluator {

    /** The same configuration with another provider, model and effort -- null when the runtime has none. */
    @FunctionalInterface
    public interface Reconfiguration<C> {
        C onRuntime(C config, String providerName, String modelName, String reasoningEffort);
    }

    /** The name the configuration gives the service address, as a rejection would name it. */
    private static final String SERVICE_ADDRESS_INPUT_NAME = "baseUrl";

    private final String evaluatorId;

    private final C config;

    private final Function<C, Evaluator> factory;

    private final Reconfiguration<C> reconfiguration;

    private final LlmProviderResolver providers;

    private Evaluator delegate;

    private MovableJudge(String evaluatorId, C config, Evaluator built, Function<C, Evaluator> factory,
            Reconfiguration<C> reconfiguration, LlmProviderResolver providers) {
        this.evaluatorId = Objects.requireNonNull(evaluatorId, "evaluatorId");
        this.config = config;
        this.delegate = built;
        this.factory = Objects.requireNonNull(factory, "factory");
        this.reconfiguration = Objects.requireNonNull(reconfiguration, "reconfiguration");
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    /** A judge its factory builds the first time it is used. */
    public static <C> MovableJudge<C> lazy(String evaluatorId, C config, Function<C, Evaluator> factory,
            Reconfiguration<C> reconfiguration, LlmProviderResolver providers) {
        return new MovableJudge<>(evaluatorId, config, null, factory, reconfiguration, providers);
    }

    /** A judge already built from {@code config}; only another runtime is built by the factory. */
    public static <C> MovableJudge<C> of(Evaluator built, C config, Function<C, Evaluator> factory,
            Reconfiguration<C> reconfiguration, LlmProviderResolver providers) {
        return new MovableJudge<>(built.evaluatorId(), config, built, factory, reconfiguration, providers);
    }

    private synchronized Evaluator delegate() {
        if (delegate == null) {
            delegate = factory.apply(config);
        }
        return delegate;
    }

    @Override
    public String evaluatorId() {
        return evaluatorId;
    }

    @Override
    public EvaluationOutcome evaluate(EvaluationSubject subject) {
        return delegate().evaluate(subject);
    }

    @Override
    public Optional<String> evaluatorVersion() {
        return delegate().evaluatorVersion();
    }

    @Override
    public EvaluationRuntimeIdentity runtimeIdentity() {
        return delegate().runtimeIdentity();
    }

    @Override
    public Set<String> compatibleEvaluatorVersions() {
        return delegate().compatibleEvaluatorVersions();
    }

    @Override
    public List<EvaluatorVersionEquivalence> versionEquivalences() {
        return delegate().versionEquivalences();
    }

    @Override
    public Optional<Evaluator> withRuntime(EvaluationRuntimeIdentity runtime) {
        if (runtime == null || !runtime.isExplicit()) {
            return Optional.empty();
        }
        if (runtime.equals(runtimeIdentity())) {
            return Optional.of(this);
        }
        LlmProviderResolution resolution = providers.resolve(runtime.getProviderName(), null,
                SERVICE_ADDRESS_INPUT_NAME);
        if (!(resolution instanceof LlmProviderResolved resolved)
                || resolved.getProviderClass() != LlmProviderClass.LOCAL_SESSION) {
            return Optional.empty();
        }
        String effort = EvaluationRuntimeIdentity.NOT_APPLICABLE.equals(runtime.getReasoningEffort())
                ? null : runtime.getReasoningEffort();
        return Optional.of(lazy(evaluatorId, reconfiguration.onRuntime(config, resolved.getProviderName(),
                runtime.getModelName(), effort), factory, reconfiguration, providers));
    }
}
