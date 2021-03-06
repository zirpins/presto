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
package com.facebook.presto.byteCode;

import com.facebook.presto.byteCode.expression.ByteCodeExpression;
import com.facebook.presto.byteCode.instruction.VariableInstruction;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.facebook.presto.byteCode.ParameterizedType.type;
import static com.google.common.base.Preconditions.checkNotNull;

public class Variable
        extends ByteCodeExpression
{
    private final String name;
    private final int slot;

    public Variable(String name, int slot, ParameterizedType type)
    {
        super(type);
        this.name = checkNotNull(name, "name is null");
        this.slot = slot;
    }

    public String getName()
    {
        return name;
    }

    public int getSlot()
    {
        return slot;
    }

    public ByteCodeExpression set(ByteCodeExpression value)
    {
        return new SetVariableByteCodeExpression(this, value);
    }

    @Override
    public ByteCodeNode getByteCode()
    {
        return VariableInstruction.loadVariable(this);
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public List<ByteCodeNode> getChildNodes()
    {
        return ImmutableList.of();
    }

    private static final class SetVariableByteCodeExpression
            extends ByteCodeExpression
    {
        private final Variable variable;
        private final ByteCodeExpression value;

        public SetVariableByteCodeExpression(Variable variable, ByteCodeExpression value)
        {
            super(type(void.class));
            this.variable = checkNotNull(variable, "variable is null");
            this.value = checkNotNull(value, "value is null");
        }

        @Override
        public ByteCodeNode getByteCode()
        {
            return new Block(null)
                    .append(value)
                    .putVariable(variable);
        }

        @Override
        public List<ByteCodeNode> getChildNodes()
        {
            return ImmutableList.<ByteCodeNode>of(value);
        }

        @Override
        public String toString()
        {
            return variable.getName() + " = " + value;
        }
    }
}
