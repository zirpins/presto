/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.sql.planner.optimizations;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.sql.planner.ExpressionSymbolInliner;
import com.facebook.presto.sql.planner.PlanNodeIdAllocator;
import com.facebook.presto.sql.planner.Symbol;
import com.facebook.presto.sql.planner.SymbolAllocator;
import com.facebook.presto.sql.planner.plan.PlanNode;
import com.facebook.presto.sql.planner.plan.PlanNodeRewriter;
import com.facebook.presto.sql.planner.plan.PlanRewriter;
import com.facebook.presto.sql.planner.plan.ProjectNode;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.sql.tree.ExpressionTreeRewriter;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Merges chains of consecutive projections
 */
public class MergeProjections
        extends PlanOptimizer
{
    @Override
    public PlanNode optimize(PlanNode plan, ConnectorSession session, Map<Symbol, Type> types, SymbolAllocator symbolAllocator, PlanNodeIdAllocator idAllocator)
    {
        checkNotNull(plan, "plan is null");
        checkNotNull(session, "session is null");
        checkNotNull(types, "types is null");
        checkNotNull(symbolAllocator, "symbolAllocator is null");
        checkNotNull(idAllocator, "idAllocator is null");

        return PlanRewriter.rewriteWith(new Rewriter(), plan);
    }

    private static class Rewriter
            extends PlanNodeRewriter<Void>
    {
        @Override
        public PlanNode rewriteProject(ProjectNode node, Void context, PlanRewriter<Void> planRewriter)
        {
            PlanNode source = planRewriter.rewrite(node.getSource(), context);

            if (source instanceof ProjectNode) {
                ImmutableMap.Builder<Symbol, Expression> projections = ImmutableMap.builder();
                for (Map.Entry<Symbol, Expression> projection : node.getOutputMap().entrySet()) {
                    Expression inlined = ExpressionTreeRewriter.rewriteWith(new ExpressionSymbolInliner(((ProjectNode) source).getOutputMap()), projection.getValue());
                    projections.put(projection.getKey(), inlined);
                }

                return new ProjectNode(node.getId(), ((ProjectNode) source).getSource(), projections.build());
            }

            return new ProjectNode(node.getId(), source, node.getOutputMap());
        }
    }
}
