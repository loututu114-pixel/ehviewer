/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.util;

import androidx.annotation.NonNull;
import com.hippo.lib.yorozuya.thread.PriorityThreadFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IoThreadPoolExecutor extends ThreadPoolExecutor {

  // 基于设备配置动态计算最优线程池大小
  private static int getOptimalCorePoolSize() {
    int cpuCount = Runtime.getRuntime().availableProcessors();
    long totalMemory = Runtime.getRuntime().maxMemory();
    
    // 基于CPU核心数和内存大小计算核心线程数
    if (totalMemory >= 8L * 1024 * 1024 * 1024) { // 8GB+设备
      return Math.min(8, cpuCount); // 最多8个核心线程
    } else if (totalMemory >= 6L * 1024 * 1024 * 1024) { // 6GB设备
      return Math.min(6, cpuCount); // 最多6个核心线程
    } else if (totalMemory >= 4L * 1024 * 1024 * 1024) { // 4GB设备
      return Math.min(4, cpuCount); // 最多4个核心线程
    } else {
      return Math.min(3, cpuCount); // 最多3个核心线程 (原配置)
    }
  }
  
  private static int getOptimalMaxPoolSize() {
    int cpuCount = Runtime.getRuntime().availableProcessors();
    long totalMemory = Runtime.getRuntime().maxMemory();
    
    // 基于CPU核心数和内存大小计算最大线程数
    if (totalMemory >= 8L * 1024 * 1024 * 1024) { // 8GB+设备
      return Math.min(64, cpuCount * 4); // 最多64个线程
    } else if (totalMemory >= 6L * 1024 * 1024 * 1024) { // 6GB设备
      return Math.min(48, cpuCount * 3); // 最多48个线程
    } else if (totalMemory >= 4L * 1024 * 1024 * 1024) { // 4GB设备
      return Math.min(36, cpuCount * 2); // 最多36个线程
    } else {
      return 32; // 原配置
    }
  }

  private final static ThreadPoolExecutor INSTANCE =
      IoThreadPoolExecutor.newInstance(getOptimalCorePoolSize(), getOptimalMaxPoolSize(), 1L, TimeUnit.SECONDS,
          new PriorityThreadFactory("IO",android.os.Process.THREAD_PRIORITY_BACKGROUND));

  private IoThreadPoolExecutor(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory,
      RejectedExecutionHandler handler
  ) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
  }

  public static ThreadPoolExecutor getInstance() {
    return INSTANCE;
  }

  private static ThreadPoolExecutor newInstance(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      ThreadFactory threadFactory
  ) {
    ThreadQueue queue = new ThreadQueue();
    PutRunnableBackHandler handler = new PutRunnableBackHandler();
    IoThreadPoolExecutor executor = new IoThreadPoolExecutor(
        corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, threadFactory, handler);
    queue.setThreadPoolExecutor(executor);
    return executor;
  }

  private static class ThreadQueue extends LinkedBlockingQueue<Runnable> {

    private ThreadPoolExecutor executor;

    void setThreadPoolExecutor(ThreadPoolExecutor executor) {
      this.executor = executor;
    }

    @Override
    public boolean offer(@NonNull Runnable o) {
      int allWorkingThreads = executor.getActiveCount() + super.size();
      return allWorkingThreads < executor.getPoolSize() && super.offer(o);
    }
  }

  public static class PutRunnableBackHandler implements RejectedExecutionHandler {
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      try {
        executor.getQueue().put(r);
      } catch (InterruptedException e) {
        throw new RejectedExecutionException(e);
      }
    }
  }
}
