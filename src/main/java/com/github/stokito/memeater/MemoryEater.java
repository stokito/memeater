package com.github.stokito.memeater;

import org.apache.derby.iapi.services.memory.LowMemory;

import java.util.ArrayList;
import java.util.List;

public class MemoryEater {
    private static final int MB = 1048576;
    private static List memoryLeakHolder = new ArrayList();
    private static LowMemory lowMemory = new LowMemory();

    public static void main(String[] args) throws InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> System.out.println("Unhandled exception " + e.toString()));
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("Max JVM memory: " + maxMemory);
        System.out.println("Total JVM memory: " + Runtime.getRuntime().totalMemory());
        int i;
        for (i = 1; ; i++) {
            if (lowMemory.isLowMemory()) {
                System.out.println("Restoring from low memory, skipping");
                i--;
                Thread.sleep(500);
                continue;
            }
            try {
                byte b[] = new byte[MB];
                memoryLeakHolder.add(b);
                Runtime rt = Runtime.getRuntime();
                long freeMemory = rt.freeMemory();
                long totalMemory = Runtime.getRuntime().totalMemory();
                long consumed = totalMemory - freeMemory;
                System.out.println("Allocated: " + i + "mb, consumed: " + consumed + " bytes, total: " + totalMemory + ", free memory: " + freeMemory + " bytes i.e. " + (freeMemory / 1024.0 / 1024.0) + "mb");
                Thread.sleep(500);
            } catch (OutOfMemoryError oom) {
                System.out.println("Catching out of memory error");
                if (args.length != 0 && "-recover".equals(args[0])) {
                    System.out.println("Switching to recovery mode");
                    memoryLeakHolder = null;
                    lowMemory.setLowMemory();
                    System.out.println("Memory freed, available: " + Runtime.getRuntime().freeMemory());
                    memoryLeakHolder = new ArrayList();
                    i = 0;
                } else {
                    throw oom;
                }
            }
        }
    }
}
