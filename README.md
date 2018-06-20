# Java Memory Eater

## What are Runtime.getRuntime().totalMemory() and freeMemory()?

According to the [API][https://download.oracle.com/javase/6/docs/api/java/lang/Runtime.html]

    totalMemory()

Returns the total amount of memory in the Java virtual machine. The value returned by this method may vary over time, depending on the host environment.
Note that the amount of memory required to hold an object of any given type may be implementation-dependent.

    maxMemory()

Returns the maximum amount of memory that the Java virtual machine will attempt to use. If there is no inherent limit then the value Long.MAX_VALUE will be returned.

    freeMemory()

Returns the amount of free memory in the Java Virtual Machine. Calling the gc method may result in increasing the value returned by freeMemory.

In reference to your question, `maxMemory()` returns the `-Xmx` value.

You may be wondering why there is a **totalMemory()** AND a **maxMemory()**.  The answer is that the JVM allocates memory lazily.  Lets say you start your Java process as such:

    java -Xms64m -Xmx1024m Foo

Your process starts with 64mb of memory, and if and when it needs more (up to 1024m), it will allocate memory.  `totalMemory()` corresponds to the amount of memory *currently* available to the JVM for Foo.  If the JVM needs more memory, it will lazily allocate it *up* to the maximum memory.  If you run with `-Xms1024m -Xmx1024m`, the value you get from `totalMemory()` and `maxMemory()` will be equal.

Also, if you want to accurately calculate the amount of *used* memory, you do so with the following calculation :

    final long usedMem = totalMemory() - freeMemory();


## My machine

    $ free -m
                  total        used        free      shared  buff/cache   available
    Mem:          15887        9328        1005         293        5554        6634
    Swap:          9999          28        9971

## Without limits

    MaxHeapSize: 4164943872 = 3972mb i.e. 15G of total mem of machine / 4
    Max JVM memory: 3702521856 = 3531mb
    Total JVM memory: 251658240 = 240mb i.e. 15G of total mem of machine / 64
    Allocated: 60mb, consumed: 65557960 bytes, free memory: 186100280 bytes i.e. 177.47905731201172mb
    [GC (Allocation Failure)  64021K->61896K(245760K), 3.7961610 secs]

Program continued to work and wasn't killed by docker's OOM killer. I stopped it after allocation of 60mb and first execution of GC.

## -Xmx20m

    MaxHeapSize: 20971520 = 20mb
    Max JVM memory: 20447232 = 19.5mb
    Total JVM memory: 20447232 = 19.5mb
    Allocated: 18mb, consumed: 19266408 bytes, free memory: 1180824 bytes i.e. 1.1261215209960938mb
    [Full GC (Ergonomics)  18814K->18747K(19968K), 0.5381021 secs]
    [Full GC (Allocation Failure)  18747K->18747K(19968K), 0.4546573 secs]
    Catching out of memory error


## -Xmx21m

    MaxHeapSize: 23068672 = 22mb
    Max JVM memory: 22544384 = 21.5
    Total JVM memory: 22544384 = 21.5
    Allocated: 19mb, consumed: 20382296 bytes, free memory: 2162088 bytes i.e. 2.0619277954101562mb
    [Full GC (Ergonomics)  19904K->19771K(22016K), 0.9921653 secs]
    [Full GC (Allocation Failure)  19771K->19771K(22016K), 0.5112764 secs]
    Catching out of memory error

JVM rounded heap size up to the 2 MB boundary

## -Xmx22m

    MaxHeapSize: 23068672 = 22mb
    Max JVM memory: 22544384 = 21.5
    Total JVM memory: 22544384 = 21.5
    Allocated: 19mb, consumed: 20382296 bytes, free memory: 2162088 bytes i.e. 2.0619277954101562mb
    [Full GC (Ergonomics)  19904K->19771K(22016K), 0.6735945 secs]
    [Full GC (Allocation Failure)  19771K->19771K(22016K), 0.3456865 secs]
    Catching out of memory error


## -Xmx10m -m=20m --memory-swap=20m --memory-swappiness=0 --kernel-memory=20m

    MaxHeapSize: 10485760 = 10mb
    Max JVM memory: 9961472 = 9.5mb
    Total JVM memory: 9961472 = 9.5mb
    Allocated: 7mb, consumed: 7961224 bytes, total: 9961472, free memory: 2000248 bytes i.e. 1.9075851440429688mb
    [GC (Allocation Failure) -- 7774K->7774K(9728K), 0.0013812 secs]
    [Full GC (Ergonomics)  7774K->7496K(9728K), 0.0028324 secs]
    [GC (Allocation Failure) -- 7496K->7504K(9728K), 0.0029179 secs]
    [Full GC (Allocation Failure)  7504K->7496K(9728K), 0.0030763 secs]
    Catching out of memory error

GC tried to cleanup twice and when it saw that memory wasn't cleared i.e. old 7504K -> new 7496K it thrown OOM exception.
The linux kernel of container itself took about 8mb of memory so we allowed to JVM only 10mb (-Xmx10m). Thus we can see that OOM exception was thrown by JVM instead of halting by Docker's OOM killer.

    root@memeater$ free
                  total        used        free      shared  buff/cache   available
    Mem:       16269252     9522456     1276612      315108     5470184     6503112
    Swap:      10239484       30208    10209276

## Limit JVM and Docker to use recovery mode
    docker run --name=memeater -m=20m --memory-swap=20m --memory-swappiness=0 --kernel-memory=20m memeater -e JAVA_OPTS="-Xmx10m" -recover
    MaxHeapSize:  = 14680064 = 14mb
    Max JVM memory: 14155776 = 13.5mb
    Total JVM memory: 14155776 = 13.5mb
    Allocated: 8mb, consumed: 8861864 bytes, total: 14155776, free memory: 5293912 bytes i.e. 5.048667907714844mb
    [GC (Allocation Failure)

As you can see Docker killed a container when JVM tried to execute GC. Thus the Java app wasn't shut down gracefully.


