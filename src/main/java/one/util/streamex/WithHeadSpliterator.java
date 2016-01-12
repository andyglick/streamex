/*
 * Copyright 2015, 2016 Tagir Valeev
 * 
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
package one.util.streamex;

import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/*package*/ final class WithHeadSpliterator<T, U> extends AbstractSpliterator<U> {
    private final Spliterator<T> source;
    private final BiFunction<T, StreamEx<T>, Stream<U>> mapper;
    private Spliterator<U> target;
    
    WithHeadSpliterator(Spliterator<T> source, BiFunction<T, StreamEx<T>, Stream<U>> mapper) {
        super(Long.MAX_VALUE, ORDERED);
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super U> action) {
        if(!init())
            return false;
        if(target instanceof WithHeadSpliterator) {
            @SuppressWarnings("unchecked")
            Spliterator<U> next = ((WithHeadSpliterator<?, U>) target).target;
            if(next != null)
                target = next;
        }
        return target.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super U> action) {
        if(init())
            target.forEachRemaining(action);
    }

    private boolean init() {
        if(target == null) {
            Box<T> first = new Box<>(null);
            if(!source.tryAdvance(x -> first.a = x)) {
                return false;
            }
            target = mapper.apply(first.a, StreamEx.of(source)).spliterator();
        }
        return true;
    }

    @Override
    public long estimateSize() {
        if(target == null) {
            long size = source.estimateSize();
            return size == Long.MAX_VALUE || size <= 0 ? size : size - 1;
        }
        return target.estimateSize();
    }
}