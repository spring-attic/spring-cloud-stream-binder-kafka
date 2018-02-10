/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.kstream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.kafka.streams.kstream.KTable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cloud.stream.binding.BindingTargetFactory} for {@link KTable}
 *
 * @since 2.0.0
 * @author Soby Chacko
 */
public class KTableBoundElementFactory extends AbstractBindingTargetFactory<KTable> {

	public KTableBoundElementFactory() {
		super(KTable.class);
	}

	@Override
	public KTable createInput(String name) {
		KTableBoundElementFactory.KTableWrapperHandler wrapper= new KTableBoundElementFactory.KTableWrapperHandler();
		ProxyFactory proxyFactory = new ProxyFactory(KTableBoundElementFactory.KTableWrapper.class, KTable.class);
		proxyFactory.addAdvice(wrapper);

		return (KTable) proxyFactory.getProxy();
	}

	@Override
	@SuppressWarnings("unchecked")
	public KTable createOutput(final String name) {
		throw new UnsupportedOperationException("Outbound operations are not allowed on target type KTable");
	}

	public interface KTableWrapper {
		void wrap(KTable<Object, Object> delegate);
	}

	private static class KTableWrapperHandler implements KTableBoundElementFactory.KTableWrapper, MethodInterceptor {

		private KTable<Object, Object> delegate;

		public void wrap(KTable<Object, Object> delegate) {
			Assert.notNull(delegate, "delegate cannot be null");
			Assert.isNull(this.delegate, "delegate already set to " + this.delegate);
			this.delegate = delegate;
		}

		@Override
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			if (methodInvocation.getMethod().getDeclaringClass().equals(KTable.class)) {
				Assert.notNull(delegate, "Trying to invoke " + methodInvocation
						.getMethod() + "  but no delegate has been set.");
				return methodInvocation.getMethod().invoke(delegate, methodInvocation.getArguments());
			}
			else if (methodInvocation.getMethod().getDeclaringClass().equals(KTableBoundElementFactory.KTableWrapper.class)) {
				return methodInvocation.getMethod().invoke(this, methodInvocation.getArguments());
			}
			else {
				throw new IllegalStateException("Only KStream method invocations are permitted");
			}
		}
	}
}
