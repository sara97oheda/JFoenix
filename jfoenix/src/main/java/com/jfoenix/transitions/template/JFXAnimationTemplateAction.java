/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.jfoenix.transitions.template;

import javafx.animation.Interpolator;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class which represents a builder and the configuration of animation keyframes. The configuration
 * methods are based on the methods of a {@link javafx.animation.KeyFrame} and {@link
 * javafx.animation.KeyValue}.<br>
 * It is possible that not all methods supported because the specific implementation of an animation
 * can diverge from general {@link javafx.animation.KeyFrame} and {@link javafx.animation.KeyValue}
 * implementations.
 *
 * @author Marcel Schlegel (schlegel11)
 * @version 1.0
 * @since 2018-09-22
 */
public final class JFXAnimationTemplateAction<N, T> {

  public static final int INFINITE_EXECUTIONS = -1;
  private final Collection<Function<N, WritableValue<T>>> targetFunctions;
  private final Function<N, T> endValueSupplier;
  private final Function<N, Interpolator> interpolatorSupplier;
  private final Predicate<N> executeWhenPredicate;
  private final BiConsumer<N, ActionEvent> onFinish;
  private final Function<N, Integer> executionsFunction;
  private final N animationObject;
  private int executionsCounter;

  private JFXAnimationTemplateAction(Builder<N, T> builder) {
    targetFunctions = builder.targetFunctions;
    endValueSupplier = builder.endValueFunction;
    interpolatorSupplier = builder.interpolatorFunction;
    animationObject = builder.animationObject;
    executeWhenPredicate = builder.executeWhenPredicate;
    onFinish = builder.onFinish;
    executionsFunction = builder.executionsFunction;
  }

  public static <N> InitBuilder<N> builder(Class<N> animationObjectType) {
    return new InitBuilder<>(
        animationObjectType,
        JFXTemplateBuilder.JFXAnimationObjectMapBuilder.DEFAULT_ANIMATION_OBJECT_NAME);
  }

  public static InitBuilder<Node> builder() {
    return builder(Node.class);
  }

  public Collection<Function<N, WritableValue<T>>> getTargetFunctions() {
    return targetFunctions;
  }

  public Optional<WritableValue<T>> getFirstTarget() {
    return getTargetFunctions()
        .stream()
        .findFirst()
        .map(function -> function.apply(animationObject));
  }

  public T getEndValue() {
    return endValueSupplier.apply(animationObject);
  }

  public Optional<Interpolator> getInterpolator() {
    return Optional.ofNullable(interpolatorSupplier.apply(animationObject));
  }

  public boolean isExecuteWhen() {
    return executeWhenPredicate.test(animationObject);
  }

  public int getExecutions() {
    return Math.max(executionsFunction.apply(animationObject), INFINITE_EXECUTIONS);
  }

  public void addExecution(int count) {
    int maxExecutions = getExecutions();
    if (count >= 0 && executionsCounter < maxExecutions) {
      executionsCounter = Math.min(executionsCounter + count, maxExecutions);
    }
  }

  public int getRemainingExecutions() {
    int maxExecutions = getExecutions();
    return maxExecutions == INFINITE_EXECUTIONS
        ? INFINITE_EXECUTIONS
        : Math.max(0, maxExecutions - executionsCounter);
  }

  public boolean hasRemainingExecutions() {
    return getRemainingExecutions() > 0 || getRemainingExecutions() == INFINITE_EXECUTIONS;
  }

  /**
   * True if {@link #hasRemainingExecutions()} and {@link #isExecuteWhen()} is {@code true}.
   *
   * @return a boolean.
   */
  public boolean isExecuted() {
    return hasRemainingExecutions() && isExecuteWhen();
  }

  public void handleOnFinish(ActionEvent actionEvent) {
    onFinish.accept(animationObject, actionEvent);
  }

  @SuppressWarnings("unchecked")
  public <M> Stream<M> mapTo(Function<WritableValue<Object>, M> mappingFunction) {
    return getTargetFunctions()
        .stream()
        .map(
            function ->
                mappingFunction.apply((WritableValue<Object>) function.apply(animationObject)));
  }

  public static final class Builder<N, T> {

    private final Collection<Function<N, WritableValue<T>>> targetFunctions;
    private final InitBuilder<N> initBuilder;
    private Function<N, T> endValueFunction = node -> null;
    private Function<N, Interpolator> interpolatorFunction = node -> null;
    private Predicate<N> executeWhenPredicate = node -> true;
    private BiConsumer<N, ActionEvent> onFinish = (node, event) -> {};
    private Function<N, Integer> executionsFunction = node -> INFINITE_EXECUTIONS;
    private N animationObject;

    private Builder(
        InitBuilder<N> initBuilder, Collection<Function<N, WritableValue<T>>> targetFunctions) {
      this.initBuilder = initBuilder;
      this.targetFunctions = targetFunctions;
    }

    private Builder(InitBuilder<N> initBuilder) {
      this(initBuilder, Collections.emptyList());
    }

    /**
     * The end value of the interpolation.
     *
     * @param endValue the end value.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> endValue(T endValue) {
      return endValue(node -> endValue);
    }

    /**
     * The lazy version of {@link #endValue(Object)} which is computed when the {@link
     * JFXAnimationTemplateAction} is build.<br>
     * The {@link Function} provides also a reference of the current animation object.
     *
     * @see #endValue(Object)
     * @param endValueFunction the end value {@link Function}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> endValue(Function<N, T> endValueFunction) {
      this.endValueFunction = endValueFunction;
      return this;
    }

    /**
     * The {@link Interpolator} of the animation.
     *
     * @param interpolator the {@link Interpolator}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> interpolator(Interpolator interpolator) {
      return interpolator(node -> interpolator);
    }

    /**
     * The lazy version of {@link #interpolator(Interpolator)} which is computed when the {@link
     * JFXAnimationTemplateAction} is build.<br>
     * The {@link Function} provides also a reference of the current animation object.
     *
     * @see #interpolator(Interpolator)
     * @param interpolatorFunction the interpolator {@link Function}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> interpolator(Function<N, Interpolator> interpolatorFunction) {
      this.interpolatorFunction = interpolatorFunction;
      return this;
    }

    /**
     * Executes the {@link JFXTemplateAction} if the condition is true.
     *
     * @param executeWhen the execute condition.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> executeWhen(boolean executeWhen) {
      return executeWhen(n -> executeWhen);
    }

    /**
     * The lazy version of {@link #executeWhen(boolean)} which is computed when the {@link
     * JFXAnimationTemplateAction} is build.<br>
     * The {@link Predicate} provides also a reference of the current animation object.
     *
     * @see #executeWhen(boolean)
     * @param executeWhenPredicate the condition {@link Predicate}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> executeWhen(Predicate<N> executeWhenPredicate) {
      this.executeWhenPredicate = executeWhenPredicate;
      return this;
    }

    /**
     * Ignores the current {@link JFXTemplateAction} so the action is never executed.
     *
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> ignore() {
      return executeWhen(false).executions(0);
    }

    /**
     * The on finish {@link ActionEvent} which is executed at the end of the current {@link
     * JFXTemplateAction}. <br>
     * The {@link BiConsumer} provides beside the {@link ActionEvent} also a reference of the
     * current animation object.
     *
     * @param onFinish the on finish {@link ActionEvent}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> onFinish(BiConsumer<N, ActionEvent> onFinish) {
      this.onFinish = onFinish;
      return this;
    }

    /**
     * The lazy version of {@link #executions(int)} which is computed when the {@link
     * JFXAnimationTemplateAction} is build.<br>
     * The {@link Function} provides also a reference of the current animation object.
     *
     * @see #executions(int)
     * @param executionsFunction the number of executions {@link Function}.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> executions(Function<N, Integer> executionsFunction) {
      this.executionsFunction = executionsFunction;
      return this;
    }

    /**
     * Executes the current {@link JFXTemplateAction} N times, until the given number of executions
     * is reached.
     *
     * @param executions the number of executions.
     * @return the {@link Builder} instance.
     */
    public Builder<N, T> executions(int executions) {
      return executions(node -> executions);
    }

    public JFXAnimationTemplateAction<N, T> build(
        Function<Collection<String>, Object> buildFunction) {
      this.animationObject =
          initBuilder.animationObjectType.cast(
              buildFunction.apply(initBuilder.animationObjectNames));
      return new JFXAnimationTemplateAction<>(this);
    }

    public Stream<JFXAnimationTemplateAction<N, T>> buildActions(
        Function<Collection<String>, Collection<Object>> buildFunction) {
      return buildFunction
          .apply(initBuilder.animationObjectNames)
          .stream()
          .map(
              animationObject -> {
                this.animationObject = initBuilder.animationObjectType.cast(animationObject);
                return new JFXAnimationTemplateAction<>(this);
              });
    }

    public JFXAnimationTemplateAction<N, T> build() {
      return new JFXAnimationTemplateAction<>(this);
    }
  }

  public static final class InitBuilder<N> {

    private final Class<N> animationObjectType;
    private final Collection<String> animationObjectNames = new ArrayList<>();

    private InitBuilder(
        Class<N> animationObjectType, String animationObjectId, String... animationObjectNames) {
      this.animationObjectType = animationObjectType;
      this.animationObjectNames.add(animationObjectId);
      this.animationObjectNames.addAll(Arrays.asList(animationObjectNames));
    }

    /**
     * The target or targets of the interpolation.
     *
     * @param target the interpolation target.
     * @param targets the interpolation targets.
     * @param <T> the target {@link WritableValue} type.
     * @return the {@link Builder} instance.
     */
    @SafeVarargs
    public final <T> Builder<N, T> target(WritableValue<T> target, WritableValue<T>... targets) {
      Function<N, WritableValue<T>>[] targetFunctions =
          Stream.of(targets)
              .map(value -> (Function<N, WritableValue<T>>) n -> value)
              .toArray((IntFunction<Function<N, WritableValue<T>>[]>) Function[]::new);
      return target(node -> target, targetFunctions);
    }

    /**
     * The lazy version of {@link #target(WritableValue, WritableValue[])} which is computed when
     * the {@link JFXAnimationTemplateAction} is build.<br>
     * The {@link Function} provides also a reference of the current animation object.
     *
     * @param targetFunction the interpolation target.
     * @param targetFunctions the interpolation targets.
     * @param <T> the target {@link WritableValue} type.
     * @return the {@link Builder} instance.
     */
    @SafeVarargs
    public final <T> Builder<N, T> target(
        Function<N, WritableValue<T>> targetFunction,
        Function<N, WritableValue<T>>... targetFunctions) {
      Collection<Function<N, WritableValue<T>>> functions = new ArrayList<>();
      functions.add(targetFunction);
      functions.addAll(Arrays.asList(targetFunctions));
      return new Builder<>(this, functions);
    }

    /** @see Builder#executeWhen(boolean) */
    public final Builder<N, ?> executeWhen(boolean animateWhen) {
      return new Builder<>(this).executeWhen(animateWhen);
    }

    /** @see Builder#executeWhen(Predicate) */
    public final Builder<N, ?> executeWhen(Predicate<N> executeWhenPredicate) {
      return new Builder<>(this).executeWhen(executeWhenPredicate);
    }

    /** @see Builder#ignore() */
    public Builder<N, ?> ignore() {
      return new Builder<>(this).ignore();
    }

    /** @see Builder#onFinish(BiConsumer) */
    public Builder<N, ?> onFinish(BiConsumer<N, ActionEvent> onFinish) {
      return new Builder<>(this).onFinish(onFinish);
    }

    /** @see Builder#executions(Function) */
    public Builder<N, ?> executions(Function<N, Integer> executionsFunction) {
      return new Builder<>(this).executions(executionsFunction);
    }

    /** @see Builder#executions(int) */
    public Builder<N, ?> executions(int executions) {
      return new Builder<>(this).executions(executions);
    }

    /**
     * Call named animation objects with a specific {@link Class} type and a name.<br>
     * The named animation objects are set when the {@link JFXAnimationTemplate} is build.
     *
     * @param clazz the specific type of the named animation objects.
     * @param animationObjectName the named animation object name.
     * @param animationObjectNames the named animation object names.
     * @param <T> the specific named animation object type.
     * @return the {@link InitBuilder} instance.
     */
    public <T> InitBuilder<T> withAnimationObject(
        Class<T> clazz, String animationObjectName, String... animationObjectNames) {
      return new InitBuilder<>(clazz, animationObjectName, animationObjectNames);
    }

    /**
     * Same as {@link #withAnimationObject(Class, String, String...)} but with a default type {@link
     * Node}.
     *
     * @see #withAnimationObject(Class, String, String...)
     * @param animationObjectName the named animation object name.
     * @param animationObjectNames the named animation object names.
     * @return the {@link InitBuilder} instance.
     */
    public InitBuilder<Node> withAnimationObject(
        String animationObjectName, String... animationObjectNames) {
      return withAnimationObject(Node.class, animationObjectName, animationObjectNames);
    }
  }
}
