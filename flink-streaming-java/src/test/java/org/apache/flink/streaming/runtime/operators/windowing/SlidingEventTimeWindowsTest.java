/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.operators.windowing;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.EventTimeTrigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.TestLogger;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.apache.flink.streaming.runtime.operators.windowing.StreamRecordMatchers.timeWindow;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SlidingEventTimeWindows}.
 */
public class SlidingEventTimeWindowsTest extends TestLogger {

	@Test
	public void testWindowAssignment() {
		WindowAssigner.WindowAssignerContext mockContext =
				mock(WindowAssigner.WindowAssignerContext.class);

		SlidingEventTimeWindows assigner =
				SlidingEventTimeWindows.of(Time.milliseconds(5000), Time.milliseconds(1000));

		assertThat(assigner.assignWindows("String", 0L, mockContext), containsInAnyOrder(
				timeWindow(-4000, 1000),
				timeWindow(-3000, 2000),
				timeWindow(-2000, 3000),
				timeWindow(-1000, 4000),
				timeWindow(0, 5000)));

		assertThat(assigner.assignWindows("String", 4999L, mockContext), containsInAnyOrder(
				timeWindow(0, 5000),
				timeWindow(1000, 6000),
				timeWindow(2000, 7000),
				timeWindow(3000, 8000),
				timeWindow(4000, 9000)));

		assertThat(assigner.assignWindows("String", 5000L, mockContext), containsInAnyOrder(
				timeWindow(1000, 6000),
				timeWindow(2000, 7000),
				timeWindow(3000, 8000),
				timeWindow(4000, 9000),
				timeWindow(5000, 10000)));
	}

	@Test
	public void testWindowAssignmentWithOffset() {
		WindowAssigner.WindowAssignerContext mockContext =
				mock(WindowAssigner.WindowAssignerContext.class);

		SlidingEventTimeWindows assigner =
				SlidingEventTimeWindows.of(Time.milliseconds(5000), Time.milliseconds(1000), Time.milliseconds(100));

		assertThat(assigner.assignWindows("String", 100L, mockContext), containsInAnyOrder(
				timeWindow(-3900, 1100),
				timeWindow(-2900, 2100),
				timeWindow(-1900, 3100),
				timeWindow(-900, 4100),
				timeWindow(100, 5100)));

		assertThat(assigner.assignWindows("String", 5099L, mockContext), containsInAnyOrder(
				timeWindow(100, 5100),
				timeWindow(1100, 6100),
				timeWindow(2100, 7100),
				timeWindow(3100, 8100),
				timeWindow(4100, 9100)));

		assertThat(assigner.assignWindows("String", 5100L, mockContext), containsInAnyOrder(
				timeWindow(1100, 6100),
				timeWindow(2100, 7100),
				timeWindow(3100, 8100),
				timeWindow(4100, 9100),
				timeWindow(5100, 10100)));

		SlidingEventTimeWindows assignerTest =
			SlidingEventTimeWindows.of(Time.seconds(5), Time.seconds(2), Time.seconds(1));
		Collection<TimeWindow> windows = assignerTest.assignWindows("String", 4000L, mockContext);
		for (TimeWindow window : windows) {
			System.out.println(window.getStart() + " - " + window.getEnd());
		}
	}

	@Test
	public void testWindowAssignmentWithNegativeOffset() {
		WindowAssigner.WindowAssignerContext mockContext =
			mock(WindowAssigner.WindowAssignerContext.class);

		SlidingEventTimeWindows assigner =
			SlidingEventTimeWindows.of(Time.milliseconds(5000), Time.milliseconds(1000), Time.milliseconds(-100));

		assertThat(assigner.assignWindows("String", 0L, mockContext), containsInAnyOrder(
			timeWindow(-4100, 900),
			timeWindow(-3100, 1900),
			timeWindow(-2100, 2900),
			timeWindow(-1100, 3900),
			timeWindow(-100, 4900)));

		assertThat(assigner.assignWindows("String", 4899L, mockContext), containsInAnyOrder(
			timeWindow(-100, 4900),
			timeWindow(900, 5900),
			timeWindow(1900, 6900),
			timeWindow(2900, 7900),
			timeWindow(3900, 8900)));

		assertThat(assigner.assignWindows("String", 4900L, mockContext), containsInAnyOrder(
			timeWindow(900, 5900),
			timeWindow(1900, 6900),
			timeWindow(2900, 7900),
			timeWindow(3900, 8900),
			timeWindow(4900, 9900)));
	}

	@Test
	public void testTimeUnits() {
		// sanity check with one other time unit

		WindowAssigner.WindowAssignerContext mockContext =
				mock(WindowAssigner.WindowAssignerContext.class);

		SlidingEventTimeWindows assigner = SlidingEventTimeWindows.of(Time.seconds(5), Time.seconds(1), Time.milliseconds(500));

		assertThat(assigner.assignWindows("String", 100L, mockContext), containsInAnyOrder(
				timeWindow(-4500, 500),
				timeWindow(-3500, 1500),
				timeWindow(-2500, 2500),
				timeWindow(-1500, 3500),
				timeWindow(-500, 4500)));

		assertThat(assigner.assignWindows("String", 5499L, mockContext), containsInAnyOrder(
				timeWindow(500, 5500),
				timeWindow(1500, 6500),
				timeWindow(2500, 7500),
				timeWindow(3500, 8500),
				timeWindow(4500, 9500)));

		assertThat(assigner.assignWindows("String", 5100L, mockContext), containsInAnyOrder(
				timeWindow(500, 5500),
				timeWindow(1500, 6500),
				timeWindow(2500, 7500),
				timeWindow(3500, 8500),
				timeWindow(4500, 9500)));
	}

	@Test
	public void testInvalidParameters() {
		try {
			SlidingEventTimeWindows.of(Time.seconds(-2), Time.seconds(1));
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertThat(e.toString(), containsString("abs(offset) < slide and size > 0"));
		}

		try {
			SlidingEventTimeWindows.of(Time.seconds(2), Time.seconds(-1));
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertThat(e.toString(), containsString("abs(offset) < slide and size > 0"));
		}

		try {
			SlidingEventTimeWindows.of(Time.seconds(-20), Time.seconds(10), Time.seconds(-1));
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertThat(e.toString(), containsString("abs(offset) < slide and size > 0"));
		}

		try {
			SlidingEventTimeWindows.of(Time.seconds(20), Time.seconds(10), Time.seconds(-11));
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertThat(e.toString(), containsString("abs(offset) < slide and size > 0"));
		}

		try {
			SlidingEventTimeWindows.of(Time.seconds(20), Time.seconds(10), Time.seconds(11));
			fail("should fail");
		} catch (IllegalArgumentException e) {
			assertThat(e.toString(), containsString("abs(offset) < slide and size > 0"));
		}
	}

	@Test
	public void testProperties() {
		SlidingEventTimeWindows assigner = SlidingEventTimeWindows.of(Time.seconds(5), Time.milliseconds(100));

		assertTrue(assigner.isEventTime());
		assertEquals(new TimeWindow.Serializer(), assigner.getWindowSerializer(new ExecutionConfig()));
		assertThat(assigner.getDefaultTrigger(mock(StreamExecutionEnvironment.class)), instanceOf(EventTimeTrigger.class));
	}
}
