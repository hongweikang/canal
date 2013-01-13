package com.alibaba.otter.canal.parse.inbound;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.Assert;

import com.alibaba.erosa.protocol.protobuf.ErosaEntry;
import com.alibaba.otter.canal.common.AbstractCanalLifeCycle;
import com.alibaba.otter.canal.store.CanalStoreException;

/**
 * 缓冲event队列，提供按事务刷新数据的机制
 * 
 * @author jianghang 2012-12-6 上午11:05:12
 * @version 1.0.0
 */
public class EventTransactionBuffer extends AbstractCanalLifeCycle {

    private static final long        INIT_SQEUENCE = -1;
    private int                      bufferSize    = 1024;
    private int                      indexMask;
    private ErosaEntry.Entry[]       entries;

    private AtomicLong               putSequence   = new AtomicLong(INIT_SQEUENCE); // 代表当前put操作最后一次写操作发生的位置
    private AtomicLong               flushSequence = new AtomicLong(INIT_SQEUENCE); // 代表满足flush条件后最后一次数据flush的时间

    private TransactionFlushCallback flushCallback;

    public EventTransactionBuffer(){

    }

    public EventTransactionBuffer(TransactionFlushCallback flushCallback){
        this.flushCallback = flushCallback;
    }

    public void start() throws CanalStoreException {
        super.start();
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }

        if (bufferSize < 64) {
            throw new IllegalArgumentException("bufferSize must be >=64");
        }

        Assert.notNull(flushCallback, "flush callback is null!");
        indexMask = bufferSize - 1;
        entries = new ErosaEntry.Entry[bufferSize];
    }

    public void stop() throws CanalStoreException {
        putSequence.set(INIT_SQEUENCE);
        flushSequence.set(INIT_SQEUENCE);

        entries = null;
        super.stop();
    }

    public void add(List<ErosaEntry.Entry> entrys) throws InterruptedException {
        for (ErosaEntry.Entry entry : entrys) {
            add(entry);
        }
    }

    public void add(ErosaEntry.Entry entry) throws InterruptedException {
        switch (entry.getEntryType()) {
            case TRANSACTIONBEGIN:
                flush();// 刷新上一次的数据
                put(entry);
                break;
            case TRANSACTIONEND:
                put(entry);
                flush();
                break;
            case ROWDATA:
                put(entry);
                break;
            default:
                break;
        }
    }

    public void reset() {
        putSequence.set(INIT_SQEUENCE);
        flushSequence.set(INIT_SQEUENCE);
    }

    private void put(ErosaEntry.Entry data) throws InterruptedException {
        // 首先检查是否有空位
        if (checkFreeSlotAt(putSequence.get() + 1)) {
            long current = putSequence.get();
            long next = current + 1;

            // 先写数据，再更新对应的cursor,并发度高的情况，putSequence会被get请求可见，拿出了ringbuffer中的老的Entry值
            entries[getIndex(next)] = data;
            putSequence.set(next);
        } else {
            flush();// buffer区满了，刷新一下
            put(data);// 继续加一下新数据
        }
    }

    private void flush() throws InterruptedException {
        long start = this.flushSequence.get() + 1;
        long end = this.putSequence.get();

        if (start < end) {
            List<ErosaEntry.Entry> transaction = new ArrayList<ErosaEntry.Entry>();
            for (long next = start; next <= end; next++) {
                transaction.add(this.entries[getIndex(next)]);
            }

            flushCallback.flush(transaction);
            flushSequence.set(end);// flush成功后，更新flush位置
        }
    }

    /**
     * 查询是否有空位
     */
    private boolean checkFreeSlotAt(final long sequence) {
        final long wrapPoint = sequence - bufferSize;
        if (wrapPoint > flushSequence.get()) { // 刚好追上一轮
            return false;
        } else {
            return true;
        }
    }

    private int getIndex(long sequcnce) {
        return (int) sequcnce & indexMask;
    }

    // ================ setter / getter ==================

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setFlushCallback(TransactionFlushCallback flushCallback) {
        this.flushCallback = flushCallback;
    }

    /**
     * 事务刷新机制
     * 
     * @author jianghang 2012-12-6 上午11:57:38
     * @version 1.0.0
     */
    public static interface TransactionFlushCallback {

        public void flush(List<ErosaEntry.Entry> transaction) throws InterruptedException;
    }

}
