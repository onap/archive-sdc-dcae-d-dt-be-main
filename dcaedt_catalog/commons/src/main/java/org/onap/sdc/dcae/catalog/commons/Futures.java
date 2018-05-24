package org.onap.sdc.dcae.catalog.commons;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.Enums.LogLevel;


/**
 */
public class Futures<T> {

	private Futures() {
	}


	public static <T> Future<T>	failedFuture(Throwable theError) {
		return new BasicFuture<T>()
							.cause(theError);
	}

	public static <T> Future<T>	succeededFuture(T theResult) {
		return new BasicFuture<T>()
							.result(theResult);
	}
	
	public static <T> Future<T>	future() {
		return new BasicFuture<T>();
	}
	
	public static <U,V> Future<V> advance(Future<U> theStep,
																				final Function<U,V> theResultFunction) {
		return advance(theStep, theResultFunction, Function.identity());
	}

	public static <U,V> Future<V> advance(Future<U> theStep,
																				final Function<U,V> theResultFunction,
																				final Function<Throwable, Throwable> theErrorFunction) {
		final Future<V> adv = new BasicFuture<V>();
		theStep.setHandler(new FutureHandler<U>() {
															public void handle(Future<U> theResult) {
																if (theResult.failed())
																	adv.cause(theErrorFunction.apply(theResult.cause()));
																else
																	adv.result(theResultFunction.apply(theResult.result()));			
															}
													}); 
		return adv;
	}
	
	/** */
	public static class BasicFuture<T> implements Future<T> {

		protected boolean 		 succeeded,
													 failed;

		protected FutureHandler<T> handler;
		protected Throwable		 		cause;
		protected T						 		result;


		protected BasicFuture() {
		}

		public T result() {
			return this.result;
		}

		public Future<T> result(T theResult) {
			this.result = theResult;
			this.succeeded = true;
			this.cause = null;
			this.failed = false;
			callHandler();
			return this;
		}
	
		public Throwable cause() {
			return this.cause;
		}
		
		public Future<T> cause(Throwable theCause) {
			this.cause = theCause;
			this.failed = true;
			this.result = null;
			this.succeeded = false;
			callHandler();
			return this;
		}
	
		public boolean succeeded() {
			return this.succeeded;
		}

		public boolean failed() {
			return this.failed;
		}

		public boolean complete() {
			return this.failed || this.succeeded;
		}
 		
		public Future<T> setHandler(FutureHandler<T> theHandler) {
			this.handler = theHandler;
			callHandler();
			return this;
		}

		public T waitForResult() throws Exception {
			BasicHandler<T> hnd = buildHandler();
			setHandler(hnd);
			hnd.waitForCompletion();
			if (failed()) {
				throw (Exception) cause();
			}
			else {
				return result();
			}
		}
	
		public Future<T> waitForCompletion() throws InterruptedException {
			BasicHandler<T> hnd = buildHandler();
			setHandler(hnd);
			hnd.waitForCompletion();
			return this;
		}
	
		protected void callHandler() {
			if (this.handler != null && complete()) {
				this.handler.handle(this);
			}
		}

		protected BasicHandler<T> buildHandler() {
			return new BasicHandler<T>();
		}
	}


	/** */
	public static class BasicHandler<T> 
												implements FutureHandler<T> {
		
		protected T 							result = null;
		protected Throwable				error = null;
		protected CountDownLatch 	latch = null;

		BasicHandler() {
			this(new CountDownLatch(1));
		}

		BasicHandler(CountDownLatch theLatch) {
			this.latch = theLatch;
		}

		public void handle(Future<T> theResult) {
			process(theResult);
			if (this.latch != null) {
				this.latch.countDown();
			}
		}

		protected void process(Future<T> theResult) {
			if (theResult.failed()) {
				this.error = theResult.cause();
			}
			else {
				this.result = theResult.result();
			}
		}

		public T result(boolean doWait)
															throws InterruptedException, RuntimeException {
			if (doWait) {
				waitForCompletion();
			}
			if (null == this.error)
				return this.result;

			throw new RuntimeException(this.error);
		}
		
		public T result()
															throws InterruptedException, RuntimeException {
			return result(true);
		}

		public BasicHandler<T> waitForCompletion() throws InterruptedException {
			this.latch.await();
			return this;
		}
	}

	public static class Accumulator<T>	extends BasicFuture<List<T>>		
																				implements Future<List<T>> {

		protected List<Future<T>> futures = new LinkedList<Future<T>>();
		protected BasicHandler<T> accumulatorHandler = null;

		private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

		public Accumulator() {
			this.result = new LinkedList<T>();
		}

		public Accumulator<T> add(Future<T> theFuture) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Intersection add");
			this.futures.add(theFuture);
			this.result.add(null);
			return this;
		}

		public Accumulator<T> addAll(Accumulator<T> theFutures) {

			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Intersection addAll");

			return this;
		}

		public Future<List<T>> accumulate() {
			this.futures = Collections.unmodifiableList(this.futures);
			this.accumulatorHandler = new BasicHandler<T>(new CountDownLatch(this.futures.size())) {
												protected void process(Future<T> theResult) {
													if (theResult.failed()) {
														Accumulator.this.cause = theResult.cause();
													}
													else {
														Accumulator.this.result.set(
															Accumulator.this.futures.indexOf(theResult), theResult.result());
													}
													if (this.latch.getCount() == 1) {
														if (Accumulator.this.cause != null)
															Accumulator.this.cause(Accumulator.this.cause);
														else
															Accumulator.this.result(Accumulator.this.result);
													}
												}
										 };
			futures.stream()
							.forEach(f -> f.setHandler(this.accumulatorHandler));

			return this;
		}

	}


}
