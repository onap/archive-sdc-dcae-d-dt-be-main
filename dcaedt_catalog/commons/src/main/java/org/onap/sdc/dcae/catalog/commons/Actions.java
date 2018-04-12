package org.onap.sdc.dcae.catalog.commons;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.dcae.catalog.commons.Action;
import org.onap.sdc.dcae.catalog.commons.Future;
import org.onap.sdc.dcae.catalog.commons.FutureHandler;
import org.onap.sdc.dcae.catalog.commons.Futures;

/**
 */
public interface Actions {

	/** */
	public static interface CompoundAction<T> extends Action<List<T>> {

		public CompoundAction<T> addAction(Action<T> theAction);

		public List<Action<T>> actions();

		public Future<List<T>> execute();
	} 


	public static class BasicCompoundAction<T> implements CompoundAction<T> {

		private LinkedList<Action<T>> actions = new LinkedList<Action<T>>();



		public CompoundAction<T> addAction(Action<T> theAction) {
			this.actions.add(theAction);
			return this;
		}

		public List<Action<T>> actions() {
			return this.actions;
		}

		public Future<List<T>> execute() {
			CompoundFuture<T> cf = new CompoundFuture<T>(this.actions.size());
			for (Action a: this.actions)
				cf.addFuture(a.execute());
			return cf;
		}
	}


	public static class CompoundFuture<T> extends Futures.BasicFuture<List<T>> {

		private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
		private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

		private LinkedList<Future<T>> futures = new LinkedList<Future<T>>();
		private FutureHandler<T>			hnd; 

		CompoundFuture(int theActionCount) {

			hnd = new Futures.BasicHandler<T>(new CountDownLatch(theActionCount)) {

							private List<T>	results = new ArrayList<T>(Collections.nCopies(theActionCount, null));

							protected void process(Future<T> theResult) {
								synchronized(CompoundFuture.this) {
									if (theResult.failed()) {
										CompoundFuture.this.cause(theResult.cause());
										//and stop processing of other results
										this.results = null;
										//??
									}
									else {
										if (this.results != null)
											this.results.set(futures.indexOf(theResult), theResult.result());
										debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got result for action {}. Count at {}", futures.indexOf(theResult), this.latch.getCount());
									}
									if (this.latch.getCount() == 1) {//this was the last result
										debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Got all results: {}", this.results);
										CompoundFuture.this.result(this.results);
									}
								}
							}
						};
		}

		CompoundFuture<T> addFuture(Future<T> theFuture) {
			synchronized(this) {
				futures.add(theFuture);
				theFuture.setHandler(this.hnd);
			}
			return this;
		}

	}

/*
	public static class CompoundFutureHandler<T> implements FutureHandler<T> {

		protected List<T> 				result = null;
		protected List<Throwable>	error = null;
		protected CountDownLatch 	latch = null;

		CompoundFutureHandler(int theResultCount) {
			this(new CountDownLatch(theResultCount));
		}

		public void handle(Future<T> theResult) {
			if (this.latch != null) {
				this.latch.countDown();
			}
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
*/

	public static class Sequence<T> implements Action<List<T>> {

		private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
		private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

		private List<Action<T>> 	actions = new LinkedList<Action<T>>();
		private int								current = 0;
		private SequenceFuture<T> future = new SequenceFuture<T>();

		public Sequence<T> add(Action<T> theAction) {
			if (this.current > 0)
				throw new IllegalStateException("In execution");
			this.actions.add(theAction);
			return this;
		}

		/* we allow 'early' access to the future so that a client can pass its reference while
		 * it still builds the sequence, for example. 
		 */
		public Future<List<T>> future() {
			return this.future;
		}

		//need to add protection when for the 'no action' case
		public Future<List<T>> execute() {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Starting serialized execution of {}", actions);
			if (hasNext())
				next().execute().setHandler(future.hnd);
			return this.future;
		}

		protected boolean hasNext() {
			return this.current < actions.size();
		}

		protected Action next() {
			return actions.get(this.current++);
		} 
	
		private class SequenceFuture<T> extends Futures.BasicFuture<List<T>> {

			private List<T>					 results = new LinkedList<T>();
			private FutureHandler<T> hnd = new Futures.BasicHandler<T>() {

				protected void process(Future<T> theResult) {
				
					if (theResult.failed()) {
						SequenceFuture.this.cause(theResult.cause());
						//and stop processing of other results
					}
					else {
						SequenceFuture.this.results.add(theResult.result());
						if (Sequence.this.hasNext()) {
							Sequence.this.next().execute().setHandler(this);
						}
						else {
							SequenceFuture.this.result(SequenceFuture.this.results);
						}
					}
				}
			};


		}	



	}

}
