/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.provider.model.internal;

import org.gradle.api.Project;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.CallableBuildOperation;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.tooling.provider.model.UnknownModelException;

import java.util.ArrayList;
import java.util.List;

public class DefaultToolingModelBuilderRegistry implements ToolingModelBuilderRegistry {
    private final ToolingModelBuilderRegistry parent;

    private final List<ToolingModelBuilder> builders = new ArrayList<ToolingModelBuilder>();
    private final BuildOperationExecutor buildOperationExecutor;

    public DefaultToolingModelBuilderRegistry(BuildOperationExecutor buildOperationExecutor) {
        this(buildOperationExecutor, null);
        register(new VoidToolingModelBuilder());
    }

    public DefaultToolingModelBuilderRegistry(BuildOperationExecutor buildOperationExecutor, ToolingModelBuilderRegistry parent) {
        this.buildOperationExecutor = buildOperationExecutor;
        this.parent = parent;
    }

    public void register(ToolingModelBuilder builder) {
        builders.add(builder);
    }

    public ToolingModelBuilder getBuilder(String modelName) throws UnsupportedOperationException {
        ToolingModelBuilder match = null;
        for (ToolingModelBuilder builder : builders) {
            if (builder.canBuild(modelName)) {
                if (match != null) {
                    throw new UnsupportedOperationException(String.format("Multiple builders are available to build a model of type '%s'.", modelName));
                }
                match = builder;
            }
        }
        if (match != null) {
            if (match instanceof ParameterizedToolingModelBuilder) {
                return new ParameterizedBuildOperationWrappingToolingModelBuilder((ParameterizedToolingModelBuilder) match);
            } else {
                return new BuildOperationWrappingToolingModelBuilder(match);
            }
        }
        if (parent != null) {
            return parent.getBuilder(modelName);
        }

        throw new UnknownModelException(String.format("No builders are available to build a model of type '%s'.", modelName));
    }

    private class BuildOperationWrappingToolingModelBuilder implements ToolingModelBuilder {
        private final ToolingModelBuilder delegate;

        private BuildOperationWrappingToolingModelBuilder(ToolingModelBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean canBuild(String modelName) {
            return delegate.canBuild(modelName);
        }

        @Override
        public Object buildAll(final String modelName, final Project project) {
            return buildOperationExecutor.call(new CallableBuildOperation<Object>() {
                @Override
                public Object call(BuildOperationContext context) {
                    return delegate.buildAll(modelName, project);
                }

                @Override
                public BuildOperationDescriptor.Builder description() {
                    return BuildOperationDescriptor.displayName("Build model '" + modelName + "' for " + project.getDisplayName()).
                        progressDisplayName("Building model '" + modelName + "'");
                }
            });
        }
    }

    private class ParameterizedBuildOperationWrappingToolingModelBuilder<T> extends BuildOperationWrappingToolingModelBuilder implements ParameterizedToolingModelBuilder<T> {
        private final ParameterizedToolingModelBuilder<T> delegate;

        private ParameterizedBuildOperationWrappingToolingModelBuilder(ParameterizedToolingModelBuilder delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public boolean canBuild(String modelName) {
            return delegate.canBuild(modelName);
        }

        @Override
        public Object buildAll(final String modelName, final T parameter, final Project project) {
            return buildOperationExecutor.call(new CallableBuildOperation<Object>() {
                @Override
                public Object call(BuildOperationContext context) {
                    return delegate.buildAll(modelName, parameter, project);
                }

                @Override
                public BuildOperationDescriptor.Builder description() {
                    return BuildOperationDescriptor.displayName("Build parameterized model '" + modelName + "' for " + project.getDisplayName()).
                        progressDisplayName("Building parameterized model '" + modelName + "'");
                }
            });
        }

        @Override
        public Class<T> getParameterType() {
            return delegate.getParameterType();
        }
    }

    private static class VoidToolingModelBuilder implements ToolingModelBuilder {
        public boolean canBuild(String modelName) {
            return modelName.equals(Void.class.getName());
        }

        public Object buildAll(String modelName, Project project) {
            return null;
        }
    }
}
